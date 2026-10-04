package com.shilapi.xcertplay.mfi

import com.shilapi.xcertplay.transport.I2cTransport
import com.shilapi.xcertplay.transport.I2cTransportException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MfiSelfCheckTest {
    @Test
    fun ignoresFloatingBusValueAndSelectsRealSecondCandidate() {
        var selectedRegister = -1
        val transport = object : I2cTransport {
            override fun transaction(address7Bit: Int, writeData: ByteArray, readLength: Int): ByteArray {
                if (writeData.isNotEmpty()) {
                    selectedRegister = writeData.single().toInt() and 0xff
                    return ByteArray(0)
                }
                return when {
                    selectedRegister != 0x00 || readLength != 1 ->
                        throw I2cTransportException.Nack("unexpected request")
                    address7Bit == 0x10 -> bytes(0xff)
                    address7Bit == 0x11 -> bytes(0x07)
                    else -> throw I2cTransportException.Nack("unexpected address")
                }
            }
        }

        val result = MfiDeviceScanner(transport).scan()

        assertEquals(0x11, result.chip?.address7Bit)
        assertEquals(0x07, result.chip?.deviceVersion)
        assertTrue(result.failures.single().error is MfiDiscoveryError.InvalidDeviceVersion)
    }

    @Test
    fun fallsBackToSecondCandidateThenReportsRawProtocolMajor() {
        val calls = mutableListOf<Pair<Int, Int>>()
        val transport = object : I2cTransport {
            override fun transaction(address7Bit: Int, writeData: ByteArray, readLength: Int): ByteArray {
                val register = writeData.firstOrNull()?.toInt()?.and(0xff) ?: -1
                calls += address7Bit to register
                return when (address7Bit to register) {
                    0x10 to 0x00 -> throw I2cTransportException.Nack("first candidate did not respond")
                    0x11 to 0x00, 0x11 to 0x02 -> ByteArray(0)
                    0x11 to -1 -> when (readLength) {
                        1 -> if (calls.count { it == 0x11 to -1 } == 1) bytes(3) else bytes(7)
                        else -> throw I2cTransportException.Nack("unexpected read length")
                    }
                    else -> throw I2cTransportException.Nack("unexpected request")
                }
            }
        }

        val result = MfiSelfCheck(transport).run()

        assertEquals(
            listOf(0x10 to 0x00, 0x11 to 0x00, 0x11 to -1, 0x11 to 0x02, 0x11 to -1),
            calls,
        )
        assertEquals(0x11, result.discovery.chip?.address7Bit)
        assertEquals(3, result.discovery.chip?.deviceVersion)
        assertEquals(0x10, result.discovery.failures.single().address7Bit)
        assertTrue(result.chip?.protocolMajor is MfiProtocolMajorResult.Value)
        assertEquals(7, (result.chip?.protocolMajor as MfiProtocolMajorResult.Value).major)
    }

    private fun bytes(vararg values: Int): ByteArray = ByteArray(values.size) { values[it].toByte() }
}
