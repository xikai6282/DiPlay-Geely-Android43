package com.shilapi.xcertplay.transport

import java.io.IOException

/** Once USBMUX is claimed, NCM must use that configuration without resetting the device. */
internal object NcmConfigurationGuard {
    fun requireActive(active: Int?, expected: Int) {
        if (expected !in 1..255 || active != expected) {
            throw IOException("USB configuration changed or could not be confirmed after USBMUX opened: expected=$expected active=${active ?: "unknown"}. Reconnect the iPhone.")
        }
    }
}
