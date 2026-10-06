package com.shilapi.xcertplay

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.media.RemoteControlClient
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import com.shilapi.xcertplay.airplay.CarPlayMediaButton
import com.shilapi.xcertplay.compat.systemService
import com.shilapi.xcertplay.media.CarPlayNowPlaying
import com.shilapi.xcertplay.orchestration.CarPlayController
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/** Hardware media keys routed to the iPhone or the on-screen CarPlay video player. */
internal object CarPlayMediaKeys {
    private const val TAG = "DiPlay-MediaKeys"
    private val mainHandler = Handler(Looper.getMainLooper())
    private val artworkQueue = NowPlayingArtworkQueue(
        worker = Executors.newSingleThreadExecutor { task ->
            Thread(task, "diplay-now-playing-artwork").apply { isDaemon = true }
        },
        main = Executor { mainHandler.post(it) },
        decode = ::decodeArtwork,
        publish = ::onArtworkDecoded,
        discard = Bitmap::recycle,
    )

    private var artworkOwner: Any? = null
    private var controller: CarPlayController? = null
    /** API 21+ implementation is isolated in ModernSession so this object loads on API 18. */
    private var session: ModernSession? = null
    private var appContext: Context? = null
    private var mediaAudioActive = false
    private var nowPlaying = CarPlayNowPlaying()
    private var elapsedUpdatedAt = 0L
    private var artwork: Bitmap? = null
    private val artworkCache = LinkedHashMap<Int, Bitmap?>()

    /** API 18 routes hardware keys through AudioManager and RemoteControlClient. */
    private val legacyFocusListener = AudioManager.OnAudioFocusChangeListener { change ->
        if (change == AudioManager.AUDIOFOCUS_LOSS) synchronized(this) { legacyFocusHeld = false }
    }
    private var legacyFocusHeld = false
    private var legacyReceiver: BroadcastReceiver? = null
    private var legacyPendingIntent: PendingIntent? = null
    private var legacyRemoteControlClient: RemoteControlClient? = null

    @Synchronized
    fun attach(context: Context, next: CarPlayController) {
        if (controller !== next) {
            releaseLocked()
            artworkOwner = artworkQueue.newSession()
        }
        appContext = context.applicationContext
        controller = next
        next.playbackListener = { playing -> onIphonePlaying(next, playing) }
        next.nowPlayingListener = { update -> onNowPlayingChanged(next, update) }
        next.artworkListener = { id, bytes -> onArtworkChanged(next, id, bytes) }
    }

    /** Ends key handling for [expected]; a newer controller's state is left alone. */
    @Synchronized
    fun detach(expected: CarPlayController?) {
        if (expected == null || controller !== expected) return
        expected.playbackListener = null
        expected.nowPlayingListener = null
        expected.artworkListener = null
        controller = null
        releaseLocked()
    }

    /** Called when CarPlay audio starts or stops; may run on any thread. */
    fun onMediaAudioChanged(active: Boolean) {
        mainHandler.post { synchronized(this) { updateLocked(active) } }
    }

    private fun onIphonePlaying(expected: CarPlayController, playing: Boolean) {
        if (!playing) return
        mainHandler.post {
            synchronized(this) {
                if (controller !== expected) return@synchronized
                regainFocusLocked()
                publishPlaybackStateLocked()
            }
        }
    }

    private fun onNowPlayingChanged(expected: CarPlayController, update: CarPlayNowPlaying) {
        mainHandler.post {
            synchronized(this) {
                if (controller !== expected) return@synchronized
                if (nowPlaying.artworkTransferId != update.artworkTransferId) {
                    artwork = update.artworkTransferId?.let { id ->
                        if (artworkCache.containsKey(id)) artworkCache[id] else null
                    }
                }
                if (nowPlaying.elapsedMillis != update.elapsedMillis) elapsedUpdatedAt = SystemClock.elapsedRealtime()
                nowPlaying = update
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) session?.setMetadata(update, artwork)
                publishPlaybackStateLocked()
            }
        }
    }

    @Synchronized
    private fun onArtworkChanged(expected: CarPlayController, id: Int, bytes: ByteArray) {
        if (controller !== expected) return
        artworkOwner?.let { artworkQueue.submit(it, id, bytes) }
    }

    @Synchronized
    private fun onArtworkDecoded(expected: Any, id: Int, decoded: Bitmap?) {
        if (artworkOwner !== expected) {
            decoded?.recycle()
            return
        }
        artworkCache.remove(id)
        artworkCache[id] = decoded
        while (artworkCache.size > MAX_CACHED_ARTWORK) artworkCache.remove(artworkCache.keys.first())
        if (nowPlaying.artworkTransferId == id) {
            artwork = decoded
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) session?.setMetadata(nowPlaying, artwork)
        }
    }

    private fun updateLocked(active: Boolean) {
        val context = appContext ?: return
        if (controller == null) return
        mediaAudioActive = active
        if (active) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (session == null) session = ModernSession(context, mainHandler, ::send)
                session?.setMetadata(nowPlaying, artwork)
            } else {
                registerLegacyMediaButtons(context)
            }
            regainFocusLocked()
        }
        publishPlaybackStateLocked()
    }

    private fun registerLegacyMediaButtons(context: Context) {
        if (legacyReceiver != null) return
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context?, intent: Intent?) {
                if (intent?.action != Intent.ACTION_MEDIA_BUTTON) return
                @Suppress("DEPRECATION")
                val event = intent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT) ?: return
                val index = CarPlayMediaButton.forKeyCode(event.keyCode) ?: return
                if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                    send(index, "legacyKeyEvent")
                }
            }
        }
        @Suppress("DEPRECATION")
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_MEDIA_BUTTON))
        legacyReceiver = receiver

        @Suppress("DEPRECATION")
        val pending = PendingIntent.getBroadcast(
            context,
            0,
            Intent(Intent.ACTION_MEDIA_BUTTON).setPackage(context.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT,
        )
        legacyPendingIntent = pending
        val audio = context.systemService(AudioManager::class.java, "audio")
        if (audio != null) {
            @Suppress("DEPRECATION")
            audio.registerMediaButtonEventReceiver(pending)
            val client = RemoteControlClient(pending)
            @Suppress("DEPRECATION")
            audio.registerRemoteControlClient(client)
            legacyRemoteControlClient = client
        }
        Log.i(TAG, "legacy media button receiver registered with RCC")
    }

    private fun unregisterLegacyMediaButtons() {
        val audio = appContext?.systemService(AudioManager::class.java, "audio")
        legacyRemoteControlClient?.let { client ->
            @Suppress("DEPRECATION")
            runCatching { audio?.unregisterRemoteControlClient(client) }
        }
        legacyRemoteControlClient = null
        legacyPendingIntent?.let { pending ->
            @Suppress("DEPRECATION")
            runCatching { audio?.unregisterMediaButtonEventReceiver(pending) }
        }
        legacyPendingIntent = null
        legacyReceiver?.let { receiver -> runCatching { appContext?.unregisterReceiver(receiver) } }
        legacyReceiver = null
    }

    private fun regainFocusLocked() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            session?.regainFocus()
            return
        }
        val context = appContext ?: return
        registerLegacyMediaButtons(context)
        // H52 track coordinator owns vendor focus; a second MUSIC focus mutes that route.
        if (AirPlayPersistence.loadGeelyAudioRouting(context)) return
        if (legacyFocusHeld) return
        val audio = context.systemService(AudioManager::class.java, "audio") ?: return
        @Suppress("DEPRECATION")
        val granted = audio.requestAudioFocus(
            legacyFocusListener,
            AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN,
        )
        legacyFocusHeld = granted == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        Log.i(TAG, "legacy audio focus granted=$legacyFocusHeld")
    }

    private fun publishPlaybackStateLocked() {
        val playing = if (nowPlaying.elapsedMillis != null || nowPlaying.title != null) {
            nowPlaying.playing
        } else {
            mediaAudioActive
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            session?.setPlaybackState(playing, nowPlaying.elapsedMillis, elapsedUpdatedAt)
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            legacyRemoteControlClient?.setPlaybackState(
                if (playing) RemoteControlClient.PLAYSTATE_PLAYING else RemoteControlClient.PLAYSTATE_PAUSED,
            )
        }
    }

    private fun releaseLocked() {
        artworkOwner = null
        artworkQueue.clear()
        unregisterLegacyMediaButtons()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) session?.close()
        session = null
        if (legacyFocusHeld) {
            @Suppress("DEPRECATION")
            appContext?.systemService(AudioManager::class.java, "audio")?.abandonAudioFocus(legacyFocusListener)
        }
        legacyFocusHeld = false
        mediaAudioActive = false
        nowPlaying = CarPlayNowPlaying()
        elapsedUpdatedAt = 0L
        artwork = null
        artworkCache.clear()
    }

    private fun send(index: Int, source: String) {
        if (CarPlayVideo.onMediaKey(index)) {
            Log.i(TAG, "media key $source -> car video player $index")
            return
        }
        val sent = synchronized(this) { controller }?.sendMediaButton(index) ?: false
        Log.i(TAG, "media key $source -> CarPlay $index sent=$sent")
    }

    private fun decodeArtwork(bytes: ByteArray): Bitmap? {
        if (bytes.isEmpty()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth !in 1..MAX_ARTWORK_SOURCE_DIMENSION ||
            bounds.outHeight !in 1..MAX_ARTWORK_SOURCE_DIMENSION
        ) return null
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > MAX_ARTWORK_DIMENSION * 2) sample *= 2
        val decoded = BitmapFactory.decodeByteArray(
            bytes,
            0,
            bytes.size,
            BitmapFactory.Options().apply { inSampleSize = sample },
        ) ?: return null
        val largest = maxOf(decoded.width, decoded.height)
        if (largest <= MAX_ARTWORK_DIMENSION) return decoded
        val scale = MAX_ARTWORK_DIMENSION.toFloat() / largest
        return Bitmap.createScaledBitmap(
            decoded,
            (decoded.width * scale).toInt().coerceAtLeast(1),
            (decoded.height * scale).toInt().coerceAtLeast(1),
            true,
        ).also { scaled -> if (scaled !== decoded) decoded.recycle() }
    }

    private const val MAX_ARTWORK_DIMENSION = 384
    private const val MAX_ARTWORK_SOURCE_DIMENSION = 8_192
    private const val MAX_CACHED_ARTWORK = 4
}
