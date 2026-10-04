package com.shilapi.xcertplay.transport

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Ch341I2cStreamEncoderTest {
    @Test
    fun encodesWriteReadWithRepeatedStartAndFinalReadNack() {
        assertArrayEquals(
            bytes(0xaa, 0x74, 0x80, 0x22, 0x80, 0x30, 0x74, 0x80, 0x23, 0xc1, 0xc0, 0x75, 0x00),
            Ch341I2cStreamEncoder.transaction(0x11, bytes(0x30), 2),
        )
    }

    @Test
    fun encodesMaximumPureReadWithOnlyFinalNackAndStop() {
        val stream = Ch341I2cStreamEncoder.transaction(
            0x11,
            ByteArray(0),
            Ch341I2cStreamEncoder.MAX_READ_BYTES,
        )

        assertEquals(1, stream.count { it.toInt() and 0xff == 0x75 })
        // The read-address setup of a segmented read is one batched `0x80|1` command, matching the
        // hardware-proven reference (mfi3.py: `[START, OUT | 1, address]`), whose reply carries the
        // requested data bytes with no leading ACK/NACK status byte.
        assertArrayEquals(bytes(0xaa, 0x74, 0x81, 0x23), stream.copyOfRange(0, 4))
        assertArrayEquals(bytes(0xde, 0xc0, 0x75, 0x00), stream.takeLast(4).toByteArray())
        assertTrue(Ch341I2cStreamEncoder.transaction(0x11, ByteArray(129), 0).isNotEmpty())
        for (offset in 0 until stream.size - Ch341I2cStreamEncoder.MAX_STREAM_PACKET_BYTES step Ch341I2cStreamEncoder.MAX_STREAM_PACKET_BYTES) {
            assertEquals(0xaa, stream[offset].toInt() and 0xff)
            assertEquals(0, stream[offset + Ch341I2cStreamEncoder.MAX_STREAM_PACKET_BYTES - 1].toInt() and 0xff)
        }
        assertTrue(stream.size > Ch341I2cStreamEncoder.MAX_STREAM_PACKET_BYTES)
    }

    private fun bytes(vararg values: Int): ByteArray = ByteArray(values.size) { values[it].toByte() }
}
