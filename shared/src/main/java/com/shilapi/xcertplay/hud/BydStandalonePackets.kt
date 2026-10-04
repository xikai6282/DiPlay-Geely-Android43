package com.shilapi.xcertplay.hud

/** Navigation records traced from the installed IVI HAL; no arbitrary feature input. */
object BydStandalonePackets {
    const val MAX_DISTANCE_METERS = 16777214
    fun start(): String = "43,E0,00,3A,01,02"
    fun clear(): String = "43,E0,00,3A,01,01"
    fun guidance(icon: Int, exit: Int, distanceMeters: Int, road: String? = null): String? {
        val turn = BydFactoryTurnCode.map(icon, exit) ?: return null
        if (distanceMeters !in 0..MAX_DISTANCE_METERS) return null
        return (record(0x43F01018, distanceMeters) + record(0x43F01010, turn) +
            record(0x43F01030, turn)).toHex() + (road?.let { "," + streetName(it) } ?: "")
    }
    /** Exact non-RCS encodeReqSplitString format from the inspected IVI HAL.
     * The HAL caps this feature at 96 bytes. Each record carries seven content bytes,
     * a sequence byte, and the original feature incremented by 0x1000.
     */
    fun streetName(road: String): String {
        val text = buildString {
            var index = 0
            while (index < road.length && length < 48) {
                val char = road[index++]
                when {
                    char.isHighSurrogate() -> {
                        if (index < road.length && road[index].isLowSurrogate()) {
                            if (length > 46) break
                            append(char).append(road[index++])
                        }
                    }
                    char.isLowSurrogate() -> Unit
                    char.isISOControl() || char.isWhitespace() -> append(' ')
                    else -> append(char)
                }
            }
        }.trim().ifEmpty { " " } // setString rejects zero bytes; a space replaces stale text.
        val bytes = text.toByteArray(Charsets.UTF_16LE)
        val contentSize = bytes.size + 2
        val content = ByteArray(((contentSize + 6) / 7) * 7) { 0xFF.toByte() }
        content[0] = bytes.size.toByte()
        bytes.copyInto(content, 1)
        content[bytes.size + 1] = bytes.sumOf { it.toInt() and 255 }.toByte()
        val packet = ByteArray(content.size / 7 * 13)
        for (index in 0 until content.size / 7) {
            val feature = 0x43FA1008 + (index shl 12)
            val offset = index * 13
            packet[offset] = (feature ushr 24).toByte()
            packet[offset + 1] = (feature ushr 16).toByte()
            packet[offset + 2] = (feature ushr 8).toByte()
            packet[offset + 3] = feature.toByte()
            packet[offset + 4] = 8
            packet[offset + 5] = (feature ushr 12).toByte()
            content.copyInto(packet, offset + 6, index * 7, (index + 1) * 7)
        }
        return packet.toHex()
    }
    private fun ByteArray.toHex(): String = joinToString(",") { "%02X".format(it.toInt() and 255) }

    private fun record(feature: Int, value: Int): ByteArray = byteArrayOf(
        (feature ushr 24).toByte(), (feature ushr 16).toByte(),
        (feature ushr 8).toByte(), feature.toByte(), 4,
        (value ushr 24).toByte(), (value ushr 16).toByte(),
        (value ushr 8).toByte(), value.toByte(),
    )
}
