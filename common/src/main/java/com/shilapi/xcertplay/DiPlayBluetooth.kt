package com.shilapi.xcertplay

import com.shilapi.xcertplay.compat.systemService
import android.bluetooth.BluetoothManager
import android.content.Context
import android.provider.Settings

internal object DiPlayBluetooth {
    fun localAddress(context: Context): String? {
        val adapter = runCatching { context.systemService(BluetoothManager::class.java, "bluetooth")?.adapter?.address }.getOrNull()
        val setting = runCatching { Settings.Secure.getString(context.contentResolver, "bluetooth_address") }.getOrNull()
        return listOfNotNull(adapter, setting).firstOrNull {
            Regex("(?i)([0-9a-f]{2}:){5}[0-9a-f]{2}").matches(it) &&
                !it.startsWith("02:00:00:00:00:") && it != "00:00:00:00:00:00"
        }
    }
}
