package com.shilapi.xcertplay

import org.junit.Assert.*
import org.junit.Test

class ConnectionKeyDiagnosticsTest {
    @Test fun laterLogsCannotEraseLastConnectionResult() {
        val tracker = ConnectionKeyDiagnostics()
        tracker.update("H52 ANW connect request result=1 index=2 uuidEncoding=canonical-big-endian")
        tracker.update("H52 ANW connect observation index=2 result=1 state=0 addressPresent=true addressMatches=false")
        tracker.update("Other log text")
        assertTrue(tracker.summary().contains("index=2"))
        assertTrue(tracker.summary().contains("state=0"))
        assertTrue(tracker.summary().contains("目标匹配=false"))
    }
    @Test fun newRequestCannotCarryOldSlotState() {
        val tracker = ConnectionKeyDiagnostics()
        tracker.update("H52 ANW connect observation index=0 result=1 state=1 addressPresent=true addressMatches=true")
        tracker.update("H52 ANW connect request result=1 index=3")
        assertFalse(tracker.summary().contains("state=1"))
    }
    @Test fun usbErrnoIsRetainedWithoutRawCredentials() {
        val tracker = ConnectionKeyDiagnostics()
        tracker.update("USBDEVFS_CLAIMINTERFACE iface=3 result=-16 errno=16")
        tracker.update("password=private-secret")
        assertTrue(tracker.summary().contains("errno=16"))
        assertFalse(tracker.summary().contains("private-secret"))
    }
}
