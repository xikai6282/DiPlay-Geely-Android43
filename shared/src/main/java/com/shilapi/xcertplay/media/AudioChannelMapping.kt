package com.shilapi.xcertplay.media

import android.media.AudioManager

internal enum class AudioChannelMappingMode {
    MOBILE_COMPATIBLE,
    AUTOMOTIVE_BUS,
}

internal enum class AudioChannel {
    MEDIA,
    PHONE,
    ASSISTANT,
    NAVIGATION,
}

internal enum class AudioContentType {
    MUSIC,
    SPEECH,
}

internal data class AudioChannelSelection(
    val channel: AudioChannel,
    val contentType: AudioContentType,
    val streamType: Int = AudioManager.STREAM_MUSIC,
)

/**
 * Maps CarPlay stream metadata to Android audio routing values.
 *
 * Each selection carries the [AudioChannel] / [AudioContentType] pair used for usage-based
 * AudioAttributes, plus the stream type used when the stream-type AudioTrack path is active.
 * Guidance resolves to [AudioChannel.NAVIGATION]. Automotive routing keeps compatibility
 * audio on the media bus, so it receives the configured media buffer and output route.
 */
internal object AudioChannelMapper {
    const val STREAM_TYPE_MAIN_HIGH_AUDIO = 102
    const val DEFAULT_NAVIGATION_STREAM_TYPE = 14

    fun map(
        audioType: String,
        payloadType: Int,
        mode: AudioChannelMappingMode,
        navigationStreamType: Int = DEFAULT_NAVIGATION_STREAM_TYPE,
    ): AudioChannelSelection {
        val normalized = audioType.lowercase()
        return when (mode) {
            AudioChannelMappingMode.MOBILE_COMPATIBLE -> mapMobileCompatible(normalized, payloadType, navigationStreamType)
            AudioChannelMappingMode.AUTOMOTIVE_BUS -> mapAutomotiveBus(normalized, payloadType, navigationStreamType)
        }
    }

    fun usesNavigationStream(audioType: String, payloadType: Int, advanced: Boolean): Boolean =
        !advanced && map(audioType, payloadType, AudioChannelMappingMode.MOBILE_COMPATIBLE).channel == AudioChannel.NAVIGATION

    private fun mapMobileCompatible(
        audioType: String,
        payloadType: Int,
        navigationStreamType: Int,
    ): AudioChannelSelection = when (audioType) {
        "telephony" -> AudioChannelSelection(AudioChannel.PHONE, AudioContentType.SPEECH)
        "speechrecognition" ->
            AudioChannelSelection(AudioChannel.ASSISTANT, AudioContentType.SPEECH)
        "media" -> AudioChannelSelection(AudioChannel.MEDIA, AudioContentType.MUSIC)
        "default", "alert", "compatibility" ->
            AudioChannelSelection(AudioChannel.NAVIGATION, AudioContentType.SPEECH, navigationStreamType)
        else -> mainHighAudioOrNavigation(payloadType, navigationStreamType)
    }

    private fun mapAutomotiveBus(
        audioType: String,
        payloadType: Int,
        navigationStreamType: Int,
    ): AudioChannelSelection = when (audioType) {
        "telephony" -> AudioChannelSelection(AudioChannel.PHONE, AudioContentType.SPEECH)
        "speechrecognition" ->
            AudioChannelSelection(AudioChannel.ASSISTANT, AudioContentType.SPEECH)
        "media", "compatibility" ->
            AudioChannelSelection(AudioChannel.MEDIA, AudioContentType.MUSIC)
        "default", "alert" -> AudioChannelSelection(AudioChannel.NAVIGATION, AudioContentType.SPEECH, navigationStreamType)
        else -> mainHighAudioOrNavigation(payloadType, navigationStreamType)
    }

    private fun mainHighAudioOrNavigation(payloadType: Int, navigationStreamType: Int): AudioChannelSelection =
        if (payloadType == STREAM_TYPE_MAIN_HIGH_AUDIO) {
            AudioChannelSelection(AudioChannel.MEDIA, AudioContentType.MUSIC)
        } else {
            AudioChannelSelection(AudioChannel.NAVIGATION, AudioContentType.SPEECH, navigationStreamType)
        }
}

/** Keeps the user-facing zero/automatic preference separate from AudioManager's real stream 0. */
internal object AudioStreamRouting {
    const val LEGACY_STREAM_MUSIC = 3
    const val LEGACY_STREAM_VOICE_CALL = 0

    fun override(
        channel: AudioChannel,
        audioType: String,
        geelyAudio: GeelyAudioCapabilities?,
        separateGeelyAlerts: Boolean,
        mediaChannel: Int,
        navigationChannel: Int,
    ): Int? = when (channel) {
        AudioChannel.MEDIA -> geelyAudio?.carPlay ?: mediaChannel.takeIf { it != 0 }
        AudioChannel.NAVIGATION -> geelyAudio?.navigationStream(audioType, separateGeelyAlerts)
            ?: navigationChannel.takeIf { it != 0 }
        AudioChannel.PHONE, AudioChannel.ASSISTANT -> null
    }

    fun legacyStreamType(channel: AudioChannel, streamOverride: Int?): Int =
        streamOverride ?: when (channel) {
            AudioChannel.MEDIA, AudioChannel.NAVIGATION -> LEGACY_STREAM_MUSIC
            AudioChannel.PHONE, AudioChannel.ASSISTANT -> LEGACY_STREAM_VOICE_CALL
        }
}
