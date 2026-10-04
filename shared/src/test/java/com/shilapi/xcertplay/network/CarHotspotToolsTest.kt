package com.shilapi.xcertplay.network

import android.net.wifi.WifiConfiguration
import com.shilapi.xcertplay.orchestration.ManualHotspotBand
import com.shilapi.xcertplay.orchestration.ManualHotspotSecurity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class CarHotspotToolsTest {
    @Test
    fun readsWpaAndOpenConfigurationWithoutNormalizingTheStoredStrings() {
        val securedNative = WifiConfiguration().apply {
            SSID = "\"quoted ssid\""
            preSharedKey = "\"quoted secret\""
            allowedKeyManagement.clear()
            allowedKeyManagement.set(1) // WPA_PSK
        }
        val secured = CarHotspotTools.configurationFrom(securedNative, bandValue = null, channelValue = null)
        assertNotNull(secured)
        assertEquals("\"quoted ssid\"", secured!!.ssid)
        assertEquals("\"quoted secret\"", secured.passphrase)
        assertEquals(ManualHotspotSecurity.WPA2, secured.security)
        assertNull(secured.band)
        assertNull(secured.channel)
        assertFalse(secured.toString().contains("quoted secret"))

        val openNative = WifiConfiguration().apply {
            SSID = "Guest"
            allowedKeyManagement.clear()
            allowedKeyManagement.set(0) // NONE
        }
        val open = CarHotspotTools.configurationFrom(openNative, bandValue = null, channelValue = null)
        assertNotNull(open)
        assertEquals("Guest", open!!.ssid)
        assertEquals("", open.passphrase)
        assertEquals(ManualHotspotSecurity.OPEN, open.security)
    }

    @Test
    fun knownBandAndChannelAreMappedAndUnknownFieldsStayUnknown() {
        val native = WifiConfiguration().apply {
            SSID = "car"
            preSharedKey = "12345678"
            allowedKeyManagement.clear()
            allowedKeyManagement.set(4) // WPA2_PSK
        }
        val known = CarHotspotTools.configurationFrom(native, bandValue = 1, channelValue = 44)
        assertNotNull(known)
        assertEquals(ManualHotspotBand.GHZ_5, known!!.band)
        assertEquals(44, known.channel)
        assertEquals(ManualHotspotSecurity.WPA2, known.security)
        assertEquals(
            ManualHotspotBand.AUTO,
            CarHotspotTools.configurationFrom(native, bandValue = -1, channelValue = 0)?.band,
        )

        val unknown = CarHotspotTools.configurationFrom(native, bandValue = null, channelValue = null)
        assertNotNull(unknown)
        assertNull(unknown!!.band)
        assertNull(unknown.channel)
    }

    @Test
    fun alreadyEnabledAndAlreadyStartingNeverSubmitASecondEnableRequest() {
        val alreadyEnabled = FakeWifiAccess(configuration(), states = listOf(13))
        assertTrue(CarHotspotTools.start(alreadyEnabled, 1000, 100, {}).isAlreadyEnabled())
        assertEquals(0, alreadyEnabled.enableRequests)

        var now = 0L
        val starting = FakeWifiAccess(configuration(), states = listOf(12, 12, 13))
        val result = CarHotspotTools.start(
            starting,
            timeoutMillis = 1000,
            pollIntervalMillis = 100,
            pause = { now += it },
            cancelled = { false },
            nowMillis = { now },
        )
        assertTrue(result.isEnabled())
        assertEquals(0, starting.enableRequests)
    }

    @Test
    fun missingConfigurationNeverCallsSetter() {
        val access = FakeWifiAccess(configurationValue = null, states = listOf(11))
        val result = CarHotspotTools.start(access, 1000, 100, {})
        assertFailure(CarHotspotFailure.CONFIGURATION_UNAVAILABLE, result)
        assertEquals(0, access.stateReads)
        assertEquals(0, access.enableRequests)
    }

    @Test
    fun cancellationWhileStateReadIsBlockedPreventsTheLaterSetterCall() {
        var cancelled = false
        var requests = 0
        val access = object : CarHotspotWifiAccess {
            override fun readConfiguration(): CarHotspotConfiguration = configuration()
            override fun readState(): Int {
                cancelled = true
                return 11
            }
            override fun requestEnable(configuration: CarHotspotConfiguration): Boolean {
                requests++
                return true
            }
        }
        val result = CarHotspotTools.start(access, 1000, 100, {}, cancelled = { cancelled })
        assertFailure(CarHotspotFailure.OPERATION_CANCELLED, result)
        assertEquals(0, requests)
    }

    @Test
    fun acceptedRequestIsNotReadyUntilStateReportsEnabled() {
        var now = 0L
        val access = FakeWifiAccess(configuration(), states = listOf(11, 12, 13))
        val result = CarHotspotTools.start(
            access,
            timeoutMillis = 1000,
            pollIntervalMillis = 100,
            pause = { now += it },
            cancelled = { false },
            nowMillis = { now },
        )
        assertTrue(result.isEnabled())
        assertEquals(1, access.enableRequests)
        assertSame(access.expectedConfiguration.nativeConfiguration, access.configurationPassedToSetter)
    }

    @Test
    fun timeoutDoesNotRetryAnAcceptedStartRequest() {
        var now = 0L
        val access = FakeWifiAccess(configuration(), states = listOf(11, 12, 11, 12, 11))
        val result = CarHotspotTools.start(
            access,
            timeoutMillis = 250,
            pollIntervalMillis = 100,
            pause = { now += it },
            cancelled = { false },
            nowMillis = { now },
        )
        assertFailure(CarHotspotFailure.START_TIMED_OUT, result)
        assertEquals(1, access.enableRequests)
    }

    @Test
    fun rejectedAndPermissionDeniedRequestsAreReportedSeparately() {
        val rejected = FakeWifiAccess(configuration(), states = listOf(11)).apply { acceptRequest = false }
        assertFailure(CarHotspotFailure.REQUEST_REJECTED, CarHotspotTools.start(rejected, 1000, 100, {}))
        assertEquals(1, rejected.enableRequests)

        val denied = FakeWifiAccess(configuration(), states = listOf(11)).apply { throwOnEnable = SecurityException() }
        assertFailure(CarHotspotFailure.PERMISSION_DENIED, CarHotspotTools.start(denied, 1000, 100, {}))
        assertEquals(1, denied.enableRequests)
    }

    @Test
    fun api18AndAospStateNumbersMapToTheSameStates() {
        assertEquals(CarHotspotState.DISABLING, CarHotspotTools.stateFromRaw(0))
        assertEquals(CarHotspotState.DISABLED, CarHotspotTools.stateFromRaw(1))
        assertEquals(CarHotspotState.ENABLING, CarHotspotTools.stateFromRaw(2))
        assertEquals(CarHotspotState.ENABLED, CarHotspotTools.stateFromRaw(3))
        assertEquals(CarHotspotState.FAILED, CarHotspotTools.stateFromRaw(4))
        assertEquals(CarHotspotState.DISABLED, CarHotspotTools.stateFromRaw(11))
        assertEquals(CarHotspotState.ENABLED, CarHotspotTools.stateFromRaw(13))
        assertEquals(CarHotspotState.FAILED, CarHotspotTools.stateFromRaw(14))
        assertEquals(CarHotspotState.UNKNOWN, CarHotspotTools.stateFromRaw(99))
    }

    @Test
    fun unknownSecurityDoesNotGetMisreportedAsWpa2() {
        val native = WifiConfiguration().apply {
            SSID = "head unit"
            preSharedKey = "secret"
            allowedKeyManagement.clear()
            allowedKeyManagement.set(2) // EAP is not a supported AP security type here.
        }
        val access = object : CarHotspotWifiAccess {
            override fun readConfiguration(): CarHotspotConfiguration? =
                CarHotspotTools.configurationFrom(native, bandValue = null, channelValue = null)
            override fun readState(): Int? = 11
            override fun requestEnable(configuration: CarHotspotConfiguration): Boolean = error("must not start")
        }
        val result = CarHotspotTools.read(access)
        assertTrue(result is CarHotspotReadResult.Failure)
        assertEquals(
            CarHotspotFailure.UNSUPPORTED_CONFIGURATION,
            (result as CarHotspotReadResult.Failure).reason,
        )
    }

    private fun configuration(): CarHotspotConfiguration = CarHotspotConfiguration(
        ssid = "head unit",
        passphrase = "secret-not-logged",
        security = ManualHotspotSecurity.WPA2,
        band = null,
        channel = null,
        nativeConfiguration = Any(),
    )

    private fun assertFailure(expected: CarHotspotFailure, result: CarHotspotStartResult) {
        assertTrue("expected $expected, got $result", result is CarHotspotStartResult.Failure)
        assertEquals(expected, (result as CarHotspotStartResult.Failure).reason)
    }

    private fun CarHotspotStartResult.isEnabled(): Boolean = this is CarHotspotStartResult.Enabled
    private fun CarHotspotStartResult.isAlreadyEnabled(): Boolean = this is CarHotspotStartResult.AlreadyEnabled

    private class FakeWifiAccess(
        private val configurationValue: CarHotspotConfiguration?,
        private val states: List<Int>,
    ) : CarHotspotWifiAccess {
        private var nextState = 0
        var stateReads = 0
        var enableRequests = 0
        var acceptRequest = true
        var throwOnEnable: SecurityException? = null
        var configurationPassedToSetter: Any? = null
        val expectedConfiguration: CarHotspotConfiguration
            get() = requireNotNull(configurationValue)

        override fun readConfiguration(): CarHotspotConfiguration? = configurationValue

        override fun readState(): Int? {
            stateReads++
            return states.getOrNull(nextState++) ?: states.lastOrNull()
        }

        override fun requestEnable(configuration: CarHotspotConfiguration): Boolean {
            enableRequests++
            configurationPassedToSetter = configuration.nativeConfiguration
            throwOnEnable?.let { throw it }
            return acceptRequest
        }
    }
}
