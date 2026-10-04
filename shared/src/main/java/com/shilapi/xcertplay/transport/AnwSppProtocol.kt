package com.shilapi.xcertplay.transport

import android.os.IBinder
import android.os.Parcel
import java.io.IOException
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/** H52.10500 wire contract. Worker-thread calls only; explicit initialization registers a local SPP service. */
internal class AnwSppProtocol(private val binder: IBinder) {
    data class ConnectRequest(val result: Int, val index: Int)
    data class DeviceState(val result: Int, val state: Int, val address: String?)

    fun power(): Int = transaction(3, {}) { it.readInt() }
    fun initialized(): Boolean = transaction(0x43, {}) {
        when (it.readInt()) { 0 -> false; 1 -> true; else -> throw IOException("Invalid ANW SPP state") }
    }
    fun initializeLocalService(uuid: ByteArray): Int {
        require(uuid.size == 16)
        return transaction(0x3e, { it.writeByteArray(uuid) }) { it.readInt() }
    }
    fun localAddress(): String = transaction(7, {}) { it.readString() ?: "" }
    fun pairedDevices(): List<AnwPairedDevice> = transaction(0x10, {
        for (capacity in listOf(1, 16, 16, 16)) it.writeInt(capacity)
    }) {
        if (it.readInt() != 1) throw IOException("ANW paired list unavailable")
        val count = IntArray(1)
        val names = arrayOfNulls<String>(16)
        val addresses = arrayOfNulls<String>(16)
        val classes = IntArray(16)
        it.readIntArray(count); it.readStringArray(names); it.readStringArray(addresses); it.readIntArray(classes)
        if (count[0] !in 0..16) throw IOException("Invalid ANW paired count")
        (0 until count[0]).map { i ->
            val address = addresses[i] ?: throw IOException("Missing ANW paired address")
            if (!address.matches(Regex("(?i)[0-9a-f]{2}(:[0-9a-f]{2}){5}"))) throw IOException("Invalid ANW paired address")
            AnwPairedDevice(address, names[i].orEmpty().ifBlank { "iPhone" })
        }
    }

    /** Native slot lookup; it may refresh the remote-name cache inside the vendor service. */
    fun deviceState(index: Int): DeviceState {
        require(index in 0..3)
        return transaction(0xd0, {
            it.writeInt(index)
            repeat(4) { _ -> it.writeInt(1) }
        }) {
            val result = it.readInt()
            val state = IntArray(1)
            val address = arrayOfNulls<String>(1)
            val name = arrayOfNulls<String>(1)
            it.readIntArray(state)
            it.readStringArray(address)
            it.readStringArray(name)
            // The remaining typed UUID array is unused; do not load the OEM Parcelable class.
            // Native result 1 and state 1 mean the queried slot is connected.
            DeviceState(result, state[0], address[0])
        }
    }

    fun requestConnection(address: String, uuid: ByteArray, onReply: (ConnectRequest) -> Unit = {}): ConnectRequest {
        require(address.matches(Regex("(?i)[0-9a-f]{2}(:[0-9a-f]{2}){5}")))
        require(uuid.size == 16) { "ANW JNI UUID buffer requires exactly 16 bytes" }
        return transaction(0x40, {
            it.writeString(address)
            it.writeByteArray(uuid)
            it.writeInt(1)
        }) {
            val result = it.readInt()
            val index = IntArray(1)
            it.readIntArray(index)
            // Result 1 accepts a request; connection must be confirmed independently.
            ConnectRequest(result, index[0]).also(onReply)
        }
    }

    fun write(index: Int, bytes: ByteArray): Int {
        require(index in 0..3)
        require(bytes.isNotEmpty() && bytes.size <= 8192)
        return transaction(0x42, {
            it.writeInt(index)
            it.writeByteArray(bytes)
            it.writeInt(bytes.size)
            it.writeInt(1)
        }) {
            val result = it.readInt()
            val count = IntArray(1)
            it.readIntArray(count)
            if (result != 1 || count[0] !in 1..bytes.size) {
                throw IOException("ANW SPP write rejected: result=$result written=${count[0]}")
            }
            count[0]
        }
    }

    fun register(callback: IBinder) = callbackTransaction(0x44, callback)
    fun unregister(callback: IBinder) = callbackTransaction(0x45, callback)

    fun disconnectOwnedIndex(index: Int): Int {
        require(index in 0..9)
        return transaction(0x41, { it.writeInt(index) }) { it.readInt() }
    }

    private fun callbackTransaction(code: Int, callback: IBinder) {
        transaction(code, { it.writeStrongBinder(callback) }) { Unit }
    }

    private fun <T> transaction(code: Int, request: (Parcel) -> Unit, response: (Parcel) -> T): T {
        val task = try { RPC.submit(Callable<T> {
        if (binder.interfaceDescriptor != DESCRIPTOR) throw IOException("Unexpected ANW Binder descriptor")
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        try {
            data.writeInterfaceToken(DESCRIPTOR)
            request(data)
            if (!binder.transact(code, data, reply, 0)) throw IOException("Unsupported ANW transaction $code")
            reply.readException()
            response(reply)
        } finally {
            reply.recycle()
            data.recycle()
        }
        }) } catch (busy: RejectedExecutionException) {
            throw IOException("H52 ANW Binder workers are busy", busy)
        }
        try { return task.get(3, TimeUnit.SECONDS) }
        catch (timeout: TimeoutException) {
            task.cancel(true)
            throw IOException("H52 ANW Binder response timed out", timeout)
        } catch (interrupted: InterruptedException) {
            task.cancel(true); Thread.currentThread().interrupt()
            throw IOException("H52 ANW Binder operation cancelled", interrupted)
        } catch (failure: ExecutionException) {
            val cause = failure.cause ?: failure
            if (cause is IOException) throw cause
            throw IOException("H52 ANW Binder operation failed", cause)
        }
    }

    companion object {
        // A timed-out remote Binder may stay blocked. Never replace it with unbounded threads.
        private val RPC = ThreadPoolExecutor(2, 2, 0L, TimeUnit.MILLISECONDS,
            SynchronousQueue<Runnable>(), { action -> Thread(action, "diplay-anw-binder").apply { isDaemon = true } })
        const val DESCRIPTOR = "com.anwsdk.service.IAnwPhoneLink"
        const val CALLBACK_DESCRIPTOR = "com.anwsdk.service.IAnwSPPDataCallBack"
    }
}
