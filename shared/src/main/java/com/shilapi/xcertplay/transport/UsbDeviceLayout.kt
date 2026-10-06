package com.shilapi.xcertplay.transport

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.os.Build
import com.shilapi.xcertplay.compat.UsbCompat

/**
 * A platform-independent view of the device's active USB configuration.
 *
 * The typed UsbDevice.getConfiguration()/UsbConfiguration.getInterface() accessors are API 21, so
 * on Android 4.3 a project-owned view is built from UsbDevice.getInterface() — API 12 — and the
 * configuration id and per-interface alternate setting are recovered from rawDescriptors, which the
 * framework exposes on every release. Callers therefore work against one shape on both old and new
 * units instead of silently receiving null on 4.3.
 */
data class UsbDeviceLayout(
    val configurationId: Int,
    val interfaces: List<UsbInterfaceView>,
) {
    fun firstInterfaceMatching(predicate: (UsbInterfaceView) -> Boolean): UsbInterfaceView? =
        interfaces.firstOrNull(predicate)

    fun describe(): String =
        "config=$configurationId interfaces=" +
            interfaces.joinToString(",") { "${it.id}/${it.alternateSetting}" }
}

/** One interface plus the alternate setting currently selected for it. */
data class UsbInterfaceView(
    val platform: android.hardware.usb.UsbInterface,
    val alternateSetting: Int,
) {
    val id: Int get() = platform.id
    val interfaceClass: Int get() = platform.interfaceClass
    val interfaceSubclass: Int get() = platform.interfaceSubclass
    val interfaceProtocol: Int get() = platform.interfaceProtocol
    val endpointCount: Int get() = platform.endpointCount
    fun endpoint(index: Int) = platform.getEndpoint(index)
}

object UsbDeviceLayoutReader {

    /**
     * UsbDeviceConnection.getRawDescriptors() has existed since API 13, so it is reachable on 4.3;
     * it is read reflectively only to keep the call site off the typed accessor list.
     */
    fun rawDescriptors(connection: UsbDeviceConnection): ByteArray = runCatching {
        UsbDeviceConnection::class.java.getMethod("getRawDescriptors").invoke(connection) as? ByteArray
    }.getOrNull() ?: ByteArray(0)

    /**
     * Reads the active configuration from an already-open connection.
     *
     * rawDescriptors lives on UsbDeviceConnection (API 13), not on UsbDevice, so the descriptors
     * are only reachable once a connection exists. Never falls back to configuration id 0:
     * SET_CONFIGURATION with wValue 0 de-configures the device.
     */
    fun read(device: UsbDevice, connection: UsbDeviceConnection): UsbDeviceLayout {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val configurations = (0 until UsbCompat.configurationCount(device))
                .mapNotNull { UsbCompat.configuration(device, it) }
            // Preserve the original selection: prefer a configuration carrying both the Apple
            // USB Multiplex and the CDC-NCM control interface rather than blindly taking index 0.
            val preferred = configurations.firstOrNull { candidate ->
                IphoneCarPlayConfiguration.hasUsbMux(candidate) &&
                    IphoneCarPlayConfiguration.hasCdcNcm(candidate)
            } ?: configurations.firstOrNull { IphoneCarPlayConfiguration.hasUsbMux(it) }
                ?: configurations.firstOrNull()
            if (preferred != null) {
                val interfaces = (0 until UsbCompat.interfaceCount(preferred))
                    .mapNotNull { UsbCompat.usbInterface(preferred, it) }
                    .map { UsbInterfaceView(it, UsbCompat.alternateSetting(it)) }
                if (interfaces.isNotEmpty()) {
                    return UsbDeviceLayout(UsbCompat.configurationId(preferred), interfaces)
                }
            }
        }
        return fromDescriptors(device, connection)
    }

    /**
     * Builds the view from UsbDevice.getInterface() (API 12) plus the connection's rawDescriptors.
     * Used on API 18-20 where the typed configuration accessors do not exist.
     */
    fun fromDescriptors(device: UsbDevice, connection: UsbDeviceConnection): UsbDeviceLayout {
        val descriptors = rawDescriptors(connection)
        val active = activeConfigurationId(connection)
        return fromDescriptors(device, descriptors, active)
    }

    /**
     * Builds the view from an explicit descriptor blob and the id the device reports as active.
     * Separated from [read] so tests can drive it with a constructed UsbDevice on any API level.
     */
    fun fromDescriptors(
        device: UsbDevice,
        descriptors: ByteArray,
        selectedConfigurationId: Int?,
    ): UsbDeviceLayout {
        return H52UsbConfigurationFix.read(device, descriptors, selectedConfigurationId)
    }

    /**
     * Standard GET_CONFIGURATION: bmRequestType 0x80 (in, device, standard), bRequest 0x08.
     * Returns null when the device does not answer, so callers can refuse rather than send 0.
     */
    fun activeConfigurationId(connection: UsbDeviceConnection): Int? {
        val buffer = ByteArray(1)
        val read = connection.controlTransfer(0x80, 0x08, 0, 0, buffer, buffer.size, GET_CONFIGURATION_TIMEOUT_MS)
        if (read != 1) return null
        return buffer[0].toInt() and 0xFF
    }

    private const val GET_CONFIGURATION_TIMEOUT_MS = 2_000
}
