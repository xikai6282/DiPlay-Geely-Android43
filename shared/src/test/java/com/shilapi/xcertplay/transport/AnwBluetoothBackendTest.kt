package com.shilapi.xcertplay.transport

import android.os.Binder
import android.os.IBinder
import android.os.Parcel
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class AnwBluetoothBackendTest {
    private val address = "01:02:03:04:05:06"
    private val remoteUuid = UUID.fromString("00000000-deca-fade-deca-deafdecacafe")

    private inner class FactoryService(var initialized: Boolean = true) : Binder() {
        var initializations = 0
        var connectResult = 1
        var connectIndex = 2
        var initResult = 1
        var stateQueries = 0
        var stateAddress = address
        var requested = false
        var callback: IBinder? = null
        var unregistered = false
        val disconnected = CountDownLatch(1)
        val unregisterDone = CountDownLatch(1)
        val written = ByteArrayOutputStream()
        init { attachInterface(null, AnwSppProtocol.DESCRIPTOR) }
        override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
            val out = requireNotNull(reply)
            data.enforceInterface(AnwSppProtocol.DESCRIPTOR)
            out.writeNoException()
            when (code) {
                3 -> out.writeInt(1)
                0x43 -> out.writeInt(if (initialized) 1 else 0)
                0x3e -> {
                    assertArrayEquals(AnwBluetoothBackend.uuidBytes(UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")), data.createByteArray())
                    initializations++; initialized = true; out.writeInt(initResult)
                }
                0x44 -> callback = data.readStrongBinder()
                0x45 -> {
                    assertSame(callback, data.readStrongBinder()); unregistered = true; unregisterDone.countDown()
                }
                0x40 -> {
                    assertEquals(address, data.readString())
                    assertArrayEquals(AnwBluetoothBackend.uuidBytes(remoteUuid), data.createByteArray())
                    assertEquals(1, data.readInt()); requested = true
                    if (connectResult == 1) {
                        val request = Parcel.obtain(); val response = Parcel.obtain()
                        try {
                            request.writeInterfaceToken(AnwSppProtocol.CALLBACK_DESCRIPTOR)
                            request.writeInt(5); request.writeByteArray(byteArrayOf(99)); request.writeInt(1)
                            assertTrue(callback!!.transact(1, request, response, 0)); response.readException()
                            request.setDataSize(0); request.setDataPosition(0)
                            response.setDataSize(0); response.setDataPosition(0)
                            request.writeInterfaceToken(AnwSppProtocol.CALLBACK_DESCRIPTOR)
                            request.writeInt(connectIndex); request.writeByteArray(byteArrayOf(7, 8, 9)); request.writeInt(3)
                            assertTrue(callback!!.transact(1, request, response, 0)); response.readException()
                        } finally { response.recycle(); request.recycle() }
                    }
                    out.writeInt(connectResult); out.writeIntArray(intArrayOf(if (connectResult == 1) connectIndex else -1))
                }
                0xd0 -> {
                    assertEquals(connectIndex, data.readInt()); repeat(4) { assertEquals(1, data.readInt()) }
                    stateQueries++
                    out.writeInt(1); out.writeIntArray(intArrayOf(1))
                    out.writeStringArray(arrayOf(stateAddress)); out.writeStringArray(arrayOf("iPhone"))
                    out.writeInt(1); out.writeInt(0) // unused nullable OEM Parcelable
                }
                0x42 -> {
                    assertEquals(2, data.readInt()); val bytes = data.createByteArray()!!
                    assertEquals(bytes.size, data.readInt()); assertEquals(1, data.readInt())
                    val count = minOf(2, bytes.size); written.write(bytes, 0, count)
                    out.writeInt(1); out.writeIntArray(intArrayOf(count))
                }
                0x41 -> { assertEquals(connectIndex, data.readInt()); out.writeInt(1); disconnected.countDown() }
                else -> error("Unexpected vendor transaction $code")
            }
            assertEquals(0, data.dataAvail())
            return true
        }
    }

    private fun <T> worker(action: () -> T): T {
        val executor = Executors.newSingleThreadExecutor()
        try { return executor.submit<T> { action() }.get(8, TimeUnit.SECONDS) }
        finally { executor.shutdownNow() }
    }

    @Test fun existingSppConnectsWithoutReinitializingAndPreservesEarlyCallbackBytes() {
        val service = FactoryService()
        val backend = AnwBluetoothBackend(RuntimeEnvironment.getApplication(), service)
        val stream = worker { backend.connect(address, remoteUuid) }
        assertArrayEquals(byteArrayOf(7, 8, 9), stream.recv(20, 0))
        stream.send(byteArrayOf(1, 2, 3, 4, 5))
        assertArrayEquals(byteArrayOf(1, 2, 3, 4, 5), service.written.toByteArray())
        assertEquals(0, service.initializations)
        stream.close()
        assertTrue(service.disconnected.await(5, TimeUnit.SECONDS))
        assertTrue(service.unregistered)
    }

    @Test fun missingInitializationRegistersLocalSppDistinctFromRemoteIap2Uuid() {
        val service = FactoryService(initialized = false)
        val stream = worker { AnwBluetoothBackend(RuntimeEnvironment.getApplication(), service).connect(address, remoteUuid) }
        assertEquals(1, service.initializations)
        stream.close(); assertTrue(service.disconnected.await(5, TimeUnit.SECONDS))
    }

    @Test fun rejectedConnectionNeverDisconnectsAnUnownedIndex() {
        val service = FactoryService().apply { connectResult = 2006 }
        val failure = runCatching { worker { AnwBluetoothBackend(RuntimeEnvironment.getApplication(), service).connect(address, remoteUuid) } }.exceptionOrNull()
        assertTrue(failure?.cause is IOException)
        assertTrue(service.unregisterDone.await(5, TimeUnit.SECONDS))
        assertEquals(1L, service.disconnected.count)
    }

    @Test fun cancelledAfterAcceptedRequestReleasesOnlyItsConfirmedSlot() {
        val service = FactoryService()
        val failure = runCatching { worker {
            AnwBluetoothBackend(RuntimeEnvironment.getApplication(), service).connect(address, remoteUuid, cancelled = { service.requested })
        } }.exceptionOrNull()
        assertTrue(failure?.cause is IOException)
        assertTrue(service.disconnected.await(5, TimeUnit.SECONDS))
    }

    @Test fun acceptedUnsupportedNativeSlotIsReleasedWithoutCallingRestrictedStateGetter() {
        val service = FactoryService().apply { connectIndex = 5 }
        val failure = runCatching { worker { AnwBluetoothBackend(RuntimeEnvironment.getApplication(), service).connect(address, remoteUuid) } }.exceptionOrNull()
        assertTrue(failure?.cause is IOException)
        assertTrue(service.disconnected.await(5, TimeUnit.SECONDS))
        assertEquals(0, service.stateQueries)
    }

    @Test fun initializationFailureCannotBeHiddenByManufacturersTrueJavaFlagOnRetry() {
        val service = FactoryService(initialized = false).apply { initResult = 2006 }
        val backend = AnwBluetoothBackend(RuntimeEnvironment.getApplication(), service)
        repeat(2) {
            val failure = runCatching { worker { backend.connect(address, remoteUuid) } }.exceptionOrNull()
            assertTrue(failure?.cause is IOException)
        }
        assertEquals(1, service.initializations)
        assertFalse(service.requested)
    }

    @Test fun slotReusedByDifferentAddressIsNeverDisconnectedOrReturnedAsOurStream() {
        val service = FactoryService().apply { stateAddress = "11:12:13:14:15:16" }
        val failure = runCatching { worker {
            AnwBluetoothBackend(RuntimeEnvironment.getApplication(), service).connect(address, remoteUuid, cancelled = { service.stateQueries > 0 })
        } }.exceptionOrNull()
        assertTrue(failure?.cause is IOException)
        assertTrue(service.unregisterDone.await(5, TimeUnit.SECONDS))
        assertEquals(1L, service.disconnected.count)
    }
}
