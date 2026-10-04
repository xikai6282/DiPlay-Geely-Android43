package com.shilapi.xcertplay.media

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.annotation.RequiresApi

/** AudioTrack.Builder and usage-routed output require API 23. */
val modernAudioSupported: Boolean
    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.M)
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M

/** Keep AudioAttributes and AudioFormat out of API 18 classes and method descriptors. */
@RequiresApi(Build.VERSION_CODES.M)
object ModernAudio {
    fun attributes(usage: Int, contentType: Int, legacyStreamType: Int?): Any {
        val builder = AudioAttributes.Builder()
        if (legacyStreamType != null && legacyStreamType in
            AudioManager.STREAM_VOICE_CALL..AudioManager.STREAM_ACCESSIBILITY
        ) {
            builder.setLegacyStreamType(legacyStreamType)
        } else {
            builder.setUsage(usage).setContentType(contentType)
        }
        return builder.build()
    }

    fun usageTrack(
        attributes: Any,
        encoding: Int,
        sampleRate: Int,
        channelMask: Int,
        bufferBytes: Int,
    ): AudioTrack {
        val format = AudioFormat.Builder()
            .setEncoding(encoding)
            .setSampleRate(sampleRate)
            .setChannelMask(channelMask)
            .build()
        return AudioTrack.Builder()
            .setAudioAttributes(attributes as AudioAttributes)
            .setAudioFormat(format)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setBufferSizeInBytes(bufferBytes)
            .build()
    }

    fun microphoneRecorder(
        source: Int,
        encoding: Int,
        sampleRate: Int,
        channelMask: Int,
        bufferBytes: Int,
    ): AudioRecord {
        val format = AudioFormat.Builder()
            .setEncoding(encoding)
            .setSampleRate(sampleRate)
            .setChannelMask(channelMask)
            .build()
        return AudioRecord.Builder()
            .setAudioSource(source)
            .setAudioFormat(format)
            .setBufferSizeInBytes(bufferBytes)
            .build()
    }

    fun legacyStreamType(attributes: Any): Int = runCatching {
        attributes.javaClass.getMethod("getLegacyStreamType").invoke(attributes) as Int
    }.getOrDefault(AudioManager.STREAM_MUSIC)
}
