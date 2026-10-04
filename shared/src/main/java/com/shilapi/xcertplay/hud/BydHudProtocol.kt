package com.shilapi.xcertplay.hud

/** Shared wire contract; keep DiPlay and DiAuto copies identical apart from their package. */
internal object BydHudProtocol {
    const val SERVICE_TOPIC = 0x000B010A00010000L
    const val NAVIGATION_TOPIC = 0x0004010A00018001L
    const val REPEAT_MILLIS = 300L
    fun serviceStarted(code: Int): Boolean = code == 0 || code == 13
    fun eventAccepted(code: Int): Boolean = code == 0
}
