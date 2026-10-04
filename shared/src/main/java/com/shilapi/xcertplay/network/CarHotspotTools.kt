package com.shilapi.xcertplay.network

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.os.SystemClock
import com.shilapi.xcertplay.compat.systemService
import com.shilapi.xcertplay.orchestration.ManualHotspotBand
import com.shilapi.xcertplay.orchestration.ManualHotspotSecurity
import java.lang.reflect.InvocationTargetException

/** Read and user-requested start operations for the legacy device-owned Wi-Fi AP. */
object CarHotspotTools {
    const val STATE_DISABLING = 0
    const val STATE_DISABLED = 1
    const val STATE_ENABLING = 2
    const val STATE_ENABLED = 3
    const val STATE_FAILED = 4

    const val AOSP_STATE_DISABLING = 10
    const val AOSP_STATE_DISABLED = 11
    const val AOSP_STATE_ENABLING = 12
    const val AOSP_STATE_ENABLED = 13
    const val AOSP_STATE_FAILED = 14

    const val DEFAULT_POLL_INTERVAL_MILLIS = 500L
    const val DEFAULT_START_TIMEOUT_MILLIS = 15_000L

    fun read(context: Context): CarHotspotReadResult = try {
        read(androidAccess(context))
    } catch (error: Throwable) {
        CarHotspotReadResult.Failure(error.toHotspotFailure())
    }

    fun readState(context: Context): CarHotspotState? = runCatching {
        readState(androidAccess(context))?.let(::stateFromRaw)
    }.getOrNull()

    fun start(
        context: Context,
        timeoutMillis: Long = DEFAULT_START_TIMEOUT_MILLIS,
        pollIntervalMillis: Long = DEFAULT_POLL_INTERVAL_MILLIS,
        pause: (Long) -> Unit = { Thread.sleep(it) },
        cancelled: () -> Boolean = { false },
    ): CarHotspotStartResult = start(
        androidAccess(context), timeoutMillis, pollIntervalMillis, pause, cancelled,
    )

    internal fun read(access: CarHotspotWifiAccess): CarHotspotReadResult {
        return try {
            val configuration = access.readConfiguration()
                ?: return CarHotspotReadResult.Failure(CarHotspotFailure.CONFIGURATION_UNAVAILABLE)
            CarHotspotReadResult.Available(configuration)
        } catch (error: Throwable) {
            CarHotspotReadResult.Failure(error.toHotspotFailure())
        }
    }

    internal fun readState(access: CarHotspotWifiAccess): Int? = access.readState()

    internal fun configurationFrom(
        native: WifiConfiguration,
        bandValue: Int? = readIntField(native, "apBand"),
        channelValue: Int? = readIntField(native, "apChannel"),
    ): CarHotspotConfiguration? {
        // H52 stores and returns literal SSID/PSK values; quotes may be part of the value.
        val ssid = native.SSID?.takeIf(String::isNotBlank) ?: return null
        val keyManagement = native.allowedKeyManagement ?: return null
        val hasWpaPsk = keyManagement.get(1)
        val hasWpa2Psk = keyManagement.get(4)
        var bit = keyManagement.nextSetBit(0)
        var hasUnsupportedSecurity = false
        while (bit >= 0) {
            if (bit != 0 && bit != 1 && bit != 4) hasUnsupportedSecurity = true
            bit = keyManagement.nextSetBit(bit + 1)
        }
        if (hasUnsupportedSecurity || (!keyManagement.get(0) && !hasWpaPsk && !hasWpa2Psk)) {
            throw CarHotspotConfigurationException(CarHotspotFailure.UNSUPPORTED_CONFIGURATION)
        }
        val isOpen = keyManagement.get(0) && !hasWpaPsk && !hasWpa2Psk
        val security = when {
            isOpen -> ManualHotspotSecurity.OPEN
            hasWpaPsk || hasWpa2Psk -> ManualHotspotSecurity.WPA2
            else -> throw CarHotspotConfigurationException(CarHotspotFailure.UNSUPPORTED_CONFIGURATION)
        }
        if (native.wepKeys?.any { !it.isNullOrBlank() } == true) {
            throw CarHotspotConfigurationException(CarHotspotFailure.UNSUPPORTED_CONFIGURATION)
        }
        val passphrase = if (isOpen) "" else native.preSharedKey.orEmpty()
        if (!isOpen && (passphrase.isBlank() || passphrase.all { it == '*' })) {
            throw CarHotspotConfigurationException(CarHotspotFailure.UNSUPPORTED_CONFIGURATION)
        }
        val band = when (bandValue) {
            0 -> ManualHotspotBand.GHZ_2_4
            1 -> ManualHotspotBand.GHZ_5
            -1 -> ManualHotspotBand.AUTO
            else -> null
        }
        val channel = channelValue?.takeIf { it > 0 }
        return CarHotspotConfiguration(ssid, passphrase, security, band, channel, native)
    }

    private fun readIntField(configuration: WifiConfiguration, fieldName: String): Int? =
        runCatching { WifiConfiguration::class.java.getField(fieldName).getInt(configuration) }.getOrNull()

    /**
     * Uses the exact configuration object returned by the firmware. In particular, do not build a
     * replacement WifiConfiguration: OEM-only AP fields such as maxNumSta must survive the call.
     */
    internal fun start(
        access: CarHotspotWifiAccess,
        timeoutMillis: Long,
        pollIntervalMillis: Long,
        pause: (Long) -> Unit,
        cancelled: () -> Boolean = { false },
        nowMillis: () -> Long = SystemClock::elapsedRealtime,
    ): CarHotspotStartResult {
        require(timeoutMillis >= 0L)
        require(pollIntervalMillis > 0L)

        val configuration = try {
            access.readConfiguration()
        } catch (error: Throwable) {
            return CarHotspotStartResult.Failure(error.toHotspotFailure())
        } ?: return CarHotspotStartResult.Failure(CarHotspotFailure.CONFIGURATION_UNAVAILABLE)
        if (cancelled()) return CarHotspotStartResult.Failure(CarHotspotFailure.OPERATION_CANCELLED)

        val initialState = try {
            access.readState()?.let(::stateFromRaw)
        } catch (error: Throwable) {
            return CarHotspotStartResult.Failure(error.toHotspotFailure())
        } ?: return CarHotspotStartResult.Failure(CarHotspotFailure.STATE_UNAVAILABLE)

        when (initialState) {
            CarHotspotState.ENABLED -> return CarHotspotStartResult.AlreadyEnabled(configuration)
            CarHotspotState.ENABLING -> return awaitEnabled(
                access, configuration, timeoutMillis, pollIntervalMillis, pause, cancelled, nowMillis,
            )
            CarHotspotState.DISABLING ->
                return CarHotspotStartResult.Failure(CarHotspotFailure.STATE_TRANSITION_IN_PROGRESS)
            CarHotspotState.FAILED ->
                return CarHotspotStartResult.Failure(CarHotspotFailure.FIRMWARE_REPORTED_FAILURE)
            CarHotspotState.DISABLED -> Unit
            CarHotspotState.UNKNOWN ->
                return CarHotspotStartResult.Failure(CarHotspotFailure.STATE_UNAVAILABLE)
        }

        if (cancelled()) return CarHotspotStartResult.Failure(CarHotspotFailure.OPERATION_CANCELLED)
        val accepted = try {
            access.requestEnable(configuration)
        } catch (error: Throwable) {
            return CarHotspotStartResult.Failure(error.toHotspotFailure())
        }
        if (!accepted) {
            return CarHotspotStartResult.Failure(CarHotspotFailure.REQUEST_REJECTED)
        }
        if (cancelled()) return CarHotspotStartResult.Failure(CarHotspotFailure.OPERATION_CANCELLED)

        // setWifiApEnabled(true) only accepts an asynchronous request. It is not proof that the AP
        // came up; wait for the service state and never send a second request on timeout.
        return awaitEnabled(access, configuration, timeoutMillis, pollIntervalMillis, pause, cancelled, nowMillis)
    }

    internal fun stateFromRaw(rawState: Int): CarHotspotState = when (rawState) {
        STATE_DISABLING, AOSP_STATE_DISABLING -> CarHotspotState.DISABLING
        STATE_DISABLED, AOSP_STATE_DISABLED -> CarHotspotState.DISABLED
        STATE_ENABLING, AOSP_STATE_ENABLING -> CarHotspotState.ENABLING
        STATE_ENABLED, AOSP_STATE_ENABLED -> CarHotspotState.ENABLED
        STATE_FAILED, AOSP_STATE_FAILED -> CarHotspotState.FAILED
        else -> CarHotspotState.UNKNOWN
    }

    private fun awaitEnabled(
        access: CarHotspotWifiAccess,
        configuration: CarHotspotConfiguration,
        timeoutMillis: Long,
        pollIntervalMillis: Long,
        pause: (Long) -> Unit,
        cancelled: () -> Boolean,
        nowMillis: () -> Long,
    ): CarHotspotStartResult {
        val startedAt = nowMillis()
        val deadline = if (Long.MAX_VALUE - startedAt < timeoutMillis) Long.MAX_VALUE else startedAt + timeoutMillis
        while (nowMillis() < deadline) {
            if (cancelled()) return CarHotspotStartResult.Failure(CarHotspotFailure.OPERATION_CANCELLED)
            val remaining = (deadline - nowMillis()).coerceAtLeast(1L)
            try {
                pause(minOf(pollIntervalMillis, remaining))
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                return CarHotspotStartResult.Failure(CarHotspotFailure.OPERATION_INTERRUPTED)
            } catch (_: Throwable) {
                return CarHotspotStartResult.Failure(CarHotspotFailure.OPERATION_FAILED)
            }

            val state = try {
                access.readState()?.let(::stateFromRaw)
            } catch (error: Throwable) {
                return CarHotspotStartResult.Failure(error.toHotspotFailure())
            } ?: return CarHotspotStartResult.Failure(CarHotspotFailure.STATE_UNAVAILABLE)

            when (state) {
                CarHotspotState.ENABLED -> return CarHotspotStartResult.Enabled(configuration)
                CarHotspotState.FAILED ->
                    return CarHotspotStartResult.Failure(CarHotspotFailure.FIRMWARE_REPORTED_FAILURE)
                CarHotspotState.UNKNOWN ->
                    return CarHotspotStartResult.Failure(CarHotspotFailure.STATE_UNAVAILABLE)
                CarHotspotState.DISABLING ->
                    return CarHotspotStartResult.Failure(CarHotspotFailure.STATE_TRANSITION_IN_PROGRESS)
                CarHotspotState.DISABLED, CarHotspotState.ENABLING -> Unit
            }
        }
        return CarHotspotStartResult.Failure(CarHotspotFailure.START_TIMED_OUT)
    }

    private fun androidAccess(context: Context): CarHotspotWifiAccess {
        val appContext = context.applicationContext
        val manager = appContext.systemService(WifiManager::class.java, "wifi")
            ?: throw IllegalStateException("Wi-Fi service unavailable")
        return AndroidCarHotspotWifiAccess(manager)
    }

    private fun Throwable.toHotspotFailure(): CarHotspotFailure {
        var cause: Throwable = this
        while (cause is InvocationTargetException && cause.targetException != null) {
            cause = cause.targetException
        }
        return when (cause) {
            is SecurityException -> CarHotspotFailure.PERMISSION_DENIED
            is CarHotspotConfigurationException -> cause.failure
            else -> CarHotspotFailure.OPERATION_FAILED
        }
    }
}

enum class CarHotspotState {
    DISABLING,
    DISABLED,
    ENABLING,
    ENABLED,
    FAILED,
    UNKNOWN,
}

enum class CarHotspotFailure {
    CONFIGURATION_UNAVAILABLE,
    UNSUPPORTED_CONFIGURATION,
    OPERATION_CANCELLED,
    PERMISSION_DENIED,
    OPERATION_FAILED,
    STATE_UNAVAILABLE,
    STATE_TRANSITION_IN_PROGRESS,
    FIRMWARE_REPORTED_FAILURE,
    REQUEST_REJECTED,
    START_TIMED_OUT,
    OPERATION_INTERRUPTED,
}

sealed class CarHotspotReadResult {
    class Available(val configuration: CarHotspotConfiguration) : CarHotspotReadResult()
    class Failure(val reason: CarHotspotFailure) : CarHotspotReadResult()
}

sealed class CarHotspotStartResult {
    class AlreadyEnabled(val configuration: CarHotspotConfiguration) : CarHotspotStartResult()
    class Enabled(val configuration: CarHotspotConfiguration) : CarHotspotStartResult()
    class Failure(val reason: CarHotspotFailure) : CarHotspotStartResult()
}

/** Does not print or expose the Wi-Fi password through toString(). */
class CarHotspotConfiguration internal constructor(
    val ssid: String,
    val passphrase: String,
    val security: ManualHotspotSecurity,
    val band: ManualHotspotBand?,
    val channel: Int?,
    internal val nativeConfiguration: Any,
) {
    override fun toString(): String =
        "CarHotspotConfiguration(ssid=$ssid, security=$security, band=$band, channel=$channel)"
}

internal class CarHotspotConfigurationException(val failure: CarHotspotFailure) : Exception()

internal interface CarHotspotWifiAccess {
    fun readConfiguration(): CarHotspotConfiguration?
    fun readState(): Int?
    fun requestEnable(configuration: CarHotspotConfiguration): Boolean
}

@SuppressLint("PrivateApi")
private class AndroidCarHotspotWifiAccess(
    private val wifiManager: WifiManager,
) : CarHotspotWifiAccess {
    override fun readConfiguration(): CarHotspotConfiguration? {
        val native = WifiManager::class.java.getMethod("getWifiApConfiguration").invoke(wifiManager)
            as? WifiConfiguration ?: return null
        return CarHotspotTools.configurationFrom(native)
    }

    override fun readState(): Int? {
        val state = runCatching {
            WifiManager::class.java.getMethod("getWifiApState").invoke(wifiManager) as Int
        }.getOrNull()
        if (state != null) return state

        val enabled = WifiManager::class.java.getMethod("isWifiApEnabled").invoke(wifiManager) as Boolean
        return if (enabled) CarHotspotTools.AOSP_STATE_ENABLED else CarHotspotTools.AOSP_STATE_DISABLED
    }

    override fun requestEnable(configuration: CarHotspotConfiguration): Boolean {
        val method = WifiManager::class.java.getMethod(
            "setWifiApEnabled",
            WifiConfiguration::class.java,
            java.lang.Boolean.TYPE,
        )
        return method.invoke(wifiManager, configuration.nativeConfiguration, true) as? Boolean ?: false
    }

}
