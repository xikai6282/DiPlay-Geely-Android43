package com.shilapi.xcertplay.transport

import java.io.IOException
import java.util.ArrayDeque
import kotlin.math.min

/**
 * A bounded [BlockingDuplexByteStream] for transports that deliver inbound bytes by callback.
 *
 * The owner supplies a synchronous writer that reports how many bytes it consumed from the given
 * range. [send] retries partial progress and serializes concurrent writers. The close callback is
 * invoked without the state or writer lock so it can interrupt a blocked write. It must be a
 * nonblocking close request: an owner that needs Binder unregistration must schedule it away from
 * an inbound Binder callback and must not wait for an active send. This class does not establish
 * or infer a transport connection.
 */
class CallbackDuplexByteStream(
    private val writeBytes: (ByteArray, Int, Int) -> Int,
    private val closeTransport: () -> Unit,
    private val maxPendingBytes: Int = DEFAULT_MAX_PENDING_BYTES,
    private val maxWriteChunkBytes: Int = DEFAULT_MAX_WRITE_CHUNK_BYTES,
) : BlockingDuplexByteStream {
    private val lock = Object()
    private val sendLock = Object()
    private val pending = ArrayDeque<ByteArray>()
    private var pendingBytes = 0
    private var inputEnded = false
    private var closed = false
    private var closeStarted = false
    private var failure: IOException? = null

    init {
        require(maxPendingBytes > 0) { "maxPendingBytes must be positive" }
        require(maxWriteChunkBytes > 0) { "maxWriteChunkBytes must be positive" }
    }

    /**
     * Queues an entire callback chunk, or rejects it in full and fails the stream on overflow.
     * A false result means the owner must stop delivering callbacks for this stream.
     */
    fun onBytes(bytes: ByteArray): Boolean {
        if (bytes.isEmpty()) return synchronized(lock) { canAcceptInputLocked() }
        val overflow: IOException
        synchronized(lock) {
            if (!canAcceptInputLocked()) return false
            if (bytes.size > maxPendingBytes - pendingBytes) {
                overflow = IOException("Callback duplex receive buffer overflow")
                failure = overflow
                lock.notifyAll()
            } else {
                pending.addLast(bytes.copyOf())
                pendingBytes += bytes.size
                lock.notifyAll()
                return true
            }
        }
        closeTransportAfterFailure(overflow)
        return false
    }

    /** Marks the inbound half closed. Already queued bytes remain readable. */
    fun onEndOfInput(): Boolean = synchronized(lock) {
        if (!canAcceptInputLocked()) return@synchronized false
        inputEnded = true
        lock.notifyAll()
        true
    }

    /** Marks the whole stream failed, wakes readers, and closes the owned transport once. */
    fun onFailure(cause: Throwable): Boolean {
        val io = asIOException(cause, "Callback duplex transport failed")
        val accepted = synchronized(lock) {
            if (closed || failure != null) {
                false
            } else {
                failure = io
                lock.notifyAll()
                true
            }
        }
        if (accepted) closeTransportAfterFailure(io)
        return accepted
    }

    override fun send(data: ByteArray) {
        if (data.isEmpty()) {
            ensureWritable()
            return
        }
        var sendError: IOException? = null
        var requestClose = false
        synchronized(sendLock) {
            try {
                var offset = 0
                while (offset < data.size) {
                    ensureWritable()
                    val requested = min(maxWriteChunkBytes, data.size - offset)
                    val written = try {
                        writeBytes(data, offset, requested)
                    } catch (error: IOException) {
                        throw error
                    } catch (error: RuntimeException) {
                        throw IOException("Callback duplex write failed", error)
                    }

                    ensureWritable()
                    if (written !in 1..requested) {
                        throw IOException("Callback duplex writer made invalid progress: $written/$requested")
                    }
                    offset += written
                }
            } catch (error: IOException) {
                sendError = error
                requestClose = recordFailure(error)
            }
        }
        val error = sendError ?: return
        if (requestClose) closeTransportAfterFailure(error)
        throw error
    }

    /** Returns null on timeout, an empty array on clean EOF/close, and queued bytes before errors. */
    override fun recv(maxBytes: Int, timeoutMillis: Long): ByteArray? {
        require(maxBytes > 0) { "maxBytes must be positive" }
        require(timeoutMillis >= 0) { "timeoutMillis must not be negative" }
        val startedAt = System.nanoTime()
        val timeoutNanos = if (timeoutMillis > Long.MAX_VALUE / NANOS_PER_MILLISECOND) {
            Long.MAX_VALUE
        } else {
            timeoutMillis * NANOS_PER_MILLISECOND
        }

        synchronized(lock) {
            while (true) {
                takePendingLocked(maxBytes)?.let { return it }
                failure?.let { throw it }
                if (inputEnded || closed) return EMPTY

                val elapsedNanos = System.nanoTime() - startedAt
                val remainingNanos = timeoutNanos - elapsedNanos
                if (remainingNanos <= 0) return null
                try {
                    lock.wait(
                        remainingNanos / NANOS_PER_MILLISECOND,
                        (remainingNanos % NANOS_PER_MILLISECOND).toInt(),
                    )
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                    return null
                }
            }
        }
    }

    /** Closes the transport once and wakes blocked receivers; does not wait for a blocked writer. */
    override fun close() {
        val changed = synchronized(lock) {
            if (closed) {
                false
            } else {
                closed = true
                lock.notifyAll()
                true
            }
        }
        if (!changed) return
        closeTransportOnce()?.let { throw it }
    }

    private fun ensureWritable() {
        synchronized(lock) {
            failure?.let { throw it }
            if (closed) throw IOException("Callback duplex stream is closed")
        }
    }

    private fun recordFailure(error: IOException): Boolean = synchronized(lock) {
            if (closed || failure != null) {
                false
            } else {
                failure = error
                lock.notifyAll()
                true
            }
        }

    private fun canAcceptInputLocked(): Boolean = !closed && !inputEnded && failure == null

    private fun takePendingLocked(maxBytes: Int): ByteArray? {
        val chunk = pending.pollFirst() ?: return null
        pendingBytes -= chunk.size
        if (chunk.size <= maxBytes) return chunk

        val head = chunk.copyOf(maxBytes)
        val tail = chunk.copyOfRange(maxBytes, chunk.size)
        pending.addFirst(tail)
        pendingBytes += tail.size
        return head
    }

    private fun closeTransportAfterFailure(error: IOException) {
        closeTransportOnce()?.let { closeError ->
            if (error !== closeError) error.addSuppressed(closeError)
        }
    }

    private fun closeTransportOnce(): IOException? {
        val shouldClose = synchronized(lock) {
            if (closeStarted) false else {
                closeStarted = true
                true
            }
        }
        if (!shouldClose) return null
        return try {
            closeTransport()
            null
        } catch (error: Throwable) {
            if (error is Error) throw error
            asIOException(error, "Could not close callback duplex transport")
        }
    }

    private fun asIOException(error: Throwable, message: String): IOException =
        error as? IOException ?: IOException(message, error)

    private companion object {
        const val DEFAULT_MAX_PENDING_BYTES = 65_536
        const val DEFAULT_MAX_WRITE_CHUNK_BYTES = 8_192
        const val NANOS_PER_MILLISECOND = 1_000_000L
        val EMPTY = ByteArray(0)
    }
}
