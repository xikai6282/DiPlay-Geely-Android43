package com.shilapi.xcertplay.hud

import org.junit.Assert.*
import org.junit.Test

class BydFactoryTurnCodeTest {
    @Test fun factoryUsesDifferentCodesFromBroadcasts() {
        assertEquals(1, BydFactoryTurnCode.map(2, 0))
        assertEquals(2, BydFactoryTurnCode.map(3, 0))
        assertEquals(5, BydFactoryTurnCode.map(5, 0))
        assertEquals(11, BydFactoryTurnCode.map(9, 0))
        assertEquals(48, BydFactoryTurnCode.map(15, 0))
    }
    @Test fun roundaboutDirectionAndBounds() {
        assertEquals(25, BydFactoryTurnCode.map(11, 1))
        assertEquals(34, BydFactoryTurnCode.map(12, 10))
        assertEquals(35, BydFactoryTurnCode.map(17, 1))
        assertEquals(44, BydFactoryTurnCode.map(18, 10))
        assertEquals(13, BydFactoryTurnCode.map(11, 11))
        assertEquals(14, BydFactoryTurnCode.map(17, 0))
    }
    @Test fun invalidInstructionsAreNotMadeIntoStraightArrows() {
        assertNull(BydFactoryTurnCode.map(-1, 0))
        assertNull(BydFactoryTurnCode.map(0, 0))
        assertNull(BydFactoryTurnCode.map(29, 0))
    }
}
