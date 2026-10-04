package com.shilapi.xcertplay.transport

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class LinuxI2cTransportTest {
    @Test
    fun transactionDelegatesCombinedReadAndMapsNackBeforeClose() {
        val bridge = RecordingBridge(response = byteArrayOf(0x12, 0x34))
        val transport = LinuxI2cTransport.open("/dev/i2c-2", bridge)

        assertArrayEquals(byteArrayOf(0x12, 0x34), transport.transaction(0x11, byteArrayOf(0x30), 2))
        assertEquals("/dev/i2c-2", bridge.openedPath)
        assertEquals(0x11, bridge.address)
        assertArrayEquals(byteArrayOf(0x30), bridge.writeData)
        assertEquals(2, bridge.readLength)

        try {
            LinuxI2cTransport.open(
                "/dev/i2c-1",
                RecordingBridge(error = LinuxI2cNativeException(121, "remote error")),
            ).transaction(0x11, byteArrayOf(1), 0)
            fail("expected Nack")
        } catch (_: I2cTransportException.Nack) {
        }

        transport.close()
        assertEquals(7, bridge.closedHandle)
        try {
            transport.transaction(0x11, byteArrayOf(1), 0)
            fail("expected closed transport")
        } catch (_: I2cTransportException.DeviceUnavailable) {
        }
    }

    private class RecordingBridge(
        private val response: ByteArray = byteArrayOf(),
        private val error: LinuxI2cNativeException? = null,
    ) : LinuxI2cBridge {
        var openedPath: String? = null
        var address: Int? = null
        var writeData: ByteArray? = null
        var readLength: Int? = null
        var closedHandle: Int? = null

        override fun open(devicePath: String): Int {
            openedPath = devicePath
            return 7
        }

        override fun transaction(
            handle: Int,
            address7Bit: Int,
            writeData: ByteArray,
            readLength: Int,
        ): ByteArray {
            address = address7Bit
            this.writeData = writeData
            this.readLength = readLength
            error?.let { throw it }
            return response
        }

        override fun close(handle: Int) {
            closedHandle = handle
        }
    }
}
