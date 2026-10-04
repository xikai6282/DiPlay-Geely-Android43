package com.shilapi.xcertplay.transport

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Fixtures mirror the descriptor shapes an iPhone NCM CarPlay configuration actually produces:
 * a device descriptor, more than one configuration, interface 0 with an endpoint, interface 2 with
 * two alternate settings, and the 5- and 7-byte descriptors that sit between them.
 */
class ConfigurationDescriptorScannerTest {

    private fun deviceDescriptor(length: Int = 18) = ByteArray(length).also {
        it[0] = length.toByte(); it[1] = 0x01
        it[2] = 0x00; it[3] = 0x02
    }

    private fun configDescriptor(configurationId: Int, totalLength: Int, interfaceCount: Int) =
        byteArrayOf(
            9, 0x02, (totalLength and 0xFF).toByte(), (totalLength shr 8).toByte(),
            interfaceCount.toByte(), configurationId.toByte(), 0, 0x80.toByte(), 250.toByte(),
        )

    /** Interface descriptor: [0]=len [1]=type [2]=bInterfaceNumber [3]=bAlternateSetting. */
    private fun interfaceDescriptor(number: Int, alternate: Int, ifaceClass: Int, subclass: Int = 0, protocol: Int = 0) =
        byteArrayOf(
            9, 0x04, number.toByte(), alternate.toByte(), 0,
            ifaceClass.toByte(), subclass.toByte(), protocol.toByte(), 0,
        )

    /** Endpoint descriptor: 7 bytes, class 0xff/0xfe/0x03/0x01/0x01 and 8 endpoints. */
    private fun endpointDescriptor(address: Int, attributes: Int) =
        byteArrayOf(
            7, 0x05, address.toByte(), attributes.toByte(),
            0x00, 0x00, 0x00,
        )

    /** Class-specific descriptor: 5 bytes, as used by CDC header/functional descriptors. */
    private fun classSpecificDescriptor(subtype: Int, length: Int = 5) =
        byteArrayOf(length.toByte(), 0x24, subtype.toByte(), 0x00, 0x00)

    private fun concat(vararg parts: ByteArray): ByteArray =
        parts.fold(ByteArray(0)) { acc, part -> acc + part }

    @Test fun readsConfigurationIdAndAlternateSettingsKeyedByInterfaceNumber() {
        val descriptors = concat(
            deviceDescriptor(),
            configDescriptor(4, 9 + 9 + 7 + 9, 2),
            interfaceDescriptor(0, 0, 0xff, 0xfe, 0x02),
            endpointDescriptor(0x04, 0x02),
            interfaceDescriptor(2, 0, 0x02, 0x0d),
        )
        assertEquals(4, ConfigurationDescriptorScanner.configurationId(descriptors))
        // Keyed by bInterfaceNumber, not by descriptor ordinal: the first interface here is 0 and
        // the second is 2, because interface 1 does not exist on this configuration.
        assertEquals(
            mapOf(0 to 0, 2 to 0),
            ConfigurationDescriptorScanner.alternateSettingsByIndex(descriptors),
        )
    }

    @Test fun scanContinuesPastShortEndpointAndClassSpecificDescriptors() {
        val descriptors = concat(
            deviceDescriptor(),
            configDescriptor(2, 9 + 9 + 7 + 5 + 9, 2),
            interfaceDescriptor(0, 0, 0xff, 0xfe, 0x02),
            endpointDescriptor(0x04, 0x02),
            classSpecificDescriptor(0x00),
            interfaceDescriptor(2, 0, 0x02, 0x0d),
        )
        // A walker that stopped at the 7-byte endpoint or the 5-byte class-specific descriptor would
        // never reach interface 2.
        assertEquals(
            mapOf(0 to 0, 2 to 0),
            ConfigurationDescriptorScanner.alternateSettingsByIndex(descriptors),
        )
    }

    @Test fun keepsTheHighestAlternateSettingForAnInterface() {
        val descriptors = concat(
            deviceDescriptor(),
            configDescriptor(4, 9 + 9 + 7 + 9 + 9 + 7, 2),
            interfaceDescriptor(0, 0, 0xff, 0xfe, 0x02),
            endpointDescriptor(0x04, 0x02),
            interfaceDescriptor(2, 0, 0x02, 0x0d),
            interfaceDescriptor(2, 1, 0x02, 0x0d),
            endpointDescriptor(0x85, 0x02),
        )
        // alt 1 is the selected alternate and must win over alt 0 for interface 2.
        assertEquals(
            mapOf(0 to 0, 2 to 1),
            ConfigurationDescriptorScanner.alternateSettingsByIndex(descriptors),
        )
    }

    @Test fun selectsTheConfigurationWithTheMostInterfaces() {
        // Passive configuration 1 with one interface, then configuration 4 with two, as the device
        // orders them; choosing the first would de-select the CarPlay configuration.
        val descriptors = concat(
            deviceDescriptor(),
            configDescriptor(1, 9 + 9, 1),
            interfaceDescriptor(0, 0, 0xff, 0x00, 0x00),
            configDescriptor(4, 9 + 9 + 9, 2),
            interfaceDescriptor(0, 0, 0xff, 0xfe, 0x02),
            interfaceDescriptor(2, 0, 0x02, 0x0d),
        )
        assertEquals(4, ConfigurationDescriptorScanner.configurationId(descriptors))
        assertEquals(
            mapOf(0 to 0, 2 to 0),
            ConfigurationDescriptorScanner.alternateSettingsByIndex(descriptors),
        )
    }

    @Test fun emptyAndTruncatedDescriptorsDoNotThrow() {
        assertEquals(0, ConfigurationDescriptorScanner.configurationId(ByteArray(0)))
        assertTrue(ConfigurationDescriptorScanner.alternateSettingsByIndex(ByteArray(0)).isEmpty())

        val truncated = concat(
            deviceDescriptor(),
            configDescriptor(6, 40, 2),
            interfaceDescriptor(0, 0, 0xff),
        ).copyOf(20)
        ConfigurationDescriptorScanner.configurationId(truncated)
        ConfigurationDescriptorScanner.alternateSettingsByIndex(truncated)
    }

    // ---- interfaceRecords: keeps every alternate and its endpoints, scoped to one configuration ----

    @Test fun interfaceRecordsKeepEveryAlternateWithItsEndpoints() {
        val descriptors = concat(
            deviceDescriptor(),
            configDescriptor(4, 9 + 9 + 7 + 9 + 7 + 7, 2),
            interfaceDescriptor(2, 0, 0x02, 0x0d),
            endpointDescriptor(0x83, 0x02),
            interfaceDescriptor(2, 1, 0x02, 0x0d),
            endpointDescriptor(0x01, 0x02),
            endpointDescriptor(0x81, 0x02),
        )
        val records = ConfigurationDescriptorScanner.interfaceRecords(descriptors, 4)
        assertEquals(1, records.size)
        val record = records.single()
        assertEquals(2, record.interfaceNumber)
        // alt 0 must survive alongside alt 1; collapsing to "last seen" would lose the data-alts.
        assertEquals(listOf(0, 1), record.alternateSettings)
        assertEquals(listOf(0x83), record.endpointAddressesByAlternate[0])
        assertEquals(listOf(0x01, 0x81), record.endpointAddressesByAlternate[1])
        assertEquals(1, record.activeAlternate)
    }

    @Test fun interfaceRecordsAreScopedToTheSelectedConfiguration() {
        val descriptors = concat(
            deviceDescriptor(),
            configDescriptor(1, 9 + 9, 1),
            interfaceDescriptor(0, 0, 0xff, 0x00, 0x00),
            configDescriptor(4, 9 + 9 + 9, 2),
            interfaceDescriptor(0, 0, 0xff, 0xfe, 0x02),
            interfaceDescriptor(2, 0, 0x02, 0x0d),
        )
        assertEquals(
            listOf(0, 2),
            ConfigurationDescriptorScanner.interfaceRecords(descriptors, 4).map { it.interfaceNumber },
        )
        assertEquals(
            listOf(0),
            ConfigurationDescriptorScanner.interfaceRecords(descriptors, 1).map { it.interfaceNumber },
        )
    }

    @Test fun configurationZeroIsRejectedRatherThanSilentlyReselected() {
        val descriptors = concat(
            deviceDescriptor(),
            configDescriptor(4, 9 + 9, 1),
            interfaceDescriptor(0, 0, 0xff, 0xfe, 0x02),
        )
        // wValue 0 de-configures the device, so it must never be treated as a selection; silently
        // falling back to another configuration would claim the wrong one.
        try {
            ConfigurationDescriptorScanner.interfaceRecords(descriptors, 0)
            throw AssertionError("expected configuration id 0 to be rejected")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message!!.contains("0"))
        }
        // No explicit selection still works.
        assertEquals(4, ConfigurationDescriptorScanner.configurationId(descriptors))
        assertEquals(
            listOf(0),
            ConfigurationDescriptorScanner.interfaceRecords(descriptors, null).map { it.interfaceNumber },
        )
    }

    @Test fun anAbsentSelectedConfigurationIsRejected() {
        val descriptors = concat(
            deviceDescriptor(),
            configDescriptor(4, 9 + 9, 1),
            interfaceDescriptor(0, 0, 0xff, 0xfe, 0x02),
        )
        try {
            ConfigurationDescriptorScanner.interfaceRecords(descriptors, 99)
            throw AssertionError("expected a missing configuration id to be rejected")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message!!.contains("99"))
        }
    }

    @Test fun endpointDescriptorsDoNotLeakAcrossInterfaces() {
        val descriptors = concat(
            deviceDescriptor(),
            configDescriptor(4, 9 + 9 + 7 + 9 + 7, 2),
            interfaceDescriptor(0, 0, 0xff, 0xfe, 0x02),
            endpointDescriptor(0x04, 0x02),
            interfaceDescriptor(2, 0, 0x02, 0x0d),
            endpointDescriptor(0x81, 0x02),
        )
        val byNumber = ConfigurationDescriptorScanner.interfaceRecords(descriptors, 4).associateBy { it.interfaceNumber }
        assertEquals(listOf(0x04), byNumber.getValue(0).endpointAddressesByAlternate[0])
        assertEquals(listOf(0x81), byNumber.getValue(2).endpointAddressesByAlternate[0])
    }
}
