package com.shilapi.xcertplay.hud

import org.junit.Assert.assertArrayEquals
import org.junit.Test

class BydHudPayloadTest {
    @Test
    fun guidanceMatchesFixedFieldTwoWireFixture() {
        assertArrayEquals(
            byteArrayOf(
                0x0a, 0x15, 0x10, 0x02, 0x30, 0x01, 0x3a, 0x00, 0x42, 0x00,
                0x48, 0x35, 0x52, 0x00, 0x80.toByte(), 0x01, 0x02,
                0xd2.toByte(), 0x01, 0x00, 0xe0.toByte(), 0x01, 0x01,
            ),
            BydHudPayload.guidance(distanceMeters = 53, maneuver = 1),
        )
    }

    @Test
    fun shortDistanceIsLiftedAndTextIsCarried() {
        val payload = BydHudPayload.guidance(distanceMeters = 4, maneuver = 11, road = "A", eta = "14:36")
        val hex = payload.joinToString("") { "%02x".format(it) }
        assert(hex.contains("480b")) { hex } // field 9 = 11 m
        assert(hex.contains("520141")) { hex } // field 10 = "A"
        assert(hex.contains("d20105" + "31343a3336")) { hex } // field 26 = "14:36"
    }

    @Test
    fun clearMatchesBydDirectClear() {
        assertArrayEquals(
            byteArrayOf(0x0a, 0x08, 0x10, 0x02, 0x30, 0xff.toByte(), 0x01, 0x80.toByte(), 0x01, 0x01),
            BydHudPayload.clear(),
        )
    }
}
