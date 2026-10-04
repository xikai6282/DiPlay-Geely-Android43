package com.shilapi.xcertplay.transport

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import java.io.IOException
import java.nio.ByteBuffer
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

data class AnwPairedDevice(val address: String, val name: String)

/** Experimental H52.105 ANW transport. Never enables/disables the vehicle radio. */
class AnwBluetoothBackend internal constructor(context: Context, private val suppliedBinder: IBinder?) {
    constructor(context: Context) : this(context, null)
    private val context = context.applicationContext

    fun pairedDevices(): List<AnwPairedDevice> = Binding().use { binding ->
        AnwSppProtocol(binding.await()).pairedDevices()
    }

    fun localAddress(): String = Binding().use { binding ->
        val address = AnwSppProtocol(binding.await()).localAddress()
        if (!address.matches(Regex("(?i)[0-9a-f]{2}(:[0-9a-f]{2}){5}")) || address == "00:00:00:00:00:00") {
            throw IOException("H52 ANW local Bluetooth address unavailable")
        }
        address
    }

    fun connect(address: String, uuid: UUID, onTrace: (String) -> Unit = {}, cancelled: () -> Boolean = { false }): BlockingDuplexByteStream {
        val binding = Binding()
        val stopped = AtomicBoolean()
        val ownedIndex = java.util.concurrent.atomic.AtomicInteger(-1)
        var protocol: AnwSppProtocol? = null
        var callback: AnwSppDataCallback? = null
        var registered = false
        val early = Array(4) { mutableListOf<ByteArray>() }
        val earlyBytes = IntArray(4)
        val earlyOverflow = BooleanArray(4)
        val receiveLock = Any()
        var receiveReady = false
        lateinit var stream: CallbackDuplexByteStream
        fun releaseOwnedSlot() {
            val index = ownedIndex.getAndSet(-1)
            if (index in 4..9) runCatching { protocol?.disconnectOwnedIndex(index) }
            else if (index in 0..3) runCatching {
                val current = protocol?.deviceState(index)
                if (current?.result == 1 && current.address.equals(address, ignoreCase = true)) {
                    protocol?.disconnectOwnedIndex(index)
                }
            }
        }
        fun cleanup() {
            if (!stopped.compareAndSet(false, true)) return
            callback?.stop()
            // Binder callbacks must never synchronously call back into their remote sender.
            Thread({
                try {
                    if (registered) runCatching { protocol?.unregister(callback!!) }
                    releaseOwnedSlot()
                } finally { binding.close() }
            }, "diplay-anw-close").apply { isDaemon = true; start() }
        }
        stream = CallbackDuplexByteStream(
            writeBytes = { bytes, offset, length ->
                if (stopped.get()) throw IOException("H52 ANW transport closed")
                protocol!!.write(ownedIndex.get(), bytes.copyOfRange(offset, offset + length))
            },
            closeTransport = { cleanup() },
        )
        binding.onDisconnected = { stream.onFailure(IOException("H52 ANW service disconnected")) }
        try {
            val rpc = AnwSppProtocol(binding.await())
            protocol = rpc
            if (cancelled()) throw IOException("H52 ANW connection cancelled")
            if (rpc.power() != 1) throw IOException("H52 factory Bluetooth is not on")
            val wasInitialized = rpc.initialized()
            onTrace("H52 ANW SPP initializedFlag=$wasInitialized")
            val initPrefs = context.getSharedPreferences("geely_bt_transport", Context.MODE_PRIVATE)
            if (wasInitialized && initPrefs.getBoolean("init_uncertain", false)) {
                throw IOException("H52 ANW previous initialization failed or timed out; restart vehicle Bluetooth before retrying")
            }
            if (!wasInitialized) {
                if (!initPrefs.edit().putBoolean("init_uncertain", true).commit()) throw IOException("Cannot persist H52 SPP initialization state")
                // Local generic SPP service UUID, distinct from the remote iAP2 UUID.
                val result = rpc.initializeLocalService(uuidBytes(UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")))
                onTrace("H52 ANW local SPP initialization result=$result")
                if (result != 1) throw IOException("H52 ANW SPP initialization failed: $result")
                initPrefs.edit().putBoolean("init_uncertain", false).commit()
            }
            callback = AnwSppDataCallback(
                onBytes = { index, bytes -> synchronized(receiveLock) {
                    if (!stopped.get()) {
                        if (receiveReady && ownedIndex.get() == index) stream.onBytes(bytes)
                        else if (!receiveReady && index in 0..3) {
                            if (bytes.size > 65536 - earlyBytes[index]) earlyOverflow[index] = true
                            else { early[index].add(bytes); earlyBytes[index] += bytes.size }
                        }
                    }
                } },
                onFailure = { index, failure -> synchronized(receiveLock) {
                    if (ownedIndex.get() == index) stream.onFailure(failure)
                    else if (ownedIndex.get() == -1 && index in 0..3) earlyOverflow[index] = true
                } },
            )
            if (stopped.get() || cancelled()) throw IOException("H52 ANW connection cancelled")
            rpc.register(callback)
            registered = true
            if (stopped.get() || cancelled()) throw IOException("H52 ANW connection cancelled")
            val request = rpc.requestConnection(address, uuidBytes(uuid)) { reply ->
                if (reply.result == 1 && reply.index in 0..9) {
                    ownedIndex.set(reply.index)
                    // Even a reply arriving after the caller deadline owns a native slot.
                    if (stopped.get()) Thread({ releaseOwnedSlot() }, "diplay-anw-late-reply-close")
                        .apply { isDaemon = true; start() }
                }
            }
            onTrace("H52 ANW connect request result=${request.result} index=${request.index} uuidEncoding=canonical-big-endian peerInterpretation=unverified")
            if (request.result != 1 || request.index !in 0..3) {
                throw IOException("H52 ANW connect request rejected: result=${request.result} index=${request.index}")
            }
            synchronized(receiveLock) {
                if (earlyOverflow[request.index]) stream.onFailure(IOException("H52 ANW early data overflow or invalid callback"))
                else for (bytes in early[request.index]) stream.onBytes(bytes)
                for (queue in early) queue.clear()
                receiveReady = true
            }
            if (stopped.get()) throw IOException("H52 ANW connection cancelled")
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15)
            while (!stopped.get() && !cancelled() && System.nanoTime() < deadline) {
                val state = rpc.deviceState(ownedIndex.get())
                if (state.result == 1 && state.state == 1 && state.address.equals(address, ignoreCase = true)) {
                    onTrace("H52 ANW native SPP link confirmed index=${ownedIndex.get()} addressMatches=true")
                    if (stopped.get() || cancelled()) throw IOException("H52 ANW connection cancelled")
                    Thread({
                        while (!stopped.get()) {
                            try {
                                Thread.sleep(3000)
                                if (stopped.get()) break
                                if (cancelled()) throw IOException("H52 ANW connection cancelled")
                                val current = rpc.deviceState(ownedIndex.get())
                                if (current.result != 1 || current.state != 1 || !current.address.equals(address, ignoreCase = true)) {
                                    throw IOException("H52 ANW SPP link disconnected or replaced")
                                }
                            } catch (failure: Exception) {
                                if (!stopped.get()) stream.onFailure(failure)
                                break
                            }
                        }
                    }, "diplay-anw-link-state").apply { isDaemon = true; start() }
                    return stream
                }
                Thread.sleep(1000)
            }
            throw IOException("H52 ANW connection not confirmed before timeout or cancellation")
        } catch (error: Throwable) {
            cleanup()
            throw error
        }
    }

    private inner class Binding : java.io.Closeable, ServiceConnection {
        private val main = Handler(Looper.getMainLooper())
        private val ready = CountDownLatch(1)
        private val closed = AtomicBoolean()
        @Volatile private var bound = false
        @Volatile private var binder: IBinder? = null
        @Volatile private var error: Throwable? = null
        @Volatile var onDisconnected: (() -> Unit)? = null
        init {
            if (suppliedBinder != null) {
                binder = suppliedBinder
                ready.countDown()
            } else main.post {
                if (!closed.get()) {
                    try {
                        bound = context.bindService(Intent("com.anwsdk.service.AnwPhoneLink").setComponent(
                            ComponentName("com.anwsdk.service", "com.anwsdk.service.AnwSdkService")), this, 0)
                        if (!bound) { error = IOException("H52 ANW service is not running"); ready.countDown() }
                        if (closed.get() && bound) { bound = false; runCatching { context.unbindService(this) } }
                    } catch (failure: Exception) { error = failure; ready.countDown() }
                }
            }
        }
        fun await(): IBinder {
            check(Looper.myLooper() != Looper.getMainLooper()) { "ANW binding must run on a worker" }
            if (!ready.await(4, TimeUnit.SECONDS)) throw IOException("H52 ANW service bind timeout")
            error?.let { throw IOException("H52 ANW service unavailable", it) }
            return binder ?: throw IOException("H52 ANW binder unavailable")
        }
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            if (!closed.get()) binder = service
            ready.countDown()
        }
        override fun onServiceDisconnected(name: ComponentName) {
            binder = null; error = IOException("H52 ANW service disconnected"); ready.countDown()
            onDisconnected?.invoke()
        }
        override fun close() {
            if (!closed.compareAndSet(false, true)) return
            ready.countDown()
            main.post { if (bound) { bound = false; runCatching { context.unbindService(this) } } }
        }
    }

    companion object {
        internal fun uuidBytes(uuid: UUID): ByteArray = ByteBuffer.allocate(16)
            .putLong(uuid.mostSignificantBits).putLong(uuid.leastSignificantBits).array()
    }
}
