package com.shilapi.xcertplay.media

import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.os.Build

/**
 * MediaCodecList's instance form and codecInfos are API 21. Android 4.1-4.3 expose the static
 * getCodecCount()/getCodecInfoAt(int) pair instead, added in API 16, so enumerate through
 * whichever the running platform provides.
 */
object CodecCompat {
    fun decoders(): List<MediaCodecInfo> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos.toList()
        } else {
            (0 until MediaCodecList.getCodecCount()).mapNotNull { index ->
                runCatching { MediaCodecList.getCodecInfoAt(index) }.getOrNull()
            }
        }

    fun videoDecoderFor(mime: String): MediaCodecInfo? = decoders().firstOrNull { info ->
        !info.isEncoder && info.supportedTypes.any { it.equals(mime, ignoreCase = true) }
    }
}