package com.shilapi.xcertplay.compat

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Parcel
import android.os.RemoteException
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.Executor
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/** Read-only diagnostics for the H52 ANW phone-link Binder. This never changes Bluetooth state. */
class GeelyBluetoothDiagnostics(context: Context) {
    private val appContext = context.applicationContext ?: context
    private val inFlight = AtomicBoolean(false)
    private var binding: ServiceBinding = AndroidServiceBinding
    private var worker: Executor = sharedWorker
    private var mainHandler: Handler = Handler(Looper.getMainLooper())
    private var ecarxReader: () -> ReadValue<Boolean> = { EcarxBluetoothReadOnlyProtocol.read() }

    internal constructor(
        context: Context,
        binding: ServiceBinding,
        worker: Executor,
        mainHandler: Handler,
        ecarxReader: () -> ReadValue<Boolean>,
    ) : this(context) {
        this.binding = binding
        this.worker = worker
        this.mainHandler = mainHandler
        this.ecarxReader = ecarxReader
    }

    fun query(timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS, callback: (GeelyBluetoothSnapshot) -> Unit): Request {
        val pending = Pending(callback)
        if (!inFlight.compareAndSet(false, true)) {
            mainHandler.post { pending.deliver(GeelyBluetoothSnapshot.unavailable(BindingState.BUSY)) }
            return Request {
                pending.cancelled.set(true)
                pending.finished.set(true)
                pending.callback.set(null)
            }
        }

        mainHandler.post {
            if (pending.finished.get() || pending.cancelled.get()) {
                pending.finished.set(true)
                pending.callback.set(null)
                release(pending)
                return@post
            }
            val timeout = Runnable { finish(pending, pending.timeoutSnapshot) }
            pending.timeout = timeout
            mainHandler.postDelayed(timeout, timeoutMillis.coerceIn(1L, MAX_TIMEOUT_MILLIS))
            try {
                pending.bound = binding.bind(appContext, serviceIntent(), pending.connection)
                if (!pending.bound) startEcarxOnly(pending, BindingState.NOT_RUNNING)
            } catch (_: SecurityException) {
                startEcarxOnly(pending, BindingState.ACCESS_DENIED)
            } catch (_: RuntimeException) {
                startEcarxOnly(pending, BindingState.BIND_FAILED)
            }
        }

        return Request {
            pending.cancelled.set(true)
            pending.callback.set(null)
            mainHandler.post {
                finish(pending, GeelyBluetoothSnapshot.unavailable(BindingState.CANCELLED), notify = false)
            }
        }
    }

    private fun serviceIntent() = Intent().setComponent(SERVICE_COMPONENT)

    private fun startRead(pending: Pending, binder: IBinder) {
        pending.timeoutSnapshot = GeelyBluetoothSnapshot.unavailable(BindingState.READ_TIMEOUT)
        startReadTask(pending) {
            GeelyBluetoothReadOnlyProtocol.read(binder).copy(
                ecarxEnabled = ecarxReader(),
            )
        }
    }

    private fun startEcarxOnly(pending: Pending, bindingState: BindingState) {
        pending.timeoutSnapshot = GeelyBluetoothSnapshot.unavailable(bindingState).copy(
            ecarxEnabled = ReadValue(ReadState.FAILED, failure = ReadFailure.TIMEOUT),
        )
        startReadTask(pending) {
            GeelyBluetoothSnapshot.unavailable(bindingState).copy(
                ecarxEnabled = ecarxReader(),
            )
        }
    }

    private fun startReadTask(pending: Pending, read: () -> GeelyBluetoothSnapshot) {
        if (pending.finished.get() || pending.cancelled.get() || pending.workerStarted) return
        pending.workerStarted = true
        try {
            worker.execute {
                if (pending.finished.get() || pending.cancelled.get()) {
                    mainHandler.post {
                        finish(pending, GeelyBluetoothSnapshot.unavailable(BindingState.CANCELLED), notify = false)
                        release(pending)
                    }
                    return@execute
                }
                val result = try {
                    read()
                } catch (_: SecurityException) {
                    GeelyBluetoothSnapshot.unavailable(BindingState.ACCESS_DENIED)
                } catch (_: RemoteException) {
                    GeelyBluetoothSnapshot.unavailable(BindingState.BINDER_FAILED)
                } catch (_: RuntimeException) {
                    GeelyBluetoothSnapshot.unavailable(BindingState.BINDER_FAILED)
                }
                mainHandler.post {
                    val completed = if (pending.disconnected) {
                        result.copy(
                            bindingState = BindingState.DISCONNECTED,
                            power = ReadValue(ReadState.FAILED, failure = ReadFailure.REMOTE_FAILURE),
                            pairedCount = ReadValue(ReadState.FAILED, failure = ReadFailure.REMOTE_FAILURE),
                            sppInitialized = ReadValue(ReadState.FAILED, failure = ReadFailure.REMOTE_FAILURE),
                        )
                    } else result
                    finish(pending, completed)
                    release(pending)
                }
            }
        } catch (_: RuntimeException) {
            pending.workerStarted = false
            finish(pending, GeelyBluetoothSnapshot.unavailable(BindingState.BUSY))
            release(pending)
        }
    }

    private fun finish(pending: Pending, snapshot: GeelyBluetoothSnapshot, notify: Boolean = true) {
        if (!pending.finished.compareAndSet(false, true)) return
        pending.timeout?.let(mainHandler::removeCallbacks)
        pending.timeout = null
        if (pending.bound) {
            pending.bound = false
            try {
                binding.unbind(appContext, pending.connection)
            } catch (_: IllegalArgumentException) {
                // A platform disconnect can race timeout/cancellation; the binding is already gone.
            } catch (_: RuntimeException) {
                // Unbinding is best-effort and must not hide the diagnostic result.
            }
        }
        if (notify) pending.deliver(snapshot) else pending.callback.set(null)
        if (!pending.workerStarted) release(pending)
    }

    private fun release(pending: Pending) {
        if (pending.released.compareAndSet(false, true)) inFlight.set(false)
    }

    private inner class Pending(callback: (GeelyBluetoothSnapshot) -> Unit) {
        val callback = AtomicReference(callback)
        val finished = AtomicBoolean(false)
        val cancelled = AtomicBoolean(false)
        val released = AtomicBoolean(false)
        @Volatile var bound = false
        @Volatile var workerStarted = false
        @Volatile var disconnected = false
        @Volatile var timeoutSnapshot = GeelyBluetoothSnapshot.unavailable(BindingState.BIND_TIMEOUT)
        var timeout: Runnable? = null

        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                mainHandler.post {
                    if (finished.get() || cancelled.get() || workerStarted) return@post
                    if (service == null) startEcarxOnly(this@Pending, BindingState.INVALID_BINDER)
                    else startRead(this@Pending, service)
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                mainHandler.post {
                    disconnected = true
                    if (!workerStarted) startEcarxOnly(this@Pending, BindingState.DISCONNECTED)
                }
            }
        }

        fun deliver(snapshot: GeelyBluetoothSnapshot) {
            val listener = callback.getAndSet(null) ?: return
            try {
                listener(snapshot)
            } catch (_: RuntimeException) {
                // A UI callback failure must not retain the service connection or worker slot.
            }
        }
    }

    class Request internal constructor(private val cancelRequest: () -> Unit) {
        fun cancel() = cancelRequest()
    }

    internal interface ServiceBinding {
        fun bind(context: Context, intent: Intent, connection: ServiceConnection): Boolean
        fun unbind(context: Context, connection: ServiceConnection)
    }

    private object AndroidServiceBinding : ServiceBinding {
        override fun bind(context: Context, intent: Intent, connection: ServiceConnection): Boolean =
            context.bindService(intent, connection, 0)

        override fun unbind(context: Context, connection: ServiceConnection) = context.unbindService(connection)
    }

    companion object {
        const val DEFAULT_TIMEOUT_MILLIS = 3_000L
        const val MAX_TIMEOUT_MILLIS = 8_000L
        val SERVICE_COMPONENT = ComponentName("com.anwsdk.service", "com.anwsdk.service.AnwSdkService")

        private val sharedWorker = ThreadPoolExecutor(
            1,
            1,
            0L,
            TimeUnit.MILLISECONDS,
            ArrayBlockingQueue(1),
            { runnable -> Thread(runnable, "diplay-anw-bt-readonly").apply { isDaemon = true } },
        )
    }
}

enum class BindingState {
    CONNECTED,
    NOT_RUNNING,
    ACCESS_DENIED,
    BIND_TIMEOUT,
    READ_TIMEOUT,
    DISCONNECTED,
    INVALID_BINDER,
    BIND_FAILED,
    BINDER_FAILED,
    BUSY,
    CANCELLED,
}

enum class ReadState { OK, UNSUPPORTED, FAILED }

enum class ReadFailure { DESCRIPTOR_MISMATCH, INVALID_RESPONSE, REMOTE_FAILURE, SERVICE_UNAVAILABLE, PERMISSION_DENIED, TIMEOUT }

enum class AnwPowerState { OFF, ON, TURNING_ON, TURNING_OFF, UNKNOWN }

data class ReadValue<T>(val state: ReadState, val value: T? = null, val failure: ReadFailure? = null)

data class GeelyBluetoothSnapshot(
    val bindingState: BindingState,
    val power: ReadValue<AnwPowerState>,
    val pairedCount: ReadValue<Int>,
    val sppInitialized: ReadValue<Boolean>,
    val ecarxEnabled: ReadValue<Boolean>,
) {
    companion object {
        fun unavailable(state: BindingState) = GeelyBluetoothSnapshot(
            bindingState = state,
            power = ReadValue(ReadState.FAILED, failure = ReadFailure.REMOTE_FAILURE),
            pairedCount = ReadValue(ReadState.FAILED, failure = ReadFailure.REMOTE_FAILURE),
            sppInitialized = ReadValue(ReadState.FAILED, failure = ReadFailure.REMOTE_FAILURE),
            ecarxEnabled = ReadValue(ReadState.FAILED, failure = ReadFailure.SERVICE_UNAVAILABLE),
        )
    }
}

/** H52.10500-verified getter transactions only; no setter or connection transaction is exposed. */
internal object GeelyBluetoothReadOnlyProtocol {
    const val DESCRIPTOR = "com.anwsdk.service.IAnwPhoneLink"
    const val TRANSACTION_POWER = 0x03
    const val TRANSACTION_PAIRED_LIST = 0x10
    const val TRANSACTION_SPP_INITIALIZED = 0x43
    const val MAX_PAIRED_DEVICES = 16

    fun read(binder: IBinder): GeelyBluetoothSnapshot {
        val descriptor = try {
            binder.interfaceDescriptor
        } catch (_: SecurityException) {
            return GeelyBluetoothSnapshot.unavailable(BindingState.ACCESS_DENIED)
        } catch (_: RemoteException) {
            return GeelyBluetoothSnapshot.unavailable(BindingState.BINDER_FAILED)
        } catch (_: RuntimeException) {
            return GeelyBluetoothSnapshot.unavailable(BindingState.BINDER_FAILED)
        }
        if (descriptor != DESCRIPTOR) {
            return GeelyBluetoothSnapshot(
                BindingState.INVALID_BINDER,
                ReadValue(ReadState.FAILED, failure = ReadFailure.DESCRIPTOR_MISMATCH),
                ReadValue(ReadState.FAILED, failure = ReadFailure.DESCRIPTOR_MISMATCH),
                ReadValue(ReadState.FAILED, failure = ReadFailure.DESCRIPTOR_MISMATCH),
                ReadValue(ReadState.FAILED, failure = ReadFailure.SERVICE_UNAVAILABLE),
            )
        }
        return GeelyBluetoothSnapshot(
            bindingState = BindingState.CONNECTED,
            power = readPower(binder),
            pairedCount = readPairedCount(binder),
            sppInitialized = readSppInitialized(binder),
            ecarxEnabled = ReadValue(ReadState.FAILED, failure = ReadFailure.SERVICE_UNAVAILABLE),
        )
    }

    private fun readPower(binder: IBinder): ReadValue<AnwPowerState> = transact(binder, TRANSACTION_POWER) { _, reply ->
        val state = when (reply.readInt()) {
            0 -> AnwPowerState.OFF
            1 -> AnwPowerState.ON
            2 -> AnwPowerState.TURNING_ON
            3 -> AnwPowerState.TURNING_OFF
            else -> AnwPowerState.UNKNOWN
        }
        if (state == AnwPowerState.UNKNOWN) ReadValue(ReadState.FAILED, state, ReadFailure.INVALID_RESPONSE)
        else ReadValue(ReadState.OK, state)
    }

    private fun readPairedCount(binder: IBinder): ReadValue<Int> = transact(
        binder,
        TRANSACTION_PAIRED_LIST,
        writeRequest = { data ->
            data.writeInt(1)
            data.writeInt(MAX_PAIRED_DEVICES)
            data.writeInt(MAX_PAIRED_DEVICES)
            data.writeInt(MAX_PAIRED_DEVICES)
        },
    ) { _, reply ->
        val returnCode = reply.readInt()
        if (returnCode != 1) return@transact ReadValue(ReadState.FAILED, failure = ReadFailure.INVALID_RESPONSE)
        val count = readExactIntArray(reply, 1).single()
        skipExactStringArray(reply, MAX_PAIRED_DEVICES)
        skipExactStringArray(reply, MAX_PAIRED_DEVICES)
        readExactIntArray(reply, MAX_PAIRED_DEVICES)
        if (count !in 0..MAX_PAIRED_DEVICES) ReadValue(ReadState.FAILED, failure = ReadFailure.INVALID_RESPONSE)
        else ReadValue(ReadState.OK, count)
    }

    private fun readSppInitialized(binder: IBinder): ReadValue<Boolean> = transact(binder, TRANSACTION_SPP_INITIALIZED) { _, reply ->
        when (reply.readInt()) {
            0 -> ReadValue(ReadState.OK, false)
            1 -> ReadValue(ReadState.OK, true)
            else -> ReadValue(ReadState.FAILED, failure = ReadFailure.INVALID_RESPONSE)
        }
    }

    private fun readExactIntArray(parcel: Parcel, expectedLength: Int): IntArray {
        require(parcel.dataAvail() >= 4) { "missing array length" }
        val actualLength = parcel.readInt()
        require(actualLength == expectedLength) { "unexpected array length" }
        require(parcel.dataAvail() >= expectedLength * 4) { "truncated integer array" }
        return IntArray(expectedLength) { parcel.readInt() }
    }

    private fun skipExactStringArray(parcel: Parcel, expectedLength: Int) {
        require(parcel.dataAvail() >= 4) { "missing string array length" }
        val actualLength = parcel.readInt()
        require(actualLength == expectedLength) { "unexpected string array length" }
        repeat(expectedLength) {
            require(parcel.dataAvail() >= 4) { "truncated string array" }
            parcel.readString()
        }
    }

    private inline fun <T> transact(
        binder: IBinder,
        code: Int,
        crossinline writeRequest: (Parcel) -> Unit = {},
        crossinline parseReply: (Parcel, Parcel) -> ReadValue<T>,
    ): ReadValue<T> {
        val request = Parcel.obtain()
        val reply = Parcel.obtain()
        return try {
            request.writeInterfaceToken(DESCRIPTOR)
            writeRequest(request)
            if (!binder.transact(code, request, reply, 0)) {
                ReadValue(ReadState.UNSUPPORTED)
            } else {
                require(reply.dataAvail() >= 4) { "missing Binder exception status" }
                reply.readException()
                require(reply.dataAvail() >= 4) { "missing Binder result" }
                parseReply(request, reply).also {
                    require(reply.dataAvail() == 0) { "unexpected trailing Binder response" }
                }
            }
        } catch (_: RemoteException) {
            ReadValue(ReadState.FAILED, failure = ReadFailure.REMOTE_FAILURE)
        } catch (_: SecurityException) {
            ReadValue(ReadState.FAILED, failure = ReadFailure.PERMISSION_DENIED)
        } catch (_: RuntimeException) {
            ReadValue(ReadState.FAILED, failure = ReadFailure.INVALID_RESPONSE)
        } finally {
            reply.recycle()
            request.recycle()
        }
    }
}

/** Read-only ECarX manager state. This is a separate Binder contract from ANW. */
internal object EcarxBluetoothReadOnlyProtocol {
    const val SERVICE_NAME = "ecarx_bluetooth_service"
    const val DESCRIPTOR = "ecarx.bluetooth.IBluetoothManager"
    const val TRANSACTION_IS_ENABLED = 0x05

    fun read(): ReadValue<Boolean> {
        val binder = try {
            val serviceManager = Class.forName("android.os.ServiceManager")
            serviceManager.getMethod("getService", String::class.java)
                .invoke(null, SERVICE_NAME) as? IBinder
        } catch (_: SecurityException) {
            return ReadValue(ReadState.FAILED, failure = ReadFailure.PERMISSION_DENIED)
        } catch (error: java.lang.reflect.InvocationTargetException) {
            val failure = if (error.targetException is SecurityException) ReadFailure.PERMISSION_DENIED
            else ReadFailure.SERVICE_UNAVAILABLE
            return ReadValue(ReadState.FAILED, failure = failure)
        } catch (_: Exception) {
            null
        } ?: return ReadValue(ReadState.FAILED, failure = ReadFailure.SERVICE_UNAVAILABLE)

        return readBinder(binder)
    }

    fun readBinder(binder: IBinder): ReadValue<Boolean> {
        val descriptor = try {
            binder.interfaceDescriptor
        } catch (_: SecurityException) {
            return ReadValue(ReadState.FAILED, failure = ReadFailure.PERMISSION_DENIED)
        } catch (_: RemoteException) {
            return ReadValue(ReadState.FAILED, failure = ReadFailure.REMOTE_FAILURE)
        } catch (_: RuntimeException) {
            return ReadValue(ReadState.FAILED, failure = ReadFailure.REMOTE_FAILURE)
        }
        if (descriptor != DESCRIPTOR) {
            return ReadValue(ReadState.FAILED, failure = ReadFailure.DESCRIPTOR_MISMATCH)
        }

        val request = Parcel.obtain()
        val reply = Parcel.obtain()
        return try {
            request.writeInterfaceToken(DESCRIPTOR)
            if (!binder.transact(TRANSACTION_IS_ENABLED, request, reply, 0)) {
                ReadValue(ReadState.UNSUPPORTED)
            } else {
                require(reply.dataAvail() >= 4) { "missing Binder exception status" }
                reply.readException()
                require(reply.dataAvail() >= 4) { "missing Binder result" }
                val rawEnabled = reply.readInt()
                require(reply.dataAvail() == 0) { "unexpected trailing Binder response" }
                val enabled = when (rawEnabled) {
                    0 -> false
                    1 -> true
                    else -> return ReadValue(ReadState.FAILED, failure = ReadFailure.INVALID_RESPONSE)
                }
                ReadValue(ReadState.OK, enabled).also {
                    require(reply.dataAvail() == 0) { "unexpected trailing Binder response" }
                }
            }
        } catch (_: RemoteException) {
            ReadValue(ReadState.FAILED, failure = ReadFailure.REMOTE_FAILURE)
        } catch (_: SecurityException) {
            ReadValue(ReadState.FAILED, failure = ReadFailure.PERMISSION_DENIED)
        } catch (_: RuntimeException) {
            ReadValue(ReadState.FAILED, failure = ReadFailure.INVALID_RESPONSE)
        } finally {
            reply.recycle()
            request.recycle()
        }
    }
}
