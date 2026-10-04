package com.shilapi.xcertplay.compat

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.os.Parcelable
import com.shilapi.xcertplay.transport.UsbDeviceLayoutReader
import com.shilapi.xcertplay.transport.NcmFunctionDiscovery

/** Real API18 platform USB objects populated with multiple configurations and NCM alternates. */
internal object LegacyUsbProbe {
    fun run(): String {
        val muxOut = endpoint(0x04, 2)
        val muxIn = endpoint(0x85, 2)
        val status = endpoint(0x83, 3)
        val bulkOut = endpoint(0x06, 2)
        val bulkIn = endpoint(0x87, 2)
        val mux = usbInterface(0, 0xff, 0xfe, 2, arrayOf(muxOut, muxIn))
        val control = usbInterface(1, 2, 0x0d, 0, arrayOf(status))
        val idleData = usbInterface(2, 0x0a, 0, 0, emptyArray())
        val activeData = usbInterface(2, 0x0a, 0, 0, arrayOf(bulkOut, bulkIn))
        val device = device(arrayOf(mux, control, idleData, activeData))
        val deviceHeader = ByteArray(18).apply { this[0] = 18; this[1] = 1; this[17] = 2 }
        val first = configuration(2, 1, iface(0, 3, 2, 0xff, 0xfe, 2) + ep(4, 2) + ep(0x85, 2))
        val second = configuration(4, 3,
            iface(0, 0, 2, 0xff, 0xfe, 2) + ep(4, 2) + ep(0x85, 2) +
                iface(1, 0, 1, 2, 0x0d, 0) + byteArrayOf(5, 0x24, 0, 0x10, 1) + ep(0x83, 3) +
                iface(2, 0, 0, 0x0a, 0, 0) + iface(2, 1, 2, 0x0a, 0, 0) + ep(6, 2) + ep(0x87, 2))
        val descriptors = deviceHeader + first + second
        val layout = UsbDeviceLayoutReader.fromDescriptors(device, descriptors, 4)
        check(layout.configurationId == 4)
        check(layout.interfaces.size == 4)
        val data = layout.interfaces.single { it.id == 2 && it.endpointCount == 2 }
        check(data.alternateSetting == 1) { "NCM data alternate was ${data.alternateSetting}" }
        val idle = layout.interfaces.single { it.id == 2 && it.endpointCount == 0 }
        check(idle.alternateSetting == 0) { "NCM alt0 incorrectly mapped to ${idle.alternateSetting}" }
        val ncm = checkNotNull(NcmFunctionDiscovery.find(layout)) { "Production NCM discovery rejected legacy layout" }
        check(ncm.control.id == 1 && ncm.data.id == 2 && ncm.dataAlternate == 1)
        check(ncm.bulkIn.address == 0x87 && ncm.bulkOut.address == 0x06 && ncm.statusIn?.address == 0x83)
        val firstLayout = UsbDeviceLayoutReader.fromDescriptors(device(arrayOf(mux)), descriptors, 2)
        check(firstLayout.configurationId == 2)
        check(firstLayout.interfaces.single().alternateSetting == 3) { "Selected config2 used another configuration's alternate" }
        var refusedZero = false
        try { UsbDeviceLayoutReader.fromDescriptors(device, descriptors, 0) }
        catch (_: IllegalArgumentException) { refusedZero = true }
        check(refusedZero) { "Unconfigured USB id0 must not silently become an active layout" }
        var refusedMissing = false
        try { UsbDeviceLayoutReader.fromDescriptors(device, descriptors, 99) }
        catch (_: IllegalArgumentException) { refusedMissing = true }
        check(refusedMissing) { "Selected config99 does not exist and must not fall back to another configuration" }
        return "configurations=2 ncmAlternates=0/1 selectedConfiguration=true zeroConfigurationRefused=true"
    }

    private fun endpoint(address: Int, attributes: Int): UsbEndpoint =
        UsbEndpoint::class.java.getConstructor(Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE)
            .newInstance(address, attributes, 512, 1)

    private fun usbInterface(id: Int, cls: Int, sub: Int, proto: Int, endpoints: Array<Parcelable>): UsbInterface =
        UsbInterface::class.java.getConstructor(Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE,
            emptyArray<Parcelable>().javaClass).newInstance(id, cls, sub, proto, endpoints)

    private fun device(interfaces: Array<Parcelable>): UsbDevice =
        UsbDevice::class.java.getConstructor(String::class.java, Integer.TYPE, Integer.TYPE, Integer.TYPE,
            Integer.TYPE, Integer.TYPE, emptyArray<Parcelable>().javaClass)
            .newInstance("/dev/bus/usb/999/001", 0x05ac, 0x12a8, 0, 0, 0, interfaces)

    private fun iface(id: Int, alt: Int, endpoints: Int, cls: Int, sub: Int, proto: Int) =
        byteArrayOf(9, 4, id.toByte(), alt.toByte(), endpoints.toByte(), cls.toByte(), sub.toByte(), proto.toByte(), 0)
    private fun ep(address: Int, attributes: Int) = byteArrayOf(7, 5, address.toByte(), attributes.toByte(), 0, 2, 1)
    private fun configuration(id: Int, interfaceCount: Int, body: ByteArray): ByteArray {
        val length = body.size + 9
        return byteArrayOf(9, 2, length.toByte(), (length shr 8).toByte(), interfaceCount.toByte(), id.toByte(), 0,
            0x80.toByte(), 50) + body
    }
}
