package com.shilapi.xcertplay.media

import android.media.AudioTrack
import android.media.AudioRecord
import android.os.Build

/** Helpers that can be called safely on Android 4.3. */
object PortableAudio {
    fun writeBlocking(track: AudioTrack, data: ByteArray, offset: Int, length: Int): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            track.write(data, offset, length, AudioTrack.WRITE_BLOCKING)
        } else {
            @Suppress("DEPRECATION")
            track.write(data, offset, length)
        }

    fun readBlocking(recorder: AudioRecord, data: ByteArray, offset: Int, length: Int): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            recorder.read(data, offset, length, AudioRecord.READ_BLOCKING)
        } else {
            @Suppress("DEPRECATION")
            recorder.read(data, offset, length)
        }

    fun setVolume(track: AudioTrack, volume: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            runCatching { track.setVolume(volume) }
        } else {
            @Suppress("DEPRECATION")
            runCatching { track.setStereoVolume(volume, volume) }
        }
    }

    fun underrunCount(track: AudioTrack): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) track.underrunCount else 0

    fun bufferSizeInFrames(track: AudioTrack): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) track.bufferSizeInFrames else -1

    fun routedDeviceType(track: AudioTrack): Int? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) runCatching { track.routedDevice?.type }.getOrNull()
        else null

    fun bufferSizeBytes(track: AudioTrack): Int =
        runCatching { AudioTrack::class.java.getMethod("getBufferSize").invoke(track) as Int }.getOrDefault(0)

    fun routedDeviceType(recorder: AudioRecord): Int? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return null
        return runCatching { recorder.javaClass.getMethod("getRoutedDevice").invoke(recorder)?.javaClass
            ?.getMethod("getType")?.invoke(recorder.javaClass.getMethod("getRoutedDevice").invoke(recorder)) as? Int }
            .getOrNull()
    }
}
