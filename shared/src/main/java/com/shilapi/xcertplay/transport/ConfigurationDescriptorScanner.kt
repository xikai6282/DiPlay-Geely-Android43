package com.shilapi.xcertplay.transport

/**
 * Minimal USB configuration-descriptor walker.
 *
 * Configuration id and per-interface alternate settings are needed on Android 4.3, where the typed
 * UsbConfiguration accessors do not exist. rawDescriptors is available on every release, so the two
 * values are recovered from the standard descriptor layout:
 *
 *   Configuration Descriptor: 9 bytes, bConfigurationValue at offset 5
 *   Interface   Descriptor: 9 bytes, bInterfaceNumber at 2, bAlternateSetting at 3
 *
 * bNumInterfaces (offset 4) bounds how many interface descriptors belong to the configuration, so
 * alternates are keyed by interface index rather than by descriptor order.
 */
object ConfigurationDescriptorScanner {

    private const val CONFIGURATION_DESCRIPTOR_TYPE = 0x02
    private const val INTERFACE_DESCRIPTOR_TYPE = 0x04
    private const val ENDPOINT_DESCRIPTOR_TYPE = 0x05
    private const val CONFIGURATION_HEADER_LENGTH = 9
    private const val INTERFACE_HEADER_LENGTH = 9
    private const val ENDPOINT_HEADER_LENGTH = 7

    /**
     * One interface descriptor inside the selected configuration: its number, every alternate
     * setting declared for it, and the endpoint addresses each alternate carries. Keeping all
     * alternatives — rather than collapsing to the last one seen — is what lets a caller match a
     * platform UsbInterface to the right (id, alternate, endpoints) triple.
     */
    data class InterfaceRecord(
        val interfaceNumber: Int,
        val alternateSettings: List<Int>,
        val endpointAddressesByAlternate: Map<Int, List<Int>>,
    ) {
        /** The highest alternate setting declared, which is the active one on most stacks. */
        val activeAlternate: Int get() = alternateSettings.maxOrNull() ?: 0
    }

    /** The active configuration id, or 0 when the descriptor set carries none. */
    fun configurationId(descriptors: ByteArray): Int {
        val configuration = findConfiguration(descriptors) ?: return 0
        return u8(descriptors, configuration + 5)
    }

    /**
     * Every interface descriptor belonging to the configuration selected by
     * [selectedConfigurationId], or the one the scanner picks when that is null or absent.
     */
    fun interfaceRecords(descriptors: ByteArray, selectedConfigurationId: Int? = null): List<InterfaceRecord> {
        val configuration = findConfiguration(descriptors, selectedConfigurationId) ?: return emptyList()
        val totalLength = u16(descriptors, configuration + 2)
        val end = minOf(descriptors.size, configuration + totalLength)

        val alternates = LinkedHashMap<Int, MutableList<Int>>()
        val endpoints = LinkedHashMap<Int, MutableMap<Int, MutableList<Int>>>()
        var currentInterface: Int? = null
        var currentAlternate: Int? = null

        var offset = configuration + CONFIGURATION_HEADER_LENGTH
        while (offset + 2 <= end) {
            val length = u8(descriptors, offset)
            val type = u8(descriptors, offset + 1)
            if (length < 2 || offset + length > end) break
            when {
                type == INTERFACE_DESCRIPTOR_TYPE && length >= INTERFACE_HEADER_LENGTH -> {
                    currentInterface = u8(descriptors, offset + 2)
                    currentAlternate = u8(descriptors, offset + 3)
                    alternates.getOrPut(currentInterface!!) { mutableListOf() }.add(currentAlternate!!)
                    // Map.putIfAbsent is a Java 8 default method (API 24) and does not exist on 4.3.
                    val forInterface = endpoints.getOrPut(currentInterface!!) { mutableMapOf() }
                    if (!forInterface.containsKey(currentAlternate)) {
                        forInterface[currentAlternate] = mutableListOf()
                    }
                }
                // Endpoint descriptors belong to the interface descriptor that precedes them.
                type == ENDPOINT_DESCRIPTOR_TYPE && length >= ENDPOINT_HEADER_LENGTH -> {
                    val number = currentInterface
                    val alternate = currentAlternate
                    if (number != null && alternate != null) {
                        val forInterface = endpoints.getOrPut(number) { mutableMapOf() }
                        val list = forInterface.getOrPut(alternate) { mutableListOf() }
                        list.add(u8(descriptors, offset + 2))
                    }
                }
            }
            offset += length
        }
        return alternates.map { (number, settings) ->
            InterfaceRecord(
                interfaceNumber = number,
                alternateSettings = settings.distinct(),
                endpointAddressesByAlternate = endpoints[number].orEmpty(),
            )
        }
    }

    /** Alternate setting currently selected for each interface index, keyed 0-based. */
    fun alternateSettingsByIndex(descriptors: ByteArray): Map<Int, Int> {
        val configuration = findConfiguration(descriptors) ?: return emptyMap()
        val totalLength = u16(descriptors, configuration + 2)
        val interfaceCount = u8(descriptors, configuration + 4)
        val end = minOf(descriptors.size, configuration + totalLength)

        // bNumInterfaces counts unique interfaces, not alternate-setting descriptors, so it cannot
        // bound the walk: interface 2 may contribute alt 0 and alt 1 entries. Walk every descriptor
        // in the configuration instead, tolerating the 5-byte class-specific and 7-byte endpoint
        // descriptors that sit between interface entries.
        val alternates = HashMap<Int, Int>()
        var offset = configuration + CONFIGURATION_HEADER_LENGTH
        while (offset + 2 <= end) {
            val length = u8(descriptors, offset)
            val type = u8(descriptors, offset + 1)
            if (length < 2 || offset + length > end) break
            if (type == INTERFACE_DESCRIPTOR_TYPE && length >= INTERFACE_HEADER_LENGTH) {
                // Key by the descriptor's own bInterfaceNumber: UsbDevice.getInterface(index) is
                // ordered by interface number, so index and bInterfaceNumber coincide, but reading
                // the descriptor keeps this independent of descriptor order.
                alternates[u8(descriptors, offset + 2)] = u8(descriptors, offset + 3)
            }
            offset += length
        }
        return alternates
    }

    /**
     * Offset of the configuration descriptor to use. Devices may expose more than one, so the one
     * carrying the most interfaces wins rather than simply the first; the active one is otherwise
     * indistinguishable from a passive one here.
     */
    fun findConfiguration(descriptors: ByteArray, selectedConfigurationId: Int? = null): Int? {
        // An explicitly reported id is authoritative: if it does not match any descriptor we must
        // fail rather than silently select a different configuration. 0 means "de-configured" and
        // is never a match either, so it also falls through to the scan.
        if (selectedConfigurationId != null) {
            // An explicit selection is authoritative. 0 means "de-configured" and any id that is not
            // present must fail loudly: silently selecting another configuration would claim the
            // wrong USB configuration and break the wired CarPlay path.
            require(selectedConfigurationId > 0) {
                "selectedConfigurationId must be a real configuration id, was $selectedConfigurationId"
            }
            return findConfigurationById(descriptors, selectedConfigurationId)
                ?: throw IllegalArgumentException(
                    "configuration $selectedConfigurationId is not present in the descriptor set",
                )
        }
        return findBestConfiguration(descriptors)
    }

    private fun findBestConfiguration(descriptors: ByteArray): Int? {
        var offset = 0
        var best: Int? = null
        var bestInterfaces = -1
        while (offset + 2 <= descriptors.size) {
            val length = u8(descriptors, offset)
            val type = u8(descriptors, offset + 1)
            if (length < 2 || offset + length > descriptors.size) break
            if (type == CONFIGURATION_DESCRIPTOR_TYPE && length >= CONFIGURATION_HEADER_LENGTH) {
                val interfaces = u8(descriptors, offset + 4)
                if (interfaces > bestInterfaces) {
                    bestInterfaces = interfaces
                    best = offset
                }
            }
            offset += length
        }
        return best
    }

    private fun findConfigurationById(descriptors: ByteArray, configurationId: Int): Int? {
        var offset = 0
        while (offset + 2 <= descriptors.size) {
            val length = u8(descriptors, offset)
            val type = u8(descriptors, offset + 1)
            if (length < 2 || offset + length > descriptors.size) break
            if (type == CONFIGURATION_DESCRIPTOR_TYPE &&
                length >= CONFIGURATION_HEADER_LENGTH &&
                u8(descriptors, offset + 5) == configurationId
            ) {
                return offset
            }
            offset += length
        }
        return null
    }

    private fun u8(bytes: ByteArray, offset: Int): Int =
        if (offset in bytes.indices) bytes[offset].toInt() and 0xFF else 0

    private fun u16(bytes: ByteArray, offset: Int): Int {
        if (offset + 1 !in bytes.indices) return 0
        return (u8(bytes, offset) or (u8(bytes, offset + 1) shl 8))
    }
}