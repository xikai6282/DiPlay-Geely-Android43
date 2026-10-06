package com.shilapi.xcertplay.transport

import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.os.Build
import android.os.SystemClock

/**
 * Synchronous USB bulk transfers for Android 4.3.
 *
 * The queue(ByteBuffer) and requestWait(timeout) overloads used by the modern path require API 26.
 * Older queue(ByteBuffer, length) and requestWait() exist from API 12, but lack that bounded wait.
 * Older units use synchronous bulkTransfer(). Its (endpoint, byte[], offset, length, timeout) overload was
 * added in API 18, which is this project's floor, so transfers move in place without a per-chunk
 * scratch copy.
 *
 * Before API 28 the platform silently truncates any bulkTransfer longer than 16 KB, on every release
 * from 12 to 27, so writeAll chunks on any version below 28 rather than only below 26.
 *
 * A single bulkTransfer is capped at 16 KB, so longer transfers are split here. Reads deliberately
 * return after one chunk: the framing layer above already reassembles 32 KB NCM frames through its
 * buffered append, and waiting for a second chunk would add a timeout to every frame.
 */
object LegacyUsbTransfer {

    /** The platform's hard limit for one bulkTransfer on Android 4.x. */
    const val MAX_CHUNK_BYTES = 16 * 1024

    /** True when the bounded modern request overloads are unavailable. */
    val synchronousReadsRequired: Boolean
        get() = Build.VERSION.SDK_INT < Build.VERSION_CODES.O

    /**
     * True when bulkTransfer may truncate a long transfer, i.e. every release before API 28. Writes
     * must therefore chunk on API 26-27 too, not only on the pre-26 synchronous path.
     */
    val chunkedWritesRequired: Boolean
        get() = Build.VERSION.SDK_INT < Build.VERSION_CODES.P

    /** The chunking boundary for a whole transfer. */
    const val CHUNK_BYTES = MAX_CHUNK_BYTES

    /**
     * The injectable part of a transfer, so the chunking rules can be tested without USB hardware.
     * Returns bytes transferred, or a negative value on error, exactly like bulkTransfer.
     */
    fun interface ChunkTransfer {
        /**
         * Moves up to [length] bytes at [offset] within the buffer the callback was built over.
         * @return bytes transferred, or a negative value on error.
         */
        fun transfer(offset: Int, length: Int, timeoutMillis: Int): Int
    }

    /**
     * Monotonic nanosecond clock, injectable so the deadline rules are testable off-device. Defaults
     * to SystemClock.elapsedRealtime; the production path never replaces it.
     */
    var clock: () -> Long = { SystemClock.elapsedRealtime() * 1_000_000L }

    /** Builds the per-chunk call for a real device. */
    fun endpointReader(
        connection: UsbDeviceConnection,
        endpoint: UsbEndpoint,
        target: ByteArray,
        offset: Int,
    ): ChunkTransfer = ChunkTransfer { chunkOffset, length, timeoutMillis ->
        H52UsbReadPump.read(connection, endpoint, target, offset + chunkOffset, length, timeoutMillis)
    }

    fun endpointWriter(
        connection: UsbDeviceConnection,
        endpoint: UsbEndpoint,
        source: ByteArray,
        offset: Int,
    ): ChunkTransfer = ChunkTransfer { chunkOffset, length, timeoutMillis ->
        connection.bulkTransfer(endpoint, source, offset + chunkOffset, length, timeoutMillis)
    }

    /**
     * Reads at most one chunk. A negative return means the device reported an error; zero means
     * nothing was queued, which the caller treats as "no data yet" rather than end of stream.
     */
    fun readOnce(
        transfer: ChunkTransfer,
        target: ByteArray,
        offset: Int,
        capacity: Int,
        timeoutMillis: Int,
    ): Int {
        require(timeoutMillis > 0) { "timeoutMillis must be positive" }
        require(offset >= 0) { "offset must not be negative" }
        require(capacity > 0) { "capacity must be positive" }
        require(offset + capacity <= target.size) { "offset+capacity exceeds the target buffer" }
        val chunk = minOf(MAX_CHUNK_BYTES, capacity)
        return transfer.transfer(0, chunk, timeoutMillis)
    }

    /**
     * Writes [length] bytes, continuing past a short write so the frame is either transferred whole
     * or reported as a cumulative failure. Every chunk gets the time left on a shared deadline, and
     * a non-positive remainder stops the loop rather than waiting forever.
     *
     * @return the number of bytes actually written.
     */
    fun writeAll(
        transfer: ChunkTransfer,
        sourceOffset: Int,
        length: Int,
        deadlineNanos: Long,
    ): Int {
        require(sourceOffset >= 0) { "sourceOffset must not be negative" }
        require(length >= 0) { "length must not be negative" }
        var total = 0
        while (total < length) {
            val remainingMillis = remainingMillis(deadlineNanos)
            if (remainingMillis <= 0) return total
            val chunk = minOf(MAX_CHUNK_BYTES, length - total)
            // A short write still advances, so the rest of the frame is attempted rather than the
            // whole transfer being reported as done.
            val written = transfer.transfer(total, chunk, remainingMillis)
            if (written <= 0) return total
            total += written
        }
        return total
    }

    /** Milliseconds left on a shared deadline, never below 1 so a chunk always has some budget. */
    fun remainingMillis(deadlineNanos: Long): Int {
        val remainingNanos = deadlineNanos - clock()
        if (remainingNanos <= 0) return 0
        val millis = (remainingNanos / 1_000_000L).toInt()
        return if (millis > 0) millis else 1
    }
}
