package com.shilapi.xcertplay.media

import com.shilapi.xcertplay.airplay.VideoCodec
import java.io.ByteArrayOutputStream

/**
 * Pure byte helpers that convert the CarPlay screen/audio payloads into the
 * records Android MediaCodec and AudioTrack expect. Kept free of Android types
 * so they stay testable on the JVM.
 */
object MediaCodecSupport {
    private val START_CODE = byteArrayOf(0x00, 0x00, 0x00, 0x01)

    /** Splits an AVCDecoderConfigurationRecord into raw first SPS and first PPS. */
    fun avcParameterSets(codecData: ByteArray): Pair<ByteArray, ByteArray> {
        if (codecData.size < 7) return emptySet()
        var cursor = 6
        val sps = readParameterSets(codecData, cursor, codecData[5].toInt() and 0x1f)
        cursor += sps.sumOf { it.size + 2 }
        if (cursor >= codecData.size) return emptySet()
        val pps = readParameterSets(codecData, cursor + 1, codecData[cursor].toInt() and 0xff)
        return (sps.firstOrNull() ?: ByteArray(0)) to (pps.firstOrNull() ?: ByteArray(0))
    }

    /**
     * Converts an HEVCDecoderConfigurationRecord (hvcC) into Annex B VPS/SPS/PPS CSD.
     *
     * Android video decoders expect the initialization data as NAL units with start codes,
     * not as the raw ISO-BMFF hvcC record.
     */
    fun hevcCodecSpecificData(codecData: ByteArray): ByteArray {
        if (codecData.size < HEVC_FIXED_RECORD_SIZE || codecData[0].toInt() != 1) {
            return ByteArray(0)
        }

        var cursor = HEVC_ARRAY_COUNT_OFFSET
        val arrayCount = codecData[cursor++].toInt() and 0xff
        val output = ByteArrayOutputStream()
        repeat(arrayCount) {
            if (cursor >= codecData.size) return ByteArray(0)
            cursor += 1 // array_completeness, reserved, and nal_unit_type
            if (cursor + 2 > codecData.size) return ByteArray(0)
            val nalUnitCount = readU16Be(codecData, cursor)
            cursor += 2
            repeat(nalUnitCount) {
                if (cursor + 2 > codecData.size) return ByteArray(0)
                val nalUnitLength = readU16Be(codecData, cursor)
                cursor += 2
                if (cursor + nalUnitLength > codecData.size) return ByteArray(0)
                output.write(START_CODE)
                output.write(codecData, cursor, nalUnitLength)
                cursor += nalUnitLength
            }
        }
        return output.toByteArray()
    }

    /** Converts CarPlay's length-prefixed NAL units into an Annex B byte stream. */
    fun toAnnexB(lengthPrefixed: ByteArray): ByteArray {
        if (lengthPrefixed.size >= 4 &&
            lengthPrefixed[0] == 0.toByte() &&
            lengthPrefixed[1] == 0.toByte() &&
            lengthPrefixed[2] == 0.toByte() &&
            lengthPrefixed[3] == 1.toByte()
        ) {
            return lengthPrefixed
        }

        var inputOffset = 0
        var outputSize = 0
        while (inputOffset + 4 <= lengthPrefixed.size) {
            val length = readU32Be(lengthPrefixed, inputOffset)
            inputOffset += 4
            if (length <= 0 || length > lengthPrefixed.size - inputOffset) return ByteArray(0)
            if (length > Int.MAX_VALUE - outputSize - START_CODE.size) return ByteArray(0)
            outputSize += START_CODE.size + length
            inputOffset += length
        }
        if (outputSize == 0 || inputOffset != lengthPrefixed.size) return ByteArray(0)

        val output = ByteArray(outputSize)
        var offset = 0
        var outputOffset = 0
        while (offset + 4 <= lengthPrefixed.size) {
            val length = readU32Be(lengthPrefixed, offset)
            offset += 4
            if (length <= 0 || offset + length > lengthPrefixed.size) break
            START_CODE.copyInto(output, outputOffset)
            outputOffset += START_CODE.size
            lengthPrefixed.copyInto(output, outputOffset, offset, offset + length)
            outputOffset += length
            offset += length
        }
        return output
    }

    /** IDR (AVC) or IRAP (HEVC) starts an independently decodable reference chain. */
    fun isRandomAccess(annexB: ByteArray, codec: VideoCodec): Boolean {
        var cursor = 0
        while (cursor + 3 < annexB.size) {
            val prefix = when {
                annexB[cursor] != 0.toByte() || annexB[cursor + 1] != 0.toByte() -> 0
                annexB[cursor + 2] == 1.toByte() -> 3
                annexB[cursor + 2] == 0.toByte() && annexB[cursor + 3] == 1.toByte() -> 4
                else -> 0
            }
            if (prefix > 0 && cursor + prefix < annexB.size) {
                val header = annexB[cursor + prefix].toInt() and 255
                if (codec == VideoCodec.H264 && header and 31 == 5) return true
                if (codec == VideoCodec.H265 && cursor + prefix + 1 < annexB.size &&
                    (header shr 1) and 63 in 16..21) return true
                cursor += prefix
            } else cursor++
        }
        return false
    }

    /** Wraps one raw AAC-LC access unit in an MPEG-4 ADTS frame. */
    fun adtsFrame(accessUnit: ByteArray, sampleRate: Int, channels: Int): ByteArray {
        val frequencyIndex = aacFrequencyIndex(sampleRate)
        val channelConfig = channels.coerceIn(1, 7)
        val frameLength = accessUnit.size + 7
        val header = ByteArray(7)
        header[0] = 0xff.toByte()
        header[1] = 0xf1.toByte()
        header[2] = ((1 shl 6) or (frequencyIndex shl 2) or (channelConfig ushr 2)).toByte()
        header[3] = (((channelConfig and 0x3) shl 6) or (frameLength ushr 11)).toByte()
        header[4] = ((frameLength ushr 3) and 0xff).toByte()
        header[5] = (((frameLength and 0x7) shl 5) or 0x1f).toByte()
        header[6] = 0xfc.toByte()
        return header + accessUnit
    }

    /** Extracts one RFC 3640 AAC access unit from an RTP payload. */
    fun aacAccessUnit(rtpPayload: ByteArray): ByteArray {
        if (rtpPayload.size < 4) return ByteArray(0)
        val headerBits = readU16Be(rtpPayload, 0)
        if (headerBits < 16 || headerBits % 16 != 0) return ByteArray(0)
        val headerBytes = headerBits / 8
        if (2 + headerBytes > rtpPayload.size) return ByteArray(0)
        val auSize = (readU16Be(rtpPayload, 2) shr 3) and 0x1fff
        val start = 2 + headerBytes
        val end = minOf(start + auSize, rtpPayload.size)
        return if (end <= start) ByteArray(0) else rtpPayload.copyOfRange(start, end)
    }

    /** MPEG-4 sampling frequency index used by both ADTS and AudioSpecificConfig. */
    fun aacFrequencyIndex(sampleRate: Int): Int = when (sampleRate) {
        96_000 -> 0
        88_200 -> 1
        64_000 -> 2
        48_000 -> 3
        44_100 -> 4
        32_000 -> 5
        24_000 -> 6
        22_050 -> 7
        16_000 -> 8
        12_000 -> 9
        11_025 -> 10
        8_000 -> 11
        7_350 -> 12
        else -> 3
    }

    private fun emptySet(): Pair<ByteArray, ByteArray> = ByteArray(0) to ByteArray(0)

    private fun readParameterSets(source: ByteArray, offset: Int, count: Int): List<ByteArray> {
        val sets = ArrayList<ByteArray>(count)
        var cursor = offset
        var index = 0
        while (index < count && cursor + 2 <= source.size) {
            index++
            val length = readU16Be(source, cursor)
            cursor += 2
            if (cursor + length > source.size) break
            sets.add(source.copyOfRange(cursor, cursor + length))
            cursor += length
        }
        return sets
    }

    private fun readU16Be(source: ByteArray, offset: Int): Int =
        ((source[offset].toInt() and 0xff) shl 8) or (source[offset + 1].toInt() and 0xff)

    private fun readU32Be(source: ByteArray, offset: Int): Int =
        ((source[offset].toInt() and 0xff) shl 24) or
            ((source[offset + 1].toInt() and 0xff) shl 16) or
            ((source[offset + 2].toInt() and 0xff) shl 8) or
            (source[offset + 3].toInt() and 0xff)

    private const val HEVC_FIXED_RECORD_SIZE = 23
    private const val HEVC_ARRAY_COUNT_OFFSET = 22
}
