package com.shilapi.xcertplay.transport

import com.shilapi.xcertplay.compat.UsbCompat

import android.hardware.usb.UsbConfiguration
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface

/**
 * Finds the NCM control/data interface pair inside an active iPhone configuration.
 *
 * LIVI claims the control interface, claims the data interface, and selects data alternate
 * setting 1, the setting that carries the bulk endpoints. This class only reads descriptors.
 */
object NcmFunctionDiscovery {
    const val CONTROL_CLASS = 0x02
    const val CONTROL_SUBCLASS = 0x0d
    const val DATA_CLASS = 0x0a
    const val APPLE_ETHERNET_CLASS = 0xff
    const val APPLE_ETHERNET_SUBCLASS = 0xfd
    const val APPLE_ETHERNET_PROTOCOL = 0x01
    const val DATA_ALTERNATE_SETTING = 1

    data class NcmFunction(
        val control: UsbInterface,
        val data: UsbInterface,
        val statusIn: UsbEndpoint?,
        val bulkIn: UsbEndpoint,
        val bulkOut: UsbEndpoint,
        /**
         * Alternate setting the data interface must be switched to before bulk transfers. On API
         * 21+ this comes from the platform interface; on 4.3 it is recovered from the descriptor
         * layout, because UsbInterface.getAlternateSetting() does not exist there.
         */
        val dataAlternate: Int = DATA_ALTERNATE_SETTING,
    ) {
        val controlAlternate: Int get() = UsbCompat.alternateSetting(control)
    }

    fun find(configuration: UsbConfiguration): NcmFunction? = findCdcNcm(configuration, null)

    /** [layout] supplies the descriptor-derived alternate settings on API 18-20. */
    fun find(configuration: UsbConfiguration, layout: UsbDeviceLayout?): NcmFunction? =
        findCdcNcm(configuration, layout)

    /**
     * Discovers the CDC-NCM function from the descriptor-derived layout alone. This is the path
     * Android 4.3 takes, where UsbConfiguration does not exist: the control and data interfaces are
     * matched by class/subclass and the data alternate by the descriptor record.
     */
    fun find(layout: UsbDeviceLayout): NcmFunction? {
        val control = layout.firstInterfaceMatching {
            it.interfaceClass == CONTROL_CLASS && it.interfaceSubclass == CONTROL_SUBCLASS
        } ?: return null
        val data = layout.interfaces
            .filter { it.interfaceClass == DATA_CLASS && bulkEndpoints(it.platform) != null }
            .minByOrNull { if (it.alternateSetting == DATA_ALTERNATE_SETTING) 0 else 1 }
            ?: return null
        val endpoints = bulkEndpoints(data.platform) ?: return null
        val statusIn = (0 until control.platform.endpointCount)
            .map(control.platform::getEndpoint)
            .singleOrNull {
                it.direction == UsbConstants.USB_DIR_IN &&
                    it.type == UsbConstants.USB_ENDPOINT_XFER_INT
            }
        return NcmFunction(
            control = control.platform,
            data = data.platform,
            statusIn = statusIn,
            bulkIn = endpoints.first,
            bulkOut = endpoints.second,
            dataAlternate = data.alternateSetting,
        )
    }

    private fun findCdcNcm(configuration: UsbConfiguration, layout: UsbDeviceLayout?): NcmFunction? {
        val control = interfaces(configuration).firstOrNull {
            it.interfaceClass == CONTROL_CLASS && it.interfaceSubclass == CONTROL_SUBCLASS
        } ?: return null
        val data = interfaces(configuration)
            .filter { it.interfaceClass == DATA_CLASS && bulkEndpoints(it) != null }
            .minByOrNull { if (UsbCompat.alternateSetting(it) == DATA_ALTERNATE_SETTING) 0 else 1 }
            ?: return null
        val endpoints = bulkEndpoints(data) ?: return null
        val statusIn = (0 until control.endpointCount)
            .map(control::getEndpoint)
            .singleOrNull {
                it.direction == UsbConstants.USB_DIR_IN &&
                    it.type == UsbConstants.USB_ENDPOINT_XFER_INT
            }
        val dataAlternate = layout?.interfaces
            ?.firstOrNull { it.platform === data }
            ?.alternateSetting
            ?: UsbCompat.alternateSetting(data)
        return NcmFunction(control, data, statusIn, endpoints.first, endpoints.second, dataAlternate)
    }

    private fun interfaces(configuration: UsbConfiguration): List<UsbInterface> =
        (0 until UsbCompat.interfaceCount(configuration))
        .mapNotNull { UsbCompat.usbInterface(configuration, it) }

    private fun bulkEndpoints(usbInterface: UsbInterface): Pair<UsbEndpoint, UsbEndpoint>? {
        val endpoints = (0 until usbInterface.endpointCount).map(usbInterface::getEndpoint)
        val input = endpoints.singleOrNull {
            it.direction == UsbConstants.USB_DIR_IN && it.type == UsbConstants.USB_ENDPOINT_XFER_BULK
        }
        val output = endpoints.singleOrNull {
            it.direction == UsbConstants.USB_DIR_OUT && it.type == UsbConstants.USB_ENDPOINT_XFER_BULK
        }
        return if (input != null && output != null) input to output else null
    }
}
