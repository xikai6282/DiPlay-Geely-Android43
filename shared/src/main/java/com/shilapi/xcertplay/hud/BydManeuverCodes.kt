package com.shilapi.xcertplay.hud

/**
 * Apple iAP2 RouteGuidanceManeuverType -> Gaode maneuver code, the vocabulary of the BYD HUD
 * icon set (assets/byd-hud-icons/0x<code>.png) and of the native HUD arrow.
 */
internal object BydManeuverCodes {
    const val NONE = 0
    const val LEFT = 1
    const val RIGHT = 2
    const val SLIGHT_LEFT = 3
    const val SLIGHT_RIGHT = 4
    const val SHARP_LEFT = 7
    const val SHARP_RIGHT = 8
    const val U_TURN_LEFT = 9
    const val U_TURN_RIGHT = 10
    const val STRAIGHT = 11
    const val ROUNDABOUT_ENTER = 13
    const val ROUNDABOUT_EXIT = 24
    const val DESTINATION = 48

    private const val ROUNDABOUT_EXIT_BASE = 24 // + exit 1..10, right-hand traffic
    private const val ROUNDABOUT_EXIT_BASE_LEFT_HAND = 34 // + exit 1..10, left-hand traffic

    /** [drivingSide] 1 is Apple's left-hand driving. */
    fun gaode(appleType: Int, drivingSide: Int): Int {
        val leftHand = drivingSide == 1
        if (appleType in 28..46) {
            val exit = appleType - 27
            return when {
                exit > 10 -> ROUNDABOUT_ENTER
                leftHand -> ROUNDABOUT_EXIT_BASE_LEFT_HAND + exit
                else -> ROUNDABOUT_EXIT_BASE + exit
            }
        }
        return when (appleType) {
            1, 20 -> LEFT
            2, 21 -> RIGHT
            47 -> SHARP_LEFT
            48 -> SHARP_RIGHT
            13, 22, 49, 52 -> SLIGHT_LEFT
            14, 23, 50, 53 -> SLIGHT_RIGHT
            4, 18, 19, 26 -> if (leftHand) U_TURN_RIGHT else U_TURN_LEFT
            6 -> ROUNDABOUT_ENTER
            7 -> ROUNDABOUT_EXIT
            10, 12, 24, 25, 27 -> DESTINATION
            3, 5, 8, 9, 11, 51 -> STRAIGHT
            else -> NONE
        }
    }

    /**
     * Native HUD arrow (field 28): 1 left, 2 right, 3 slight left, 5 slight right, 7/8 U-turn,
     * 11 straight, 99 blank. Anything else renders as a phantom left arrow, so glyph-less
     * maneuvers (roundabouts, destination) are blank and rely on the field-8 icon; 0 clears.
     */
    fun hudArrow(gaode: Int): Int = when (gaode) {
        NONE -> 0
        LEFT, SHARP_LEFT -> 1
        RIGHT, SHARP_RIGHT -> 2
        SLIGHT_LEFT -> 3
        SLIGHT_RIGHT -> 5
        U_TURN_LEFT -> 7
        U_TURN_RIGHT -> 8
        STRAIGHT -> 11
        else -> 99
    }
}
