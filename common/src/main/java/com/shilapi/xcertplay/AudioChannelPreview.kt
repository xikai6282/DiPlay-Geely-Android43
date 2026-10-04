package com.shilapi.xcertplay

import android.content.Context

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.shilapi.xcertplay.media.ModernAudio
import com.shilapi.xcertplay.media.PortableAudio
import java.io.Closeable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.sin

/** Plays one short tone through the same legacy stream route used by CarPlay audio. */
internal class AudioChannelPreview(private val context: Context? = null, private val onUnavailable: (Int) -> Unit) : Closeable {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor { task ->
        Thread(task, "diplay-channel-preview").apply { isDaemon = true }
    }
    private val generation = AtomicInteger()
    private val activeTrack = AtomicReference<AudioTrack?>()
    private var pending: Future<*>? = null
    @Volatile private var closed = false

    fun play(channel: Int, navigation: Boolean, geelyFocusGain: Int? = null) {
        if (closed) return
        require(channel in AirPlayPersistence.AUDIO_CHANNELS ||
            (geelyFocusGain != null && channel in listOf(11, 23, 25)))
        val request = generation.incrementAndGet()
        pending?.cancel(true)
        activeTrack.get()?.let { runCatching { it.stop() } }
        pending = worker.submit {
            var track: AudioTrack? = null
            val manager = context?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            val callbackVersion = AtomicInteger()
            val focusTrack = AtomicReference<AudioTrack?>()
            val listener = AudioManager.OnAudioFocusChangeListener { change ->
                if (!closed && generation.get() == request) {
                    callbackVersion.incrementAndGet()
                    val volume = when (change) {
                        AudioManager.AUDIOFOCUS_GAIN -> PREVIEW_VOLUME
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> PREVIEW_VOLUME * 0.2f
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
                        AudioManager.AUDIOFOCUS_LOSS -> 0f
                        else -> null
                    }
                    volume?.let { focusTrack.get()?.let { current -> PortableAudio.setVolume(current, it) } }
                }
            }
            var focusRequested = false
            try {
                if (closed || generation.get() != request) return@submit
                val pcm = tone()
                val minimum = AudioTrack.getMinBufferSize(
                    SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                )
                check(minimum > 0) { "No PCM output buffer is available" }
                val bufferBytes = maxOf(minimum, SAMPLE_RATE / 10 * 2)
                val built = if (channel == 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    ModernAudio.usageTrack(
                        attributes = ModernAudio.attributes(
                            usage = if (navigation) 12 else 1,
                            contentType = if (navigation) 1 else 2,
                            legacyStreamType = null,
                        ),
                        encoding = AudioFormat.ENCODING_PCM_16BIT,
                        sampleRate = SAMPLE_RATE,
                        channelMask = AudioFormat.CHANNEL_OUT_MONO,
                        bufferBytes = bufferBytes,
                    )
                } else {
                    // Match playback and let the head unit handle vendor-specific stream types.
                    @Suppress("DEPRECATION")
                    AudioTrack(if (channel == 0) AudioManager.STREAM_MUSIC else channel,
                        SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT, bufferBytes, AudioTrack.MODE_STREAM)
                }
                track = built
                focusTrack.set(built)
                check(built.state == AudioTrack.STATE_INITIALIZED) { "Audio output did not initialize" }
                if (geelyFocusGain != null) {
                    check(manager != null) { "Audio focus service unavailable" }
                    PortableAudio.setVolume(built, 0f)
                    val versionBeforeRequest = callbackVersion.get()
                    @Suppress("DEPRECATION")
                    val result = manager.requestAudioFocus(listener, channel, geelyFocusGain)
                    focusRequested = true
                    check(result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) { "Audio focus denied" }
                    if (callbackVersion.get() == versionBeforeRequest) {
                        PortableAudio.setVolume(built, PREVIEW_VOLUME)
                    }
                }
                if (closed || generation.get() != request) return@submit
                activeTrack.set(built)
                if (geelyFocusGain == null) PortableAudio.setVolume(built, PREVIEW_VOLUME)
                built.play()
                var written = 0
                while (written < pcm.size && !closed && generation.get() == request) {
                    val count = PortableAudio.writeBlocking(built, pcm, written, minOf(4096, pcm.size - written))
                    check(count > 0) { "Could not write preview tone" }
                    written += count
                }
                if (!closed && generation.get() == request && written == pcm.size) {
                    Log.i(TAG, "Preview started channel=$channel navigation=$navigation")
                }
                Thread.sleep(120L)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            } catch (error: Exception) {
                Log.w(TAG, "Channel preview unavailable channel=$channel", error)
                mainHandler.post {
                    if (!closed && generation.get() == request) onUnavailable(channel)
                }
            } finally {
                focusTrack.set(null)
                if (focusRequested) runCatching { manager?.abandonAudioFocus(listener) }
                activeTrack.compareAndSet(track, null)
                track?.let { runCatching { it.stop() }; it.release() }
            }
        }
    }

    override fun close() {
        closed = true
        generation.incrementAndGet()
        pending?.cancel(true)
        activeTrack.get()?.let { runCatching { it.stop() } }
        worker.shutdownNow()
    }

    private fun tone(): ByteArray {
        val sampleCount = SAMPLE_RATE * TONE_MILLIS / 1000
        val fadeSamples = SAMPLE_RATE / 100
        return ByteArray(sampleCount * 2).also { pcm ->
            for (index in 0 until sampleCount) {
                val fade = minOf(1.0, index.toDouble() / fadeSamples,
                    (sampleCount - index - 1).toDouble() / fadeSamples).coerceAtLeast(0.0)
                val sample = (sin(2.0 * Math.PI * 880.0 * index / SAMPLE_RATE) *
                    fade * Short.MAX_VALUE * 0.45).toInt()
                pcm[index * 2] = sample.toByte()
                pcm[index * 2 + 1] = (sample ushr 8).toByte()
            }
        }
    }

    private companion object {
        private const val TAG = "DiPlayAudioPreview"
        private const val SAMPLE_RATE = 48_000
        private const val TONE_MILLIS = 600
        private const val PREVIEW_VOLUME = 0.6f
    }
}
