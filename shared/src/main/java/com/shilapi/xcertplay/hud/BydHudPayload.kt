package com.shilapi.xcertplay.hud

import java.io.ByteArrayOutputStream

/** Wire shape emitted by BYD HUD's DirectTbtPayload; field order matters to the HUD firmware. */
internal object BydHudPayload {
    private const val MIN_DISTANCE_METERS = 11 // the glass garbles 0..10 m readouts
    private const val MAX_TEXT_BYTES = 200

    fun guidance(
        distanceMeters: Int,
        maneuver: Int,
        icon: ByteArray? = null,
        road: String = "",
        eta: String = "",
    ): ByteArray =
        ByteArrayOutputStream(64 + (icon?.size ?: 0)).apply {
            // HUD experiment A: match the fixed guidance value observed by BYDMate.
            field(2, 2)
            field(6, 1)
            bytes(7, ByteArray(0))
            bytes(8, icon ?: ByteArray(0))
            field(9, displayDistance(distanceMeters).toLong())
            bytes(10, text(road))
            field(16, 2)
            bytes(26, text(eta))
            field(28, maneuver.coerceAtLeast(0).toLong())
        }.toByteArray().wrap()

    fun clear(): ByteArray = ByteArrayOutputStream(12).apply {
        field(2, 2)
        field(6, 255)
        field(16, 1)
    }.toByteArray().wrap()

    private fun displayDistance(meters: Int): Int = meters.coerceAtLeast(0).coerceAtLeast(MIN_DISTANCE_METERS)

    private fun text(value: String): ByteArray {
        val encoded = value.toByteArray(Charsets.UTF_8)
        if (encoded.size <= MAX_TEXT_BYTES) return encoded
        // Cut on a character boundary so the HUD never receives a broken UTF-8 sequence.
        var end = MAX_TEXT_BYTES
        while (end > 0 && (encoded[end].toInt() and 0xc0) == 0x80) end--
        return encoded.copyOf(end)
    }

    private fun ByteArrayOutputStream.field(number: Int, value: Long) {
        writeVarint(number.toLong() shl 3)
        writeVarint(value)
    }

    private fun ByteArrayOutputStream.bytes(number: Int, value: ByteArray) {
        writeVarint((number.toLong() shl 3) or 2)
        writeVarint(value.size.toLong())
        write(value)
    }

    private fun ByteArray.wrap(): ByteArray = ByteArrayOutputStream(size + 4).apply {
        write(0x0a)
        writeVarint(size.toLong())
        write(this@wrap)
    }.toByteArray()

    private fun ByteArrayOutputStream.writeVarint(initialValue: Long) {
        var value = initialValue
        do {
            var next = (value and 0x7f).toInt()
            value = value ushr 7
            if (value != 0L) next = next or 0x80
            write(next)
        } while (value != 0L)
    }
}
