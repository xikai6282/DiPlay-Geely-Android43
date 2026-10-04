package com.shilapi.xcertplay.orchestration

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ManualHotspotValidationTest {
    @Test
    fun `accepts a WPA2 car hotspot and an open one`() {
        assertNull(ManualHotspotValidation.validate("BYD_1234", "12345678"))
        assertNull(ManualHotspotValidation.validate("BYD_1234", ""))
        assertEquals(ManualHotspotSecurity.WPA2, ManualHotspotValidation.securityFor("12345678"))
        assertEquals(ManualHotspotSecurity.OPEN, ManualHotspotValidation.securityFor(""))
    }

    @Test
    fun `rejects what the iPhone cannot join`() {
        assertNotNull(ManualHotspotValidation.validate(" ", "12345678"))
        assertNotNull(ManualHotspotValidation.validate("BYD", "short"))
        assertNotNull(ManualHotspotValidation.validate("x".repeat(33), "12345678"))
        assertNotNull(ManualHotspotValidation.validate("BYD", "a".repeat(64)))
    }
}
