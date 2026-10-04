package com.shilapi.xcertplay.transport

import android.os.Binder
import android.os.IBinder
import android.os.Parcel
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class AnwSppProtocolTest {
    private fun service(handler: (Int, Parcel, Parcel) -> Unit): IBinder = object : Binder() {
        init { attachInterface(null, AnwSppProtocol.DESCRIPTOR) }
        override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
            data.enforceInterface(AnwSppProtocol.DESCRIPTOR)
            handler(code, data, requireNotNull(reply))
            return true
        }
    }

    @Test fun connectionRequestUsesExactOutputCapacityAndKeepsAcceptedSeparateFromConnected() {
        val uuid = ByteArray(16) { it.toByte() }
        val client = AnwSppProtocol(service { code, data, reply ->
            assertEquals(0x40, code)
            assertEquals("01:02:03:04:05:06", data.readString())
            assertArrayEquals(uuid, data.createByteArray())
            assertEquals(1, data.readInt())
            assertEquals(0, data.dataAvail())
            reply.writeNoException(); reply.writeInt(1); reply.writeIntArray(intArrayOf(2))
        })
        assertEquals(AnwSppProtocol.ConnectRequest(1, 2), client.requestConnection("01:02:03:04:05:06", uuid))
    }

    @Test fun unsafeUuidLengthNeverReachesVendorBinder() {
        var calls = 0
        val client = AnwSppProtocol(service { _, _, _ -> calls++ })
        for (size in listOf(0, 15, 17, 32)) {
            try { client.requestConnection("01:02:03:04:05:06", ByteArray(size)); fail() }
            catch (_: IllegalArgumentException) { }
        }
        assertEquals(0, calls)
    }

    @Test fun partialWriteReturnsActualCountAndRejectsImpossibleProgress() {
        var count = 2
        val client = AnwSppProtocol(service { code, data, reply ->
            assertEquals(0x42, code); assertEquals(1, data.readInt())
            assertArrayEquals(byteArrayOf(1, 2, 3), data.createByteArray())
            assertEquals(3, data.readInt()); assertEquals(1, data.readInt())
            assertEquals(0, data.dataAvail())
            reply.writeNoException(); reply.writeInt(1); reply.writeIntArray(intArrayOf(count))
        })
        assertEquals(2, client.write(1, byteArrayOf(1, 2, 3)))
        for (invalid in listOf(-1, 0, 4)) {
            count = invalid
            try { client.write(1, byteArrayOf(1, 2, 3)); fail() }
            catch (_: IOException) { }
        }
    }

    @Test fun callbackRegistrationUsesSameBinderForRemoval() {
        val callback = Binder()
        val codes = mutableListOf<Int>()
        val client = AnwSppProtocol(service { code, data, reply ->
            codes.add(code); assertSame(callback, data.readStrongBinder())
            assertEquals(0, data.dataAvail()); reply.writeNoException()
        })
        client.register(callback); client.unregister(callback)
        assertEquals(listOf(0x44, 0x45), codes)
    }
}
