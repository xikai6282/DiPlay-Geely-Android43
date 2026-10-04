package com.shilapi.xcertplay.hud

/** Factory navigation's NEW_ICON -> instrument code mapping, verified against installed firmware. */
internal object BydFactoryTurnCode {
    private val codes = intArrayOf(0, 0, 1, 2, 3, 5, 7, 8, 9, 11, 45, 13, 24, 46, 47, 48, 49, 14, 23, 10, 12, 15, 18, 20, 22, 16, 17, 19, 21)
    fun map(icon: Int, exit: Int): Int? {
        if (icon !in 2..28) return null
        if (exit in 1..10) {
            if (icon == 11 || icon == 12) return exit + 24
            if (icon == 17 || icon == 18) return exit + 34
        }
        return codes[icon]
    }
}
