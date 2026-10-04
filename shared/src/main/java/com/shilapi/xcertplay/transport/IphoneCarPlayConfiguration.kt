package com.shilapi.xcertplay.transport

import android.hardware.usb.UsbConfiguration
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.os.Build
import com.shilapi.xcertplay.compat.UsbCompat
import android.util.Log

/**
 * Descriptor-based discovery of the iPhone's CarPlay configuration.
 *
 * Configuration ids differ between iPhone models, so the configuration is identified by its
 * interfaces: Apple USB Multiplexor (USBMUX) plus the NCM/Ethernet function CarPlay uses.
 */
object IphoneCarPlayConfiguration {
    const val TAG = "xcertplay-usb"

    private const val USBMUX_CLASS = 0xff
    private const val USBMUX_SUBCLASS = 0xfe
    private const val USBMUX_PROTOCOL = 0x02
    private const val APPLE_ETHERNET_CLASS = 0xff
    private const val APPLE_ETHERNET_SUBCLASS = 0xfd
    private const val APPLE_ETHERNET_PROTOCOL = 0x01
    private const val NCM_CONTROL_CLASS = 0x02
    private const val NCM_CONTROL_SUBCLASS = 0x0d
    private const val PREFERRED_USBMUX_OUT = 0x04
    private const val PREFERRED_USBMUX_IN = 0x85

    fun find(device: UsbDevice): UsbConfiguration? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return null
        val configurations = (0 until UsbCompat.configurationCount(device))
            .mapNotNull { UsbCompat.configuration(device, it) }
        val chosen = configurations.firstOrNull { usbMuxInterface(it) != null && hasCdcNcm(it) && hasAppleEthernet(it) }
            ?: configurations.firstOrNull { usbMuxInterface(it) != null && hasCdcNcm(it) }
        Log.i(
            TAG,
            "carplay config chosen=${chosen?.let(UsbCompat::configurationId)} " +
                "available=${configurations.map(UsbCompat::configurationId)} detail=${chosen?.let(::describe)}",
        )
        return chosen
    }

    fun describe(configuration: UsbConfiguration): String =
        (0 until UsbCompat.interfaceCount(configuration)).joinToString(",") { index ->
            val usbInterface = UsbCompat.usbInterface(configuration, index) ?: return@joinToString "?"
            "${usbInterface.id}/${UsbCompat.alternateSetting(usbInterface)}" +
                ":${usbInterface.interfaceClass.toString(16)}" +
                ".${usbInterface.interfaceSubclass.toString(16)}" +
                ".${usbInterface.interfaceProtocol.toString(16)}" +
                "x${usbInterface.endpointCount}"
        }

    fun usbMuxInterface(configuration: UsbConfiguration): UsbInterface? =
        (0 until UsbCompat.interfaceCount(configuration)).mapNotNull { UsbCompat.usbInterface(configuration, it) }.firstOrNull {
            it.interfaceClass == USBMUX_CLASS &&
                it.interfaceSubclass == USBMUX_SUBCLASS &&
                it.interfaceProtocol == USBMUX_PROTOCOL
        }

    fun usbMuxInterface(layout: UsbDeviceLayout): UsbInterfaceView? =
        layout.firstInterfaceMatching {
            it.interfaceClass == USBMUX_CLASS &&
                it.interfaceSubclass == USBMUX_SUBCLASS &&
                it.interfaceProtocol == USBMUX_PROTOCOL
        }

    fun hasUsbMux(configuration: UsbConfiguration): Boolean = usbMuxInterface(configuration) != null

    fun usbMuxEndpoints(usbInterface: UsbInterface): Pair<UsbEndpoint, UsbEndpoint>? {
        val endpoints = (0 until usbInterface.endpointCount).map(usbInterface::getEndpoint)
        val out = endpoints.firstOrNull {
            it.address == PREFERRED_USBMUX_OUT &&
                it.direction == UsbConstants.USB_DIR_OUT &&
                it.type == UsbConstants.USB_ENDPOINT_XFER_BULK
        } ?: endpoints.singleOrNull {
            it.direction == UsbConstants.USB_DIR_OUT &&
                it.type == UsbConstants.USB_ENDPOINT_XFER_BULK
        }
        val input = endpoints.firstOrNull {
            it.address == PREFERRED_USBMUX_IN &&
                it.direction == UsbConstants.USB_DIR_IN &&
                it.type == UsbConstants.USB_ENDPOINT_XFER_BULK
        } ?: endpoints.singleOrNull {
            it.direction == UsbConstants.USB_DIR_IN &&
                it.type == UsbConstants.USB_ENDPOINT_XFER_BULK
        }
        return if (out != null && input != null) out to input else null
    }

    fun hasCdcNcm(configuration: UsbConfiguration): Boolean =
        (0 until UsbCompat.interfaceCount(configuration)).mapNotNull { UsbCompat.usbInterface(configuration, it) }.any {
            it.interfaceClass == NCM_CONTROL_CLASS && it.interfaceSubclass == NCM_CONTROL_SUBCLASS
        }

    private fun hasAppleEthernet(configuration: UsbConfiguration): Boolean =
        (0 until UsbCompat.interfaceCount(configuration)).mapNotNull { UsbCompat.usbInterface(configuration, it) }.any {
            it.interfaceClass == APPLE_ETHERNET_CLASS &&
                it.interfaceSubclass == APPLE_ETHERNET_SUBCLASS &&
                it.interfaceProtocol == APPLE_ETHERNET_PROTOCOL
        }

    /** Resolves the active configuration without API-21-only UsbConfiguration accessors. */
    fun readLayout(device: UsbDevice, connection: UsbDeviceConnection): UsbDeviceLayout =
        UsbDeviceLayoutReader.read(device, connection)

    fun hasCarPlayFunction(layout: UsbDeviceLayout): Boolean =
        usbMuxInterface(layout) != null && NcmFunctionDiscovery.find(layout) != null
}
