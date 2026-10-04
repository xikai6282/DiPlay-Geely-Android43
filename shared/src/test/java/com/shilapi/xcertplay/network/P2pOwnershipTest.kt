package com.shilapi.xcertplay.network

import org.junit.Assert.*
import org.junit.Test

class P2pOwnershipTest {
    @Test fun reinstallWithoutPreferencesCanReclaimOnlyItsOwnNamespace() {
        val prefix = P2pOwnership.prefix("com.shihab.diplay", "device-user-signing-key-id")
        val ssid = prefix + "aB23"
        assertTrue(P2pOwnership.canReclaim(true, ssid, null, prefix))
        assertFalse(P2pOwnership.canReclaim(false, ssid, null, prefix))
        assertFalse(P2pOwnership.canReclaim(true, ssid, null, P2pOwnership.prefix("other.app", "device-user-signing-key-id")))
        assertFalse(P2pOwnership.canReclaim(true, ssid, null, P2pOwnership.prefix("com.shihab.diplay", "other-device")))
        assertTrue(ssid.length <= 32)
    }

    @Test fun legacyGroupNeedsExactRecordAndUnknownGroupsNeedExplicitReset() {
        val prefix = P2pOwnership.prefix("app", "id")
        assertTrue(P2pOwnership.canReclaim(true, "DIRECT-xcaB23", "DIRECT-xcaB23", prefix))
        assertFalse(P2pOwnership.canReclaim(true, "DIRECT-xcaB23", null, prefix))
        assertFalse(P2pOwnership.canReclaim(true, null, null, prefix))
        assertFalse(P2pOwnership.canReclaim(true, prefix + "", null, prefix))
        assertFalse(P2pOwnership.canReclaim(true, prefix + "aB23-extra", null, prefix))
    }
}
