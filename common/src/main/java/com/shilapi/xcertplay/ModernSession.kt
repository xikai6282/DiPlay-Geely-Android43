package com.shilapi.xcertplay

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.Handler
import android.view.KeyEvent
import androidx.annotation.RequiresApi
import com.shilapi.xcertplay.airplay.CarPlayMediaButton
import com.shilapi.xcertplay.compat.systemService
import com.shilapi.xcertplay.media.CarPlayNowPlaying

/** API 21+ media session, isolated so Android 4.3 never verifies framework session types. */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
internal class ModernSession(
    context: Context,
    handler: Handler,
    private val send: (Int, String) -> Unit,
) {
    private val audioManager: AudioManager? = context.systemService(AudioManager::class.java, "audio")
    private val session = MediaSession(context, "DiPlay CarPlay")
    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS -> synchronized(this) { focusHeld = false }
            AudioManager.AUDIOFOCUS_GAIN -> synchronized(this) { focusHeld = true }
        }
    }
    private var focusHeld = false
    private val focusRequest: AudioFocusRequest? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(attributes)
            .setOnAudioFocusChangeListener(focusListener, handler)
            .build()
    } else null

    init {
        session.setCallback(CarPlayMediaCallback(send), handler)
        session.isActive = true
    }

    fun setMetadata(info: CarPlayNowPlaying, artwork: Bitmap?) {
        session.setMetadata(androidMetadata(info, artwork))
    }

    fun setPlaybackState(playing: Boolean, positionMillis: Long?, updatedAt: Long) {
        session.setPlaybackState(
            PlaybackState.Builder()
                .setActions(CarPlayMediaCallback.ACTIONS)
                .setState(
                    if (playing) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED,
                    positionMillis ?: PlaybackState.PLAYBACK_POSITION_UNKNOWN,
                    if (playing) 1f else 0f,
                    updatedAt,
                )
                .build(),
        )
    }

    @Synchronized
    fun regainFocus() {
        if (focusHeld) return
        val request = focusRequest
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && request != null) {
            audioManager?.requestAudioFocus(request)
        } else {
            @Suppress("DEPRECATION")
            audioManager?.requestAudioFocus(focusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        }
        focusHeld = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    @Synchronized
    fun close() {
        if (focusHeld) {
            val request = focusRequest
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && request != null) {
                audioManager?.abandonAudioFocusRequest(request)
            } else {
                @Suppress("DEPRECATION")
                audioManager?.abandonAudioFocus(focusListener)
            }
            focusHeld = false
        }
        session.isActive = false
        session.release()
    }

    internal companion object {
        internal fun androidMetadata(info: CarPlayNowPlaying, artwork: Bitmap?): MediaMetadata =
            MediaMetadata.Builder().apply {
                info.title?.let {
                    putString(MediaMetadata.METADATA_KEY_TITLE, it)
                    putString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE, it)
                }
                info.artist?.let {
                    putString(MediaMetadata.METADATA_KEY_ARTIST, it)
                    putString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE, it)
                }
                info.album?.let { putString(MediaMetadata.METADATA_KEY_ALBUM, it) }
                info.durationMillis?.let { putLong(MediaMetadata.METADATA_KEY_DURATION, it) }
                info.sourceApp?.let { putString(MediaMetadata.METADATA_KEY_DISPLAY_DESCRIPTION, it) }
                artwork?.let {
                    putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, it)
                    putBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON, it)
                }
            }.build()
    }
}

/** API 21+ input adapter, kept out of API-18-loadable CarPlayMediaKeys. */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
internal class CarPlayMediaCallback(private val send: (Int, String) -> Unit) : MediaSession.Callback() {
    override fun onMediaButtonEvent(mediaButtonIntent: Intent): Boolean {
        @Suppress("DEPRECATION")
        val event = mediaButtonIntent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT) ?: return false
        val index = CarPlayMediaButton.forKeyCode(event.keyCode) ?: return super.onMediaButtonEvent(mediaButtonIntent)
        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
            send(index, KeyEvent.keyCodeToString(event.keyCode))
        }
        return true
    }

    override fun onPlay() = send(CarPlayMediaButton.PLAY, "play")
    override fun onPause() = send(CarPlayMediaButton.PAUSE, "pause")
    override fun onSkipToNext() = send(CarPlayMediaButton.NEXT, "next")
    override fun onSkipToPrevious() = send(CarPlayMediaButton.PREVIOUS, "previous")

    internal companion object {
        internal const val ACTIONS = PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or
            PlaybackState.ACTION_PLAY_PAUSE or PlaybackState.ACTION_SKIP_TO_NEXT or
            PlaybackState.ACTION_SKIP_TO_PREVIOUS
    }
}
