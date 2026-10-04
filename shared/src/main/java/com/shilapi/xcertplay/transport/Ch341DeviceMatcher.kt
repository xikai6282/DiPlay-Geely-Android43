package com.shilapi.xcertplay.transport

/** A USB identity explicitly allowed by the product or deployment configuration. */
data class UsbDeviceId(val vendorId: Int, val productId: Int)

/**
 * Matches only configured CH341 USB identities. There is intentionally no built-in VID/PID:
 * deployed CH341 hardware must be identified on the target unit first.
 */
class Ch341DeviceMatcher(allowedDevices: Collection<UsbDeviceId>) {
    private val allowedDevices = allowedDevices.toSet()

    fun matches(vendorId: Int, productId: Int): Boolean =
        UsbDeviceId(vendorId, productId) in allowedDevices
}
