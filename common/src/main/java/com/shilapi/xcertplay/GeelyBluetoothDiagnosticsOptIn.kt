package com.shilapi.xcertplay

import android.content.Context

/** Central gate for every OEM Bluetooth lookup so the default path stays Android-only. */
internal object GeelyBluetoothDiagnosticsOptIn {
    fun enabled(context: Context): Boolean = AirPlayPersistence.loadGeelyBluetoothDiagnosticsEnabled(context)

    fun <T> runIfEnabled(context: Context, action: () -> T): T? {
        if (!enabled(context)) return null
        return action()
    }
}
