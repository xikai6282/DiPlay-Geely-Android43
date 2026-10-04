package com.shilapi.xcertplay.mfi

import com.shilapi.xcertplay.transport.I2cTransport
import com.shilapi.xcertplay.transport.I2cTransportException

/**
 * Finds the first responding MFi authentication coprocessor.
 *
 * LIVI probes `0x10`, then `0x11`, by selecting device-version register `0x00` with STOP and
 * issuing a separate pure read. It retries that candidate cycle for at most two seconds and
 * returns immediately on the first valid response. Call this blocking scanner off the main thread.
 */
class MfiDeviceScanner(
    private val transport: I2cTransport,
    candidateAddresses: List<Int> = DEFAULT_CANDIDATE_ADDRESSES,
    private val probeTimeoutMillis: Long = DEFAULT_PROBE_TIMEOUT_MILLIS,
) {
    private val candidateAddresses = candidateAddresses.toList()

    init {
        require(probeTimeoutMillis > 0) { "probeTimeoutMillis must be positive" }
    }

    fun scan(): MfiDiscoveryResult {
        val failures = LinkedHashMap<Int, MfiDiscoveryError>()
        val validCandidates = candidateAddresses.filter { address7Bit ->
            if (address7Bit in MIN_ADDRESS_7_BIT..MAX_ADDRESS_7_BIT) {
                true
            } else {
                failures[address7Bit] = MfiDiscoveryError.InvalidCandidateAddress(address7Bit)
                false
            }
        }
        if (validCandidates.isEmpty()) return MfiDiscoveryResult(null, failures.toFailures())

        val deadlineNanos = deadlineAfter(probeTimeoutMillis)
        while (true) {
            for (address7Bit in validCandidates) {
                when (val result = readDeviceVersion(address7Bit)) {
                    is DeviceVersionRead.Success -> {
                        return MfiDiscoveryResult(
                            MfiDiscoveredChip(
                                address7Bit = address7Bit,
                                writeAddress8Bit = address7Bit shl 1,
                                readAddress8Bit = (address7Bit shl 1) or READ_BIT,
                                deviceVersion = result.value,
                            ),
                            failures.toFailures(),
                        )
                    }
                    is DeviceVersionRead.Failure -> failures[address7Bit] = result.error
                }
            }
            if (System.nanoTime() >= deadlineNanos) return MfiDiscoveryResult(null, failures.toFailures())
            if (!sleepMicros(RETRY_DELAY_MICROS)) {
                return MfiDiscoveryResult(null, failures.toFailures(), interrupted = true)
            }
        }
    }

    private fun readDeviceVersion(address7Bit: Int): DeviceVersionRead = try {
        val selectResponse = transport.transaction(address7Bit, byteArrayOf(DEVICE_VERSION_REGISTER.toByte()), 0)
        if (selectResponse.isNotEmpty()) {
            DeviceVersionRead.Failure(MfiDiscoveryError.UnexpectedResponseLength(0, selectResponse.size))
        } else {
            val value = transport.transaction(address7Bit, ByteArray(0), 1)
            if (value.size != 1) {
                DeviceVersionRead.Failure(MfiDiscoveryError.UnexpectedResponseLength(1, value.size))
            } else {
                val deviceVersion = value[0].toInt() and BYTE_MASK
                if (deviceVersion == 0x00 || deviceVersion == 0xff) {
                    DeviceVersionRead.Failure(MfiDiscoveryError.InvalidDeviceVersion(deviceVersion))
                } else {
                    DeviceVersionRead.Success(deviceVersion)
                }
            }
        }
    } catch (error: I2cTransportException) {
        DeviceVersionRead.Failure(MfiDiscoveryError.Transport(error))
    }

    private fun deadlineAfter(millis: Long): Long {
        val now = System.nanoTime()
        val delta = millis * NANOS_PER_MILLISECOND
        return if (Long.MAX_VALUE - now < delta) Long.MAX_VALUE else now + delta
    }

    private fun sleepMicros(micros: Long): Boolean = try {
        Thread.sleep(micros / MICROS_PER_MILLISECOND, ((micros % MICROS_PER_MILLISECOND) * NANOS_PER_MICROSECOND).toInt())
        true
    } catch (_: InterruptedException) {
        Thread.currentThread().interrupt()
        false
    }

    private fun LinkedHashMap<Int, MfiDiscoveryError>.toFailures(): List<MfiDiscoveryFailure> =
        entries.map { (address7Bit, error) -> MfiDiscoveryFailure(address7Bit, error) }

    private sealed class DeviceVersionRead {
        data class Success(val value: Int) : DeviceVersionRead()

        data class Failure(val error: MfiDiscoveryError) : DeviceVersionRead()
    }

    companion object {
        /** Documented MFi candidates in LIVI probe order. */
        val DEFAULT_CANDIDATE_ADDRESSES = listOf(0x10, 0x11)

        private const val MIN_ADDRESS_7_BIT = 0x00
        private const val MAX_ADDRESS_7_BIT = 0x7f
        private const val DEVICE_VERSION_REGISTER = 0x00
        private const val READ_BIT = 0x01
        private const val BYTE_MASK = 0xff
        private const val DEFAULT_PROBE_TIMEOUT_MILLIS = 2_000L
        // A first NACK can merely wake a sleeping authentication coprocessor.
        private const val RETRY_DELAY_MICROS = 20_000L
        private const val MICROS_PER_MILLISECOND = 1_000L
        private const val NANOS_PER_MICROSECOND = 1_000L
        private const val NANOS_PER_MILLISECOND = 1_000_000L
    }
}

/** Structured outcome of a first-match MFi device probe. */
data class MfiDiscoveryResult(
    val chip: MfiDiscoveredChip?,
    val failures: List<MfiDiscoveryFailure>,
    val interrupted: Boolean = false,
)

/** The first candidate that returned a one-byte device version. */
data class MfiDiscoveredChip(
    val address7Bit: Int,
    val writeAddress8Bit: Int,
    val readAddress8Bit: Int,
    val deviceVersion: Int,
)

/** The most recent failure retained for a probe candidate. */
data class MfiDiscoveryFailure(
    val address7Bit: Int,
    val error: MfiDiscoveryError,
)

/** Failure categories retained for caller-side diagnostics without a logging dependency. */
sealed class MfiDiscoveryError {
    data class InvalidCandidateAddress(val actual: Int) : MfiDiscoveryError()

    data class Transport(val cause: I2cTransportException) : MfiDiscoveryError()

    data class UnexpectedResponseLength(val expected: Int, val actual: Int) : MfiDiscoveryError()

    /** CH341 returns all-zero/all-one bus data when no usable coprocessor answered. */
    data class InvalidDeviceVersion(val actual: Int) : MfiDiscoveryError()

}
