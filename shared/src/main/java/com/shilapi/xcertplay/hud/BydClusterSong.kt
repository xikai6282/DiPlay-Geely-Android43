package com.shilapi.xcertplay.hud

import android.annotation.SuppressLint
import android.content.Context
import android.util.Base64
import android.util.Log
import com.shilapi.xcertplay.iap2.body.Iap2BodyReader
import com.shilapi.xcertplay.iap2.wire.Iap2Frame
import java.lang.reflect.InvocationTargetException
import java.util.concurrent.Executors

/** What the dashboard's music card shows. */
internal data class ClusterSong(val text: String, val playing: Boolean)

/**
 * The CarPlay song for the dashboard, from iAP2 NowPlayingUpdate (0x5001): title (1) and artist (12)
 * in MediaItemAttributes, playback status in PlaybackAttributes. Updates carry only what changed; a
 * new title replaces the item, so a missing artist then means none.
 */
internal class ClusterSongState {
    private var title: String? = null
    private var artist: String? = null
    private var playing = false
    private var last: ClusterSong? = null

    /** Updates the cached card; null can mean unchanged or cleared, so consumers compare [current]. */
    fun accept(frame: Iap2Frame): ClusterSong? {
        if (frame.messageId != NOW_PLAYING_UPDATE) return null
        val body = runCatching { Iap2BodyReader.of(frame) }.getOrNull() ?: return null
        runCatching { body.optionalGroup(ITEM) }.getOrNull()?.let { item ->
            val nextTitle = runCatching { item.optionalString(TITLE) }.getOrNull()
            if (nextTitle != null) {
                title = nextTitle
                artist = runCatching { item.optionalString(ARTIST) }.getOrNull()
            } else {
                runCatching { item.optionalString(ARTIST) }.getOrNull()?.let { artist = it }
            }
        }
        runCatching { body.optionalGroup(PLAYBACK)?.optionalU8(STATUS) }.getOrNull()?.let { status ->
            playing = status == STATUS_PLAYING || status == STATUS_SEEK_FORWARD || status == STATUS_SEEK_BACKWARD
        }
        val next = text(title, artist)?.let { ClusterSong(it, playing) }
        if (next == last) return null
        last = next
        return next
    }

    /** The card for the song known so far, if any. */
    fun current(): ClusterSong? = last

    /** The session ended: forget the song. */
    fun clear() {
        title = null
        artist = null
        playing = false
        last = null
    }

    companion object {
        const val NOW_PLAYING_UPDATE = 0x5001
        private const val ITEM = 0
        private const val TITLE = 1
        private const val ARTIST = 12
        private const val PLAYBACK = 1
        private const val STATUS = 0
        private const val STATUS_PLAYING = 1
        private const val STATUS_SEEK_FORWARD = 3
        private const val STATUS_SEEK_BACKWARD = 4

        /** The dashboard takes at most 255 bytes of UTF-16LE. */
        const val MAX_TEXT_BYTES = 255

        /** "Title — Artist", shortened to what the dashboard takes; null without a title. */
        fun text(title: String?, artist: String?): String? {
            val name = title?.trim()?.takeIf { it.isNotEmpty() } ?: return null
            val full = artist?.trim()?.takeIf { it.isNotEmpty() }?.let { "$name — $it" } ?: name
            var end = full.length
            while (full.substring(0, end).toByteArray(Charsets.UTF_16LE).size > MAX_TEXT_BYTES) {
                end--
                if (end > 0 && Character.isLowSurrogate(full[end])) end--
            }
            return full.substring(0, end)
        }
    }
}

/**
 * Optional, needs ADB over network: shows the CarPlay song in the dashboard's music card. Apps cannot
 * write it (BYDAutoInstrumentDevice checks a BYD permission), but autoservice accepts the adb shell
 * user, so DiPlay runs [BydClusterSongTool] from its own APK under the head unit's adb shell. The card
 * appears as BYD's "other" music source; CarPlay's own source value is not drawn on this dashboard.
 */
internal object BydClusterSong {
    private const val TAG = "DiPlay-BYD-Song"
    private const val SOURCE_OTHERS = 11
    private const val STATE_PLAYING = 1
    private const val STATE_PAUSED = 2
    private const val STATE_STOPPED = 3

    private val shell = BydAdbShell(TAG)
    private val writer = Executors.newSingleThreadExecutor { Thread(it, "diplay-cluster-song").apply { isDaemon = true } }
    private val state = ClusterSongState() // guards wanted too
    @Volatile private var context: Context? = null
    private var wanted: ClusterSong? = null
    private var shown: ClusterSong? = null // writer thread
    private var firstLogged = false // writer thread

    fun attach(appContext: Context) {
        context = appContext.applicationContext
    }

    /** NowPlayingUpdate frames; the song is followed even while the setting is off, so it can show at once. */
    fun onFrame(frame: Iap2Frame) {
        val app = context ?: return
        val song = synchronized(state) {
            val previous = state.current()
            state.accept(frame)
            state.current().also { if (it == previous) return }
        }
        if (BydOutputSettings.clusterSong(app)) {
            if (song == null) stop(app) else show(app, song)
        }
    }

    /** The setting changed: show the current song now, or stop the card DiPlay set. */
    fun settingChanged(enabled: Boolean) {
        val app = context ?: return
        if (enabled) synchronized(state) { state.current() }?.let { show(app, it) } else stop(app)
    }

    /** The session ended: forget the song and stop the card DiPlay set. */
    fun end() {
        val app = context ?: return
        synchronized(state) { state.clear() }
        stop(app)
    }

    private fun show(app: Context, song: ClusterSong) {
        synchronized(state) { wanted = song }
        writer.execute { write(app, song) }
    }

    private fun stop(app: Context) {
        synchronized(state) { wanted = null }
        writer.execute { clear(app) }
    }

    private fun write(app: Context, song: ClusterSong) {
        // Only the newest song matters; older queued ones are skipped.
        if (synchronized(state) { wanted } != song || song == shown) return
        val text = Base64.encodeToString(song.text.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        val playing = if (song.playing) STATE_PLAYING else STATE_PAUSED
        if (run(app, "$SOURCE_OTHERS $playing $text")) {
            shown = song
            if (!firstLogged) {
                firstLogged = true
                Log.i(TAG, "song on the dashboard")
            }
        }
    }

    private fun clear(app: Context) {
        if (shown == null || synchronized(state) { wanted } != null) return
        if (run(app, "- $STATE_STOPPED -")) shown = null
    }

    private fun run(app: Context, args: String): Boolean {
        val apk = app.applicationInfo.sourceDir
        val output = shell.run(app, "CLASSPATH=$apk app_process /system/bin ${BydClusterSongTool::class.java.name} $args")
            ?: return false
        val failed = output.lineSequence().map { it.trim() }.filter { it.contains('=') }
            .any { line -> line.substringAfter('=').trim().toIntOrNull() != 0 }
        if (failed) Log.w(TAG, "dashboard write failed: ${output.trim().take(160)}")
        return !failed
    }
}

/**
 * Runs under the head unit's adb shell through app_process, not in DiPlay: writes the dashboard's music
 * source, play state and song text to the instrument (device 1007), as BYD's media controller does
 * (source 0x33F00030, state 0x43E0000A, text 0x43FB1008 in UTF-16LE). Arguments: source, state and
 * base64 UTF-8 text, "-" to skip one. Prints "name=result" per write; 0 is success.
 */
object BydClusterSongTool {
    private const val DEVICE = 1007
    private const val SOURCE = 0x33F00030
    private const val STATE = 0x43E0000A
    private const val TEXT = 0x43FB1008

    @JvmStatic
    fun main(args: Array<String>) {
        try {
            write(args)
        } catch (error: Throwable) {
            println("write=ERR ${describe(error)}")
        } finally {
            // ActivityThread leaves threads behind; without this the shell command would not return.
            System.exit(0)
        }
    }

    @SuppressLint("PrivateApi")
    private fun write(args: Array<String>) {
        runCatching { android.os.Looper.prepareMainLooper() }
        val thread = Class.forName("android.app.ActivityThread")
        val main = thread.getMethod("systemMain").invoke(null)
        val context = thread.getMethod("getSystemContext").invoke(main)
        val deviceClass = Class.forName("android.hardware.bydauto.instrument.BYDAutoInstrumentDevice")
        // getInstance checks BYDAUTO_INSTRUMENT_COMMON on the caller's side only; autoservice itself
        // accepts the shell user, so build the device the way getInstance does.
        val device = try {
            deviceClass.getMethod("getInstance", Context::class.java).invoke(null, context)
        } catch (_: InvocationTargetException) {
            deviceClass.getDeclaredConstructor(Context::class.java).apply { isAccessible = true }.newInstance(context)
        }
        val setState = deviceClass.getMethod("setMediaState", Int::class.java, Int::class.java, Int::class.java)
        val setInfo = deviceClass.getMethod("setMediaInfo", Int::class.java, Int::class.java, ByteArray::class.java)
        args.getOrNull(0)?.takeIf { it != "-" }?.let { println("source=${setState.invoke(device, DEVICE, SOURCE, it.toInt())}") }
        args.getOrNull(1)?.takeIf { it != "-" }?.let { println("state=${setState.invoke(device, DEVICE, STATE, it.toInt())}") }
        args.getOrNull(2)?.takeIf { it != "-" }?.let { encoded ->
            val text = String(Base64.decode(encoded, Base64.DEFAULT), Charsets.UTF_8).toByteArray(Charsets.UTF_16LE)
            println("text=${if (text.size > ClusterSongState.MAX_TEXT_BYTES) "ERR too long" else setInfo.invoke(device, DEVICE, TEXT, text)}")
        }
    }

    private fun describe(error: Throwable): String {
        val cause = error.cause ?: error
        return cause.javaClass.name + (cause.message?.let { ": $it" } ?: "")
    }
}
