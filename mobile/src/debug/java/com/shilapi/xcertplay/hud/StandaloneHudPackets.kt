package com.shilapi.xcertplay.hud

/** Fixed navigation-only vectors matching the inspected IVI HAL. Debug builds only. */
internal object StandaloneHudPackets {
    fun start(): String = "43,E0,00,3A,01,02"
    fun clear(): String = "43,E0,00,3A,01,01"
    fun guidance(turn: Int, distanceMeters: Int): String {
        require(turn in setOf(1, 2, 11)) { "Demo permits only left, right and straight" }
        require(distanceMeters in 0..9999) { "Invalid demo distance" }
        return (record(0x43F01018, distanceMeters) + record(0x43F01010, turn) +
            record(0x43F01030, turn)).joinToString(",") { "%02X".format(it.toInt() and 255) }
    }
    private fun record(feature: Int, value: Int): ByteArray = byteArrayOf(
        (feature ushr 24).toByte(), (feature ushr 16).toByte(),
        (feature ushr 8).toByte(), feature.toByte(), 4,
        (value ushr 24).toByte(), (value ushr 16).toByte(),
        (value ushr 8).toByte(), value.toByte(),
    )
}
