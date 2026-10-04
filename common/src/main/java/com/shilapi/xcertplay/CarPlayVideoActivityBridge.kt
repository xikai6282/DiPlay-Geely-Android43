package com.shilapi.xcertplay

import com.shilapi.xcertplay.airplay.VideoInCar

/** API-18-safe seam between the host state object and the API-23 Media3 Activity. */
internal interface CarPlayVideoActivityBridge {
    fun load()
    fun applyRate()
    fun skip(deltaMillis: Int)
    fun applySeek()
    fun state(): VideoInCar.PlayerState
    fun requestFinish()
}
