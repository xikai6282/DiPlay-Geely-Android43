package com.shilapi.xcertplay

import android.content.Context

/** Localized copy for the controller's unchanged, strict Android RFCOMM preflight failures. */
internal object AndroidBluetoothFailureCopy {
    fun forControllerMessage(context: Context, message: String): String? = when (message) {
        "Bluetooth adapter is unavailable", "Bluetooth is not enabled" ->
            context.getString(com.shilapi.xcertplay.host.R.string.android_bluetooth_transport_unavailable)
        else -> null
    }
}
