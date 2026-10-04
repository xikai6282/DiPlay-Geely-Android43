package com.shilapi.xcertplay

import android.content.Context
import com.shilapi.xcertplay.host.R

/** Translate presentation labels without changing protocol or diagnostic values. */
internal fun Context.hotspotBackendLabel(value: String): String = when (value) {
    "Wi-Fi P2P", "Wi-Fi Direct" -> getString(R.string.wifi_direct)
    "LocalOnlyHotspot" -> getString(R.string.localonlyhotspot)
    "Manual hotspot" -> getString(R.string.manual_hotspot)
    else -> value
}

internal fun Context.hotspotBandLabel(value: String): String = when (value) {
    "2.4 / 5 GHz (auto)" -> "2.4 / 5 GHz (${getString(R.string.auto_value)})"
    "auto", "Auto" -> getString(R.string.auto_value)
    else -> value
}
