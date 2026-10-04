package com.shilapi.xcertplay.orchestration

import com.shilapi.xcertplay.transport.Iap2IdentificationConfig
import com.shilapi.xcertplay.transport.UsbDeviceId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class MfiTargetConfigTest {
    @Test
    fun usbTargetRequiresACh341Device() {
        assertThrows(IllegalArgumentException::class.java) {
            config(mfiTarget = MfiTarget.USB_CH341)
        }
    }

    @Test
    fun i2cTargetUsesTheConfiguredDevicePath() {
        val config = config(
            mfiTarget = MfiTarget.I2C,
            linuxI2cPath = "/dev/i2c-7",
        )

        assertEquals("/dev/i2c-7", config.linuxI2cPath)
    }

    @Test
    fun i2cTargetRejectsABlankDevicePath() {
        assertThrows(IllegalArgumentException::class.java) {
            config(mfiTarget = MfiTarget.I2C, linuxI2cPath = "  ")
        }
    }

    @Test
    fun remoteTargetDoesNotRequireALocalDevice() {
        val config = config(
            mfiTarget = MfiTarget.REMOTE,
            remoteMfiServer = "https://mfi.example.test",
            remoteMfiToken = "secret",
        )

        assertEquals(MfiTarget.REMOTE, config.mfiTarget)
        assertEquals("https://mfi.example.test", config.remoteMfiServer)
        assertEquals("secret", config.remoteMfiToken)
    }

    @Test
    fun remoteTargetRequiresAServerAddress() {
        assertThrows(IllegalArgumentException::class.java) {
            config(mfiTarget = MfiTarget.REMOTE, remoteMfiServer = "  ")
        }
    }

    private fun config(
        mfiTarget: MfiTarget,
        ch341Devices: List<UsbDeviceId> = emptyList(),
        linuxI2cPath: String? = null,
        remoteMfiServer: String? = null,
        remoteMfiToken: String? = null,
    ): CarPlayRuntimeConfig = CarPlayRuntimeConfig(
        mfiTarget = mfiTarget,
        ch341Devices = ch341Devices,
        linuxI2cPath = linuxI2cPath,
        remoteMfiServer = remoteMfiServer,
        remoteMfiToken = remoteMfiToken,
        identification = Iap2IdentificationConfig(
            name = "test",
            modelIdentifier = "test",
            manufacturer = "test",
            serialNumber = "test",
            firmwareVersion = "1",
            hardwareVersion = "1",
            carPlayUsbInterfaceNumber = 3,
        ),
    )
}
