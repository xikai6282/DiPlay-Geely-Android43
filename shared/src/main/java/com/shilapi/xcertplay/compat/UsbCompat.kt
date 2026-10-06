package com.shilapi.xcertplay.compat

import android.hardware.usb.UsbConfiguration
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbInterface
import android.os.Build
import android.util.Log

/**
 * The typed USB getters (getConfiguration/getInterface/getAlternateSetting/setConfiguration) are API
 * 21; Android 4.1-4.3 only has the int-index accessors. Both are read reflectively here so the
 * callers can stay written once.
 */
object UsbCompat {
    fun configurationCount(device: UsbDevice): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) device.configurationCount
        else invokeInt(device, "getConfigurationCount", fallback = 1)

    fun configuration(device: UsbDevice, index: Int): UsbConfiguration? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) device.getConfiguration(index)
        else invoke(device, "getConfiguration", index)

    fun configurationId(configuration: UsbConfiguration): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) configuration.id
        else invokeInt(configuration, "getId", fallback = 0)

    fun interfaceCount(configuration: UsbConfiguration): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) configuration.interfaceCount
        else invokeInt(configuration, "getInterfaceCount", fallback = 0)

    fun usbInterface(configuration: UsbConfiguration, index: Int): UsbInterface? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) configuration.getInterface(index)
        else invoke(configuration, "getInterface", index)

    fun alternateSetting(usbInterface: UsbInterface): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) usbInterface.alternateSetting
        else invokeInt(usbInterface, "getAlternateSetting", fallback = 0)

    /** Select configuration through usbfs so device and kernel state change together. */
    fun setConfiguration(
        connection: UsbDeviceConnection,
        configuration: UsbConfiguration,
    ): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            runCatching { connection.setConfiguration(configuration) }.getOrDefault(false)
        } else {
            setConfigurationLegacy(connection, configurationId(configuration))
        }

    /**
     * Selects a configuration by id, which is the only form available once the layout has been
     * resolved from descriptors rather than from a platform UsbConfiguration.
     */
    fun setConfigurationById(connection: UsbDeviceConnection, configurationId: Int): Boolean =
        setConfigurationLegacy(connection, configurationId)

    private fun setConfigurationLegacy(connection: UsbDeviceConnection, configurationId: Int): Boolean {
        val result = runCatching { LegacyUsbNative.configure(connection.fileDescriptor, configurationId) }
            .getOrElse { Log.w(TAG, "USBDEVFS_SETCONFIGURATION helper unavailable", it); return false }
        Log.i(TAG, "USBDEVFS_SETCONFIGURATION config=$configurationId result=$result errno=${-result}")
        return result == 0
    }

    /** Preserve Android's force-claim behavior, then capture the real kernel error on failure. */
    fun claimInterface(connection: UsbDeviceConnection, usbInterface: UsbInterface, diagnostic: (String) -> Unit = {}): Boolean {
        if (connection.claimInterface(usbInterface, true)) return true
        val result = runCatching { LegacyUsbNative.claim(connection.fileDescriptor, usbInterface.id) }
            .getOrElse { Log.w(TAG, "USBDEVFS_CLAIMINTERFACE helper unavailable", it); return false }
        val detail = "USBDEVFS_CLAIMINTERFACE iface=${usbInterface.id} result=$result errno=${-result}"
        Log.w(TAG, detail)
        diagnostic(detail)
        return result == 0
    }

    /**
     * Selects an alternate setting on an already-claimed interface. setInterface() is API 21; the
     * legacy branch uses USBDEVFS_SETINTERFACE to update the kernel endpoint state too.
     */
    fun setInterface(connection: UsbDeviceConnection, usbInterface: UsbInterface): Boolean =
        setInterface(connection, usbInterface, alternateSetting(usbInterface))

    /**
     * Selects [alternateSetting] on an already-claimed interface. On API 18-20 the typed
     * setInterface() does not exist and the platform object carries no alternate, so the value
     * resolved from raw descriptors must be passed in explicitly.
     */
    fun setInterface(
        connection: UsbDeviceConnection,
        usbInterface: UsbInterface,
        alternateSetting: Int,
    ): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && alternateSetting == alternateSetting(usbInterface)) {
            return runCatching { connection.setInterface(usbInterface) }.getOrDefault(false)
        }
        val result = runCatching {
            LegacyUsbNative.selectAlternate(connection.fileDescriptor, usbInterface.id, alternateSetting)
        }.getOrElse { Log.w(TAG, "USBDEVFS_SETINTERFACE helper unavailable", it); return false }
        Log.i(TAG, "USBDEVFS_SETINTERFACE iface=${usbInterface.id}/$alternateSetting result=$result errno=${-result}")
        return result == 0
    }

    private const val TAG = "xcertplay-usb"

    private inline fun <reified T> invoke(target: Any, name: String, arg: Int): T? = runCatching {
        @Suppress("UNCHECKED_CAST")
        target.javaClass.getMethod(name, Integer.TYPE).invoke(target, arg) as? T
    }.getOrNull()

    private fun invokeInt(target: Any, name: String, fallback: Int): Int =
        runCatching { target.javaClass.getMethod(name).invoke(target) as Int }.getOrDefault(fallback)
}
