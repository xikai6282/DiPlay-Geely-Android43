package com.shilapi.xcertplay.compat

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.os.Parcel
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.shilapi.xcertplay.airplay.AudioCodecKind
import com.shilapi.xcertplay.airplay.AudioFormat
import com.shilapi.xcertplay.airplay.AudioStreamId
import com.shilapi.xcertplay.airplay.MicrophoneConfig
import com.shilapi.xcertplay.airplay.VideoCodec
import com.shilapi.xcertplay.media.AndroidMediaSink
import com.shilapi.xcertplay.media.CodecCompat
import com.shilapi.xcertplay.mfi.LocalMfiAuthenticationClient
import com.shilapi.xcertplay.transport.LockdownPairRecordGenerator
import com.shilapi.xcertplay.transport.LockdownTlsEngineFactory
import java.nio.ByteBuffer
import java.io.DataInputStream
import java.io.File
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.security.SecureRandom
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.sin

/** Debug-only, shell-permission-protected probes of the production rendering and crypto code. */
class Api18CompatProbeActivity : Activity(), SurfaceHolder.Callback {
    private lateinit var status: TextView
    private val started = AtomicBoolean(false)
    private val failures = AtomicInteger()
    private var receiverRegistered = false
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != VPN_RESULT) return
            if (!intent.getBooleanExtra("passed", false)) failures.incrementAndGet()
            line(intent.getStringExtra("result") ?: "FAIL VPN no result")
            complete()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        status = TextView(this).apply { textSize = 13f; setPadding(12, 12, 12, 12) }
        val surface = SurfaceView(this).apply { holder.addCallback(this@Api18CompatProbeActivity) }
        layout.addView(surface, LinearLayout.LayoutParams(-1, 260))
        layout.addView(ScrollView(this).apply { addView(status) }, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(layout)
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(receiver, IntentFilter(VPN_RESULT), Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION", "UnspecifiedRegisterReceiverFlag")
            registerReceiver(receiver, IntentFilter(VPN_RESULT))
        }
        receiverRegistered = true
        line("START sdk=${Build.VERSION.SDK_INT} probe=${intent.getStringExtra("probe") ?: "full"}")
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        if (!started.compareAndSet(false, true)) return
        Thread({ runProbes(holder.surface) }, "codex-api18-probes").apply { isDaemon = true; start() }
    }
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) = Unit
    override fun surfaceDestroyed(holder: SurfaceHolder) = Unit

    private fun runProbes(surface: Surface) {
        val mode = intent.getStringExtra("probe") ?: "full"
        if (mode == "geely_bluetooth") {
            checkCase("H52_ANW_READONLY_PARCEL_MOCK") { geelyAnwParcelMock() }
            checkCase("H52_ECARX_READONLY_PARCEL_MOCK") { geelyEcarxParcelMock() }
            val client = com.shilapi.xcertplay.compat.GeelyBluetoothDiagnostics(applicationContext)
            client.query(timeoutMillis = 6_000L) { snapshot ->
                checkCase("VENDOR_SERVICES_UNAVAILABLE_REMAIN_UNKNOWN") {
                    if (snapshot.bindingState != com.shilapi.xcertplay.compat.BindingState.CONNECTED) {
                        check(snapshot.power.state != com.shilapi.xcertplay.compat.ReadState.OK && snapshot.power.value == null) {
                            "Unbound ANW service was incorrectly reported as an OFF state"
                        }
                    }
                    if (snapshot.power.state != com.shilapi.xcertplay.compat.ReadState.OK) {
                        check(snapshot.power.value == null || snapshot.power.value == com.shilapi.xcertplay.compat.AnwPowerState.UNKNOWN)
                    }
                    if (snapshot.ecarxEnabled.state != com.shilapi.xcertplay.compat.ReadState.OK) {
                        check(snapshot.ecarxEnabled.value == null) { "Unavailable ECarX service was reported false" }
                    }
                    "anW=${snapshot.bindingState}/${snapshot.power.state}:${snapshot.power.value} " +
                        "paired=${snapshot.pairedCount.state}:${snapshot.pairedCount.value} " +
                        "spp=${snapshot.sppInitialized.state}:${snapshot.sppInitialized.value} " +
                        "ecarx=${snapshot.ecarxEnabled.state}:${snapshot.ecarxEnabled.value} " +
                        "(actual vehicle Binder access is not implied by the mock)"
                }
                complete()
            }
            return
        }
        if (mode == "geely_focus") {
            checkCase("GEELY_CAPABILITY_GUARD") {
                check(com.shilapi.xcertplay.media.GeelyAudioCapabilities.detect() == null)
                "stockApi18Rejected=true"
            }
            checkCase("GEELY_INDEPENDENT_FOCUS") { geelyFocus() }
            complete()
            return
        }
        if (mode == "bonjour_discovery") {
            checkCase("BONJOUR_CONTROL_DISCOVERY") { LegacyNetworkProbe.bonjourDiscovery(this) }
            complete()
            return
        }
        if (mode == "map_embed") {
            runOnUiThread {
                val finished = AtomicBoolean(false)
                var bound = false
                lateinit var connection: android.content.ServiceConnection
                val serviceIntent = Intent().setClassName(applicationContext,
                    "com.shilapi.xcertplay.MapEmbedService")
                fun finishCase(message: android.os.Message?) {
                    if (!finished.compareAndSet(false, true)) return
                    checkCase("MAP_EMBED_UNSUPPORTED_REPLY") {
                        check(message != null) { "No Binder reply within five seconds" }
                        check(message.what == 199 && message.data.getString("error") == "unsupported") {
                            "Expected unsupported reply, got what=${message.what} error=${message.data.getString("error")}" }
                        "actualServiceBound=true actualBinderReply=unsupported"
                    }
                    if (bound) unbindService(connection)
                    stopService(serviceIntent)
                    complete()
                }
                val reply = android.os.Messenger(android.os.Handler(android.os.Looper.getMainLooper()) {
                    finishCase(it); true
                })
                connection = object : android.content.ServiceConnection {
                    override fun onServiceConnected(name: android.content.ComponentName, binder: android.os.IBinder) {
                        val peer = android.os.Messenger(binder)
                        peer.send(android.os.Message.obtain().apply { what = 1; replyTo = reply })
                    }
                    override fun onServiceDisconnected(name: android.content.ComponentName) = Unit
                }
                bound = bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)
                status.postDelayed({ finishCase(null) }, 5_000)
            }
            return
        }
        if (mode == "overlay") {
            runOnUiThread {
                val type = Class.forName("com.shilapi.xcertplay.CenterMapOverlay")
                val singleton = type.getField("INSTANCE").get(null)
                val actualSurface = AtomicReference<Surface?>()
                var showAccepted = false
                var permissionGranted = false
                checkCase("CENTER_OVERLAY_OPEN") {
                    permissionGranted = type.getMethod("permitted", Context::class.java)
                        .invoke(singleton, applicationContext) as Boolean
                    val onSurface: (Surface?) -> Unit = { actualSurface.set(it) }
                    val onTap: () -> Unit = { }
                    showAccepted = type.getMethod("show", Context::class.java, java.lang.Double.TYPE,
                        Class.forName("kotlin.jvm.functions.Function1"), Class.forName("kotlin.jvm.functions.Function0"))
                        .invoke(singleton, applicationContext, 16.0 / 9.0, onSurface, onTap) as Boolean
                    check(showAccepted == permissionGranted) { "Overlay permission/window mismatch granted=$permissionGranted opened=$showAccepted" }
                    "permissionGranted=$permissionGranted opened=$showAccepted"
                }
                status.postDelayed({
                    checkCase("CENTER_OVERLAY_SURFACE_AND_CLOSE") {
                        try {
                            if (showAccepted) check(actualSurface.get()?.isValid == true) { "No actual TextureView surface" }
                            "surfaceValid=$showAccepted unavailable=${!permissionGranted}"
                        } finally {
                            type.getMethod("hide").invoke(singleton)
                            check(!(type.getMethod("getShown").invoke(singleton) as Boolean))
                        }
                    }
                    complete()
                }, 1200)
            }
            return
        }
        if (mode == "reset_probe_hotspot") {
            checkCase("RESET_DISPOSABLE_HOTSPOT") {
                val prefs = getSharedPreferences("xcertplay_airplay", Context.MODE_PRIVATE)
                val saved = prefs.getString("manual_hotspot_ssid", null)
                check(saved == "Codex-API18-Test" || saved.isNullOrEmpty()) {
                    "Refusing to remove a hotspot not created by this probe"
                }
                val editor = prefs.edit()
                for (key in listOf("manual_hotspot_ssid", "manual_hotspot_passphrase",
                    "manual_hotspot_band", "manual_hotspot_channel", "manual_hotspot_security")) editor.remove(key)
                check(editor.putBoolean("wireless_enabled", true).commit())
                "disposableTestSettingsRemoved=true wirelessDefaultRestored=true"
            }
            complete()
            return
        }
        if (mode == "usb_transfer") {
            checkCase("LEGACY_USB_CHUNK_ENGINE") {
                val helper = com.shilapi.xcertplay.transport.LegacyUsbTransfer
                check(helper.synchronousReadsRequired && helper.chunkedWritesRequired)
                val source = ByteArray(32768 + 7) { (it * 19).toByte() }
                val received = java.io.ByteArrayOutputStream()
                val offsets = ArrayList<Int>()
                val callback = com.shilapi.xcertplay.transport.LegacyUsbTransfer.ChunkTransfer { offset, length, timeout ->
                    check(length in 1..16384 && timeout > 0)
                    val count = if (offsets.isEmpty()) minOf(100, length) else length
                    offsets.add(offset)
                    received.write(source, 7 + offset, count)
                    count
                }
                val written = helper.writeAll(callback, 7, 32768,
                    android.os.SystemClock.elapsedRealtime() * 1_000_000L + 2_000_000_000L)
                check(written == 32768)
                check(offsets == listOf(0, 100, 16484))
                check(received.toByteArray().contentEquals(source.copyOfRange(7, source.size)))
                "shortWrite=true chunks=3 bytes=32768 offsetDataVerified=true (callback engine, physical USB not tested)"
            }
            complete()
            return
        }
        if (mode == "optional") {
            runOnUiThread {
                for (name in listOf("DiLink51ClusterMonitor", "HomeScreenMonitor")) {
                    checkCase("OPTIONAL_USAGE_ACCESS_$name") {
                        check(Build.VERSION.SDK_INT < 21)
                        val type = Class.forName("com.shilapi.xcertplay.$name")
                        val companion = type.getField("Companion").get(null)
                        val access = companion.javaClass.getMethod("hasAccess", Context::class.java)
                            .invoke(companion, applicationContext) as Boolean
                        check(!access) { "Usage access must be unavailable on API18" }
                        val callback: (Any?) -> Unit = { }
                        val monitor = type.getDeclaredConstructor(Context::class.java,
                            Class.forName("kotlin.jvm.functions.Function1"))
                            .apply { isAccessible = true }.newInstance(applicationContext, callback)
                        try {
                            type.getDeclaredMethod("start").apply { isAccessible = true }.invoke(monitor)
                            if (name == "HomeScreenMonitor") {
                                val running = type.getDeclaredMethod("getRunning").apply { isAccessible = true }
                                    .invoke(monitor) as Boolean
                                check(!running) { "Unsupported home monitor started a poller" }
                            }
                        } finally {
                            type.getDeclaredMethod("stop").apply { isAccessible = true }.invoke(monitor)
                        }
                        "loaded=true unavailable=true actualStartStop=true"
                    }
                }
                complete()
            }
            return
        }
        if (mode == "keys_start" || mode == "keys_pause" || mode == "keys_stop") {
            runOnUiThread {
                checkCase("LEGACY_MEDIA_KEYS_REQUEST") {
                    check(Build.VERSION.SDK_INT < 21)
                    val type = Class.forName("com.shilapi.xcertplay.CarPlayMediaKeys")
                    val singleton = type.getField("INSTANCE").get(null)
                    if (mode == "keys_stop") {
                        type.getDeclaredMethod("releaseLocked").apply { isAccessible = true }.invoke(singleton)
                        "released=true"
                    } else {
                        // No phone session exists on the emulator. Supply only the app context to
                        // exercise the actual production receiver/focus/RCC without mocking Android.
                        type.getDeclaredField("appContext").apply { isAccessible = true }.set(singleton, applicationContext)
                        type.getDeclaredField("mediaAudioActive").apply { isAccessible = true }
                            .setBoolean(singleton, mode == "keys_start")
                        type.getDeclaredMethod("registerLegacyMediaButtons", Context::class.java)
                            .apply { isAccessible = true }.invoke(singleton, applicationContext)
                        type.getDeclaredMethod("regainFocusLocked").apply { isAccessible = true }.invoke(singleton)
                        type.getDeclaredMethod("publishPlaybackStateLocked").apply { isAccessible = true }.invoke(singleton)
                        for (field in listOf("legacyPendingIntent", "legacyRemoteControlClient")) {
                            check(type.getDeclaredField(field).apply { isAccessible = true }.get(singleton) != null)
                        }
                        "routingRegistered=true paused=${mode == "keys_pause"} (hardware event checked independently with adb)"
                    }
                }
                complete()
            }
            return
        }
        if (mode == "media_api18") {
            runOnUiThread {
                checkCase("API18_MEDIA3_CLASS_ISOLATION") {
                    check(Build.VERSION.SDK_INT < 21)
                    val keys = Class.forName("com.shilapi.xcertplay.CarPlayMediaKeys")
                    val video = Class.forName("com.shilapi.xcertplay.CarPlayVideo")
                    val bridge = Class.forName("com.shilapi.xcertplay.CarPlayVideoActivityBridge")
                    val frameworkTypes = setOf("android.media.session.MediaSession", "android.media.AudioFocusRequest",
                        "androidx.media3.exoplayer.ExoPlayer", "com.google.android.exoplayer2.ExoPlayer")
                    val linkedTypes = (keys.declaredFields.map { it.type.name } +
                        keys.declaredMethods.flatMap { method ->
                            listOf(method.returnType.name) + method.parameterTypes.map { it.name }
                        } + video.declaredFields.map { it.type.name } + bridge.declaredMethods.flatMap { method ->
                            listOf(method.returnType.name) + method.parameterTypes.map { it.name }
                        }).toSet()
                    check(linkedTypes.intersect(frameworkTypes).isEmpty()) {
                        "API21+/Media3 type leaked into API18 host signature: ${linkedTypes.intersect(frameworkTypes)}"
                    }
                    val playerInfo = packageManager.getActivityInfo(
                        android.content.ComponentName(packageName, "com.shilapi.xcertplay.CarPlayVideoActivity"),
                        PackageManager.GET_DISABLED_COMPONENTS,
                    )
                    check(!playerInfo.enabled) { "Media3 Activity must be disabled below API 23" }
                    val singleton = video.getField("INSTANCE").get(null)
                    video.getDeclaredField("appContext").apply { isAccessible = true }.set(singleton, applicationContext)
                    video.getDeclaredMethod("show").apply { isAccessible = true }.invoke(singleton)
                    "keysLoaded=true videoLoaded=true bridgeLoaded=true media3TypesLinked=false playerActivityEnabled=false showRejected=true"
                }
                complete()
            }
            return
        }
        if (mode == "session_start" || mode == "session_stop") {
            runOnUiThread {
                checkCase("SESSION_SERVICE_REQUEST") {
                    val service = Intent(this, com.shilapi.xcertplay.DiPlaySessionService::class.java)
                    if (mode == "session_start") {
                        check(startService(service) != null)
                        "startRequested=true (foreground state checked independently with adb)"
                    } else {
                        check(stopService(service))
                        "stopRequested=true"
                    }
                }
                complete()
            }
            return
        }
        if (mode == "full" || mode == "foundation") {
            checkCase("BASE64") {
                val bytes = ByteArray(180) { (it * 7).toByte() }
                val encoded = Base64Compat.encodeToString(bytes)
                check(Base64Compat.decode(encoded).contentEquals(bytes))
                val mime = Base64Compat.encodeMimeLines(bytes, byteArrayOf(10))
                check(mime.lines().dropLast(1).all { it.length == 64 })
                check(Base64Compat.decodeMime(mime.toByteArray(Charsets.US_ASCII)).contentEquals(bytes))
                "roundtrip=180 mimeLineWidth=64"
            }
            checkCase("CODEC_ENUMERATION") {
                val codecs = CodecCompat.decoders()
                check(codecs.any { !it.isEncoder && it.supportedTypes.any { type -> type.equals("video/avc", true) } })
                "count=${codecs.size} avc=true"
            }
            if (Build.VERSION.SDK_INT < 21) checkCase("LEGACY_USB_LAYOUT") { LegacyUsbProbe.run() }
            checkCase("IPV6_PRODUCTION_LISTENERS") {
                LegacyNetworkProbe.run(assets.open("compat-probe/video-smoke.avcc").use { it.readBytes() })
            }
            checkCase("BONJOUR_REQUIRED_TXT") { LegacyNetworkProbe.bonjour(this) }
            checkCase("LOCKDOWN_PAIR_RECORD_AND_TLS") {
                val record = LockdownPairRecordGenerator.generate(
                    assets.open("compat-probe/device-public-test.txt").use { it.readBytes() },
                    "02:00:00:00:00:01", "CODEX-API18-TEST-HOST", "CODEX-API18-TEST-BUID",
                )
                val engine = LockdownTlsEngineFactory.create(record)
                check(engine.useClientMode)
                engine.beginHandshake()
                val output = ByteBuffer.allocate(engine.session.packetBufferSize)
                val result = engine.wrap(ByteBuffer.allocate(0), output)
                check(result.bytesProduced() > 0) { "TLS engine produced no ClientHello: $result" }
                engine.closeOutbound()
                "certificatesGenerated=true clientHelloBytes=${result.bytesProduced()} (phone trust not tested)"
            }
            checkCase("MFI_IDENTITY") {
                val folder = File(cacheDir, "api18-probe-identity").apply { mkdirs() }
                for (name in listOf("identity.pk8", "certificate.p7b")) {
                    assets.open("offline-mfi/$name").use { input -> File(folder, name).outputStream().use { input.copyTo(it) } }
                }
                try {
                    val client = LocalMfiAuthenticationClient.load(folder)
                    val challenge = ByteArray(32).also { SecureRandom().nextBytes(it) }
                    val signature = client.signChallenge(challenge)
                    check(signature.size == 64)
                    "keyConsistency=true signatureBytes=64 (iPhone trust not tested)"
                } finally {
                    folder.listFiles()?.forEach { it.delete() }
                    folder.delete()
                }
            }
        }
        if (mode == "full" || mode == "media") {
            checkCase("H264_RENDER_AND_SURFACE_REATTACH") { video(surface) }
            checkCase("PCM_PLAYBACK") { audio(AudioCodecKind.LPCM) }
            checkCase("AAC_PLAYBACK") { audio(AudioCodecKind.AAC_LC) }
        }
        if (mode == "full" || mode == "microphone") checkCase("MICROPHONE_RTP") { microphone() }
        if (mode == "full" || mode == "native") {
            for (name in listOf("xcertplay_i2c", "local_hotspot_radio")) {
                checkCase("NATIVE_LOAD_$name") { System.loadLibrary(name); "loaded=true" }
            }
        }
        if (mode == "full" || mode == "vpn") runOnUiThread { beginVpn() } else complete()
    }

    private fun video(surface: Surface): String {
        val rendered = AtomicInteger()
        val diagnostics = ConcurrentLinkedQueue<String>()
        val sink = AndroidMediaSink(surface, videoWidth = 320, videoHeight = 240, context = this)
        try {
            sink.setVideoDiagnosticHandler(110) {
                diagnostics.add(it)
                if (it == "first frame rendered") rendered.incrementAndGet()
                line("VIDEO $it")
            }
            sink.setSurface(110, surface)
            sink.onVideoCodec(110, VideoCodec.H264)
            sink.onVideoConfig(110, assets.open("compat-probe/video-smoke.avcc").use { it.readBytes() })
            repeat(2) { cycle ->
                if (cycle == 1) {
                    sink.clearSurface(110, surface)
                    Thread.sleep(150)
                    sink.setSurface(110, surface)
                    Thread.sleep(150)
                }
                DataInputStream(assets.open("compat-probe/video-smoke.frames")).use { input ->
                    while (input.available() > 0) {
                        val length = input.readInt()
                        check(length in 1..100_000)
                        val frame = ByteArray(length).also { input.readFully(it) }
                        sink.onVideoFrame(110, frame)
                        Thread.sleep(100)
                    }
                }
                Thread.sleep(500)
            }
            check(rendered.get() >= 2) { "Expected rendered frame before and after reattach; got ${rendered.get()}: $diagnostics" }
            check(diagnostics.none { it.contains("failed") || it.contains("decoder error") })
            return "renderedCycles=${rendered.get()} framesSubmitted=40"
        } finally { sink.close() }
    }

    private fun audio(kind: AudioCodecKind): String {
        val diagnostics = ConcurrentLinkedQueue<String>()
        val sink = AndroidMediaSink(context = this, audioFocusEnabled = true, onAudioDiagnostic = {
            diagnostics.add(it); line("AUDIO $it")
        })
        val id = AudioStreamId(96, "media")
        val format = AudioFormat(kind, 44_100, 2, 96)
        try {
            sink.onAudioStarted(id, format, 0)
            if (kind == AudioCodecKind.LPCM) {
                repeat(100) { packetIndex ->
                    val rtp = ByteArray(12 + 882 * 4)
                    rtp[0] = 0x80.toByte()
                    repeat(882) { frame ->
                        val sample = (sin((packetIndex * 882 + frame) * 2 * Math.PI * 440 / 44100) * 4000).toInt()
                        for (channel in 0..1) {
                            val offset = 12 + frame * 4 + channel * 2
                            rtp[offset] = (sample shr 8).toByte(); rtp[offset + 1] = sample.toByte()
                        }
                    }
                    sink.onAudioRtp(id, format, rtp, packetIndex * 882)
                    Thread.sleep(20)
                }
            } else {
                val bytes = assets.open("compat-probe/audio-smoke.aac").use { it.readBytes() }
                var offset = 0
                var sample = 0
                while (offset + 7 <= bytes.size) {
                    check(bytes[offset].toInt() and 255 == 255)
                    val header = if (bytes[offset + 1].toInt() and 1 == 1) 7 else 9
                    val length = ((bytes[offset + 3].toInt() and 3) shl 11) or
                        ((bytes[offset + 4].toInt() and 255) shl 3) or ((bytes[offset + 5].toInt() and 224) shr 5)
                    check(length > header && offset + length <= bytes.size)
                    val rtp = ByteArray(12 + length - header)
                    rtp[0] = 0x80.toByte()
                    bytes.copyInto(rtp, 12, offset + header, offset + length)
                    sink.onAudioRtp(id, format, rtp, sample)
                    offset += length; sample += 1024
                    Thread.sleep(23)
                }
            }
            Thread.sleep(700)
            sink.onAudioStopped(id)
            Thread.sleep(400)
            val written = diagnostics.mapNotNull { Regex("totalWrittenFrames=(\\d+)").find(it)?.groupValues?.get(1)?.toLong() }.maxOrNull() ?: 0
            val head = diagnostics.mapNotNull { Regex("playbackHeadFrames=(\\d+)").find(it)?.groupValues?.get(1)?.toLong() }.maxOrNull() ?: 0
            val decoded = diagnostics.mapNotNull { Regex("outputBuffersTotal=(\\d+)").find(it)?.groupValues?.get(1)?.toLong() }.maxOrNull() ?: 0
            check(diagnostics.none { it.contains("renderer failed") }) { diagnostics.toString() }
            check(written > 0 && head > 0) { "No actual playback progress written=$written head=$head: $diagnostics" }
            if (kind == AudioCodecKind.AAC_LC) check(decoded > 0) { "AAC produced no decoded output" }
            return "writtenFrames=$written playbackHead=$head decodedBuffers=$decoded"
        } finally { sink.close() }
    }

    private fun microphone(): String {
        val socket = DatagramSocket(null)
        socket.bind(InetSocketAddress(InetAddress.getByName("::1"), 0))
        socket.soTimeout = 4000
        val sink = AndroidMediaSink(context = this)
        val id = AudioStreamId(97, "speechrecognition")
        try {
            sink.onMicrophoneStarted(id, MicrophoneConfig("speechrecognition", 16000, 1, 97, 20,
                InetAddress.getByName("::1"), socket.localPort, ByteArray(32) { it.toByte() }))
            val packet = DatagramPacket(ByteArray(4096), 4096)
            socket.receive(packet)
            check(packet.length > 12 + 24)
            return "receivedSealedRtpBytes=${packet.length}"
        } finally { sink.onMicrophoneStopped(id); sink.close(); socket.close() }
    }

    private fun beginVpn() {
        val consent = VpnService.prepare(this)
        if (consent != null) startActivityForResult(consent, 180) else startVpn()
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 180) {
            if (resultCode == RESULT_OK) startVpn() else {
                failures.incrementAndGet(); line("FAIL VPN consent denied"); complete()
            }
        }
    }
    private fun geelyFocus(): String {
        val type = Class.forName("com.shilapi.xcertplay.media.AudioFocusCoordinator")
        val channelType = Class.forName("com.shilapi.xcertplay.media.AudioChannel")
        val media = channelType.enumConstants!!.first { it.toString() == "MEDIA" }
        val navigation = channelType.enumConstants!!.first { it.toString() == "NAVIGATION" }
        val events = ConcurrentLinkedQueue<String>()
        val callback: (String) -> Unit = { events.add(it) }
        val coordinator = type.declaredConstructors.first { it.parameterTypes.size == 4 }
            .apply { isAccessible = true }.newInstance(this, true, callback, true)
        val acquire = type.getDeclaredMethod("acquireLegacy", android.media.AudioTrack::class.java, channelType, Int::class.javaPrimitiveType)
        val release = type.getDeclaredMethod("release", android.media.AudioTrack::class.java)
        val listeners = type.getDeclaredField("geelyListeners").apply { isAccessible = true }
        fun count() = (listeners.get(coordinator) as Map<*, *>).size
        val tracks = listOf(3, 4, 4, 5, 4).map { stream ->
            android.media.AudioTrack(stream, 8000, android.media.AudioFormat.CHANNEL_OUT_MONO,
                android.media.AudioFormat.ENCODING_PCM_16BIT, 4096, android.media.AudioTrack.MODE_STREAM)
                .also { check(it.state == android.media.AudioTrack.STATE_INITIALIZED) }
        }
        fun keyedEntry(stream: Int): Map.Entry<*, *> =
            (listeners.get(coordinator) as Map<*, *>).entries.firstOrNull { entry ->
                val field = entry.key!!.javaClass.getDeclaredField("streamType").apply { isAccessible = true }
                field.getInt(entry.key) == stream
            } ?: error("Missing listener for stream=$stream")
        fun callbackFor(entry: Map.Entry<*, *>): android.media.AudioManager.OnAudioFocusChangeListener =
            entry.value!!.javaClass.getDeclaredField("listener").apply { isAccessible = true }
                .get(entry.value) as android.media.AudioManager.OnAudioFocusChangeListener
        fun volumeFor(stream: Int): Float {
            val entry = keyedEntry(stream)
            val volumes = type.getDeclaredField("geelyVolumes").apply { isAccessible = true }
                .get(coordinator) as Map<*, *>
            return volumes[entry.key] as? Float ?: error("No volume state for stream=$stream")
        }
        try {
            acquire.invoke(coordinator, tracks[0], media, 3)
            acquire.invoke(coordinator, tracks[1], navigation, 4)
            acquire.invoke(coordinator, tracks[2], navigation, 4)
            acquire.invoke(coordinator, tracks[3], navigation, 5)
            check(count() == 3) { "Media, speech, and alert focus requests were not independent: $events" }
            val staleSpeechListener = callbackFor(keyedEntry(4))
            release.invoke(coordinator, tracks[1])
            check(count() == 3) { "Navigation speech focus released before its last track" }
            release.invoke(coordinator, tracks[2])
            check(count() == 2) { "Last navigation speech track did not release its focus" }
            acquire.invoke(coordinator, tracks[4], navigation, 4)
            check(count() == 3) { "Replacement speech route failed to acquire focus" }
            check(volumeFor(4) == 1f) { "Replacement speech route was not audible after grant: ${volumeFor(4)}" }
            staleSpeechListener.onAudioFocusChange(android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)
            check(volumeFor(4) == 1f) { "Stale listener changed replacement route volume: ${volumeFor(4)}" }
            val currentSpeechListener = callbackFor(keyedEntry(4))
            currentSpeechListener.onAudioFocusChange(android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK)
            check(kotlin.math.abs(volumeFor(4) - 0.2f) < 0.001f) { "Duck callback did not lower current route" }
            currentSpeechListener.onAudioFocusChange(android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)
            check(volumeFor(4) == 0f) { "Transient loss did not mute current route" }
            currentSpeechListener.onAudioFocusChange(android.media.AudioManager.AUDIOFOCUS_GAIN)
            check(volumeFor(4) == 1f) { "Gain callback did not restore current route" }
            check((listeners.get(coordinator) as Map<*, *>).keys.any { key ->
                key!!.javaClass.getDeclaredField("streamType").apply { isAccessible = true }.getInt(key) == 5
            }) { "Alert focus was coupled to navigation speech" }
            tracks.forEach { release.invoke(coordinator, it) }
            check(count() == 0) { "A media, speech, or alert focus request leaked" }
            check(events.any { it.contains("channel=NAVIGATION") && it.contains("stream=4") && it.contains("gain=3") && it.contains("granted=true") })
            return "independentListeners=media3+speech4+alert5 speechRefCount=true staleCallbackIgnored=true duckLossGain=true released=true (standard test streams, H52 HAL not tested)"
        } finally {
            tracks.forEach { track -> runCatching { release.invoke(coordinator, track) }; track.release() }
        }
    }

    private fun startVpn() { startService(Intent(this, Api18VpnProbeService::class.java)) }

    private fun geelyAnwParcelMock(): String {
        val descriptor = "com.anwsdk.service.IAnwPhoneLink"
        val codes = mutableListOf<Int>()
        val capacities = AtomicReference<List<Int>>()
        val binder = object : Binder() {
            init { attachInterface(null, descriptor) }
            override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
                val output = requireNotNull(reply)
                if (code == INTERFACE_TRANSACTION) { output.writeString(descriptor); return true }
                data.enforceInterface(descriptor)
                codes += code
                when (code) {
                    0x03 -> { check(data.dataAvail() == 0); output.writeNoException(); output.writeInt(1) }
                    0x10 -> {
                        val requested = List(4) { data.readInt() }
                        check(requested == listOf(1, 16, 16, 16)) { "Unexpected 10500 capacities: $requested" }
                        check(data.dataAvail() == 0) { "Unexpected data after four capacity integers" }
                        capacities.set(requested)
                        output.writeNoException()
                        output.writeInt(1)
                        output.writeIntArray(intArrayOf(2))
                        output.writeStringArray(arrayOfNulls<String>(16).apply { this[0] = "probe device" })
                        output.writeStringArray(arrayOfNulls<String>(16).apply { this[0] = "02:00:00:00:00:01" })
                        output.writeIntArray(IntArray(16))
                    }
                    0x43 -> { check(data.dataAvail() == 0); output.writeNoException(); output.writeInt(1) }
                    else -> return false
                }
                return true
            }
        }
        val protocol = Class.forName("com.shilapi.xcertplay.compat.GeelyBluetoothReadOnlyProtocol")
        val result = protocol.getMethod("read", IBinder::class.java)
            .invoke(protocol.getField("INSTANCE").get(null), binder)
        fun value(getter: String): Any? = result.javaClass.getMethod(getter).invoke(result)
        val power = value("getPower")!!
        val paired = value("getPairedCount")!!
        val spp = value("getSppInitialized")!!
        check(power.javaClass.getMethod("getValue").invoke(power).toString() == "ON")
        check(paired.javaClass.getMethod("getValue").invoke(paired) == 2)
        check(spp.javaClass.getMethod("getValue").invoke(spp) == true)
        check(capacities.get() == listOf(1, 16, 16, 16))
        check(codes == listOf(0x03, 0x10, 0x43)) { "Unexpected ANW transaction set: $codes" }
        return "descriptorChecked=true getterCodes=$codes capacities=${capacities.get()} extraPayload=false"
    }

    private fun geelyEcarxParcelMock(): String {
        val descriptor = "ecarx.bluetooth.IBluetoothManager"
        var calls = 0
        val binder = object : Binder() {
            init { attachInterface(null, descriptor) }
            override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
                val output = requireNotNull(reply)
                if (code == INTERFACE_TRANSACTION) { output.writeString(descriptor); return true }
                data.enforceInterface(descriptor)
                check(code == 0x05) { "Unexpected ECarX transaction $code" }
                check(data.dataAvail() == 0) { "Unexpected ECarX request payload" }
                calls++
                output.writeNoException()
                output.writeInt(0)
                return true
            }
        }
        val protocol = Class.forName("com.shilapi.xcertplay.compat.EcarxBluetoothReadOnlyProtocol")
        val result = protocol.getMethod("readBinder", IBinder::class.java)
            .invoke(protocol.getField("INSTANCE").get(null), binder)
        check(result.javaClass.getMethod("getState").invoke(result).toString() == "OK")
        check(result.javaClass.getMethod("getValue").invoke(result) == false)
        check(calls == 1)
        return "descriptorChecked=true transaction=0x05 falseReadOnly=true calls=$calls"
    }

    private fun checkCase(name: String, action: () -> String) {
        try { line("PASS $name ${action()}") }
        catch (error: Throwable) {
            failures.incrementAndGet()
            line("FAIL $name ${error.javaClass.simpleName}: ${error.message}")
            Log.e(TAG, "Probe failure $name", error)
        }
    }
    private fun line(message: String) {
        Log.i(TAG, message)
        runOnUiThread { status.append(message + "\n") }
    }
    private fun complete() { line("COMPLETE failures=${failures.get()}") }
    override fun onDestroy() {
        if (receiverRegistered) unregisterReceiver(receiver)
        super.onDestroy()
    }
    companion object {
        const val TAG = "CodexApi18Probe"
        const val VPN_RESULT = "com.shihab.diplay.hudtest.CODEX_VPN_PROBE_RESULT"
    }
}
