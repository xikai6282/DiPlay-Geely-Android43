package com.shilapi.xcertplay.transport

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Ch341DeviceMatcherTest {
    @Test
    fun matchesOnlyConfiguredUsbIdentities() {
        val matcher = Ch341DeviceMatcher(listOf(UsbDeviceId(0x1A86, 0x5512)))

        assertTrue(matcher.matches(0x1A86, 0x5512))
        assertFalse(matcher.matches(0x1A86, 0x5513))
        assertFalse(matcher.matches(0x1234, 0x5512))
    }

}
