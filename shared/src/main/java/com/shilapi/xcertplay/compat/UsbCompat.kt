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

    /**
     * Selects [configuration] on the device. The typed setConfiguration() is API 21, so on Android
     * 4.3 this issues the standard SET_CONFIGURATION request over controlTransfer(), which has
     * existed since API 12. Returning false here would silently disable the wired CarPlay path, so
     * the legacy branch performs the real transfer instead of bailing out.
     */
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

    /** Standard USB SET_CONFIGURATION: bmRequestType 0x00, bRequest 0x09, wValue = configuration id. */
    private fun setConfigurationLegacy(connection: UsbDeviceConnection, configurationId: Int): Boolean {
        val value = configurationId and 0xFF
        val result = connection.controlTransfer(0x00, 0x09, value, 0, null, 0, SET_CONFIGURATION_TIMEOUT_MS)
        if (result < 0) {
            Log.w(TAG, "SET_CONFIGURATION $configurationId failed code=$result")
            return false
        }
        return true
    }

    /**
     * Selects an alternate setting on an already-claimed interface. setInterface() is API 21; the
     * legacy branch sends SET_INTERFACE (0x01 0x0B) with wIndex encoding interface and alternate.
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
        // SET_INTERFACE: bmRequestType=0x01, bRequest=0x0B, wValue=alternateSetting,
        // wIndex=interface id. Putting the alternate in wIndex instead selects alt 0.
        val result = connection.controlTransfer(
            0x01, 0x0B,
            alternateSetting and 0xFF,
            usbInterface.id and 0xFF,
            null, 0, SET_CONFIGURATION_TIMEOUT_MS,
        )
        if (result < 0) {
            Log.w(TAG, "SET_INTERFACE ${usbInterface.id}/$alternateSetting failed code=$result")
            return false
        }
        return true
    }

    private const val TAG = "xcertplay-usb"
    private const val SET_CONFIGURATION_TIMEOUT_MS = 2_000

    private inline fun <reified T> invoke(target: Any, name: String, arg: Int): T? = runCatching {
        @Suppress("UNCHECKED_CAST")
        target.javaClass.getMethod(name, Integer.TYPE).invoke(target, arg) as? T
    }.getOrNull()

    private fun invokeInt(target: Any, name: String, fallback: Int): Int =
        runCatching { target.javaClass.getMethod(name).invoke(target) as Int }.getOrDefault(fallback)
}
