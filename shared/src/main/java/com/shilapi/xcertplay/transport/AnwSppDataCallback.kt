package com.shilapi.xcertplay.transport

import android.os.Binder
import android.os.Parcel
import java.io.IOException

/** Decodes the OEM synchronous callback, without doing transport writes on a Binder thread. */
internal class AnwSppDataCallback(
    private val onBytes: (Int, ByteArray) -> Unit,
    private val onFailure: (Int, IOException) -> Unit,
) : Binder() {
    @Volatile private var stopped = false

    init { attachInterface(null, AnwSppProtocol.CALLBACK_DESCRIPTOR) }

    fun stop() { stopped = true }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
        if (code == INTERFACE_TRANSACTION) {
            reply?.writeString(AnwSppProtocol.CALLBACK_DESCRIPTOR)
            return true
        }
        if (code != 1) return super.onTransact(code, data, reply, flags)
        data.enforceInterface(AnwSppProtocol.CALLBACK_DESCRIPTOR)
        val index = data.readInt()
        val bytes = data.createByteArray()
        val length = data.readInt()
        if (!stopped) {
            if (index !in 0..9) {
                // This callback is global; an unknown unrelated slot is not our stream.
            } else if (bytes == null || length !in 0..bytes.size || length > 65536) {
                onFailure(index, IOException("Malformed ANW SPP data callback"))
            } else if (length > 0) {
                onBytes(index, bytes.copyOf(length))
            }
        }
        reply?.writeNoException()
        return true
    }
}
