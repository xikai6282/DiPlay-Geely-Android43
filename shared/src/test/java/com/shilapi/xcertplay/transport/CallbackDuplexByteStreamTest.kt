package com.shilapi.xcertplay.transport

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CallbackDuplexByteStreamTest {
    @Test fun receivesFifoBytesSplitsChunksTimesOutAndRejectsBytesAfterEof() {
        val stream = CallbackDuplexByteStream(
            writeBytes = { _, _, length -> length },
            closeTransport = {},
            maxPendingBytes = 16,
            maxWriteChunkBytes = 4,
        )

        assertNull(stream.recv(4, 5))
        assertTrue(stream.onBytes(byteArrayOf(1, 2, 3, 4, 5)))
        assertArrayEquals(byteArrayOf(1, 2), stream.recv(2, 100))
        assertArrayEquals(byteArrayOf(3, 4), stream.recv(2, 100))
        assertArrayEquals(byteArrayOf(5), stream.recv(2, 100))
        assertTrue(stream.onEndOfInput())
        assertFalse(stream.onBytes(byteArrayOf(6)))
        assertArrayEquals(ByteArray(0), stream.recv(2, 100))
    }

    @Test fun sendRetriesPartialWritesWithOffsetsAndBoundedChunksWithoutSkippingBytes() {
        val output = ByteArrayOutputStream()
        val requestedSizes = mutableListOf<Int>()
        val stream = CallbackDuplexByteStream(
            writeBytes = { bytes, offset, length ->
                requestedSizes += length
                val consumed = minOf(2, length)
                output.write(bytes, offset, consumed)
                consumed
            },
            closeTransport = {},
            maxPendingBytes = 16,
            maxWriteChunkBytes = 4,
        )
        val payload = ByteArray(11) { (it + 1).toByte() }

        stream.send(payload)

        assertArrayEquals(payload, output.toByteArray())
        assertTrue(requestedSizes.all { it in 1..4 })
    }

    @Test fun zeroProgressAndWriterExceptionsFailStreamAndReleaseOwnerOnce() {
        val closeCalls = AtomicInteger()
        val zeroWriter = CallbackDuplexByteStream(
            writeBytes = { _, _, _ -> 0 },
            closeTransport = { closeCalls.incrementAndGet() },
        )
        val noProgress = expectIOException { zeroWriter.send(byteArrayOf(1, 2)) }
        assertTrue(noProgress.message.orEmpty().contains("invalid progress"))
        assertEquals(1, closeCalls.get())
        assertEquals(noProgress, expectIOException { zeroWriter.recv(1, 0) })
        zeroWriter.close()
        assertEquals(1, closeCalls.get())

        val throwing = CallbackDuplexByteStream(
            writeBytes = { _, _, _ -> throw IllegalStateException("write refused") },
            closeTransport = {},
        )
        val writerFailure = expectIOException { throwing.send(byteArrayOf(1)) }
        assertTrue(writerFailure.cause is IllegalStateException)
        assertEquals(writerFailure, expectIOException { throwing.recv(1, 0) })
    }

    @Test fun writerFailureRequestsTransportCloseAfterReleasingWriterLock() {
        val closeWasOutsideWriterLock = AtomicReference(false)
        lateinit var stream: CallbackDuplexByteStream
        stream = CallbackDuplexByteStream(
            writeBytes = { _, _, _ -> throw IOException("write failed") },
            closeTransport = {
                val nestedSendFinished = CountDownLatch(1)
                Thread {
                    try {
                        stream.send(byteArrayOf(2))
                    } catch (_: IOException) {
                        // The stream has failed; acquiring the writer lock must still be possible.
                    } finally {
                        nestedSendFinished.countDown()
                    }
                }.start()
                closeWasOutsideWriterLock.set(nestedSendFinished.await(1, TimeUnit.SECONDS))
            },
        )

        expectIOException { stream.send(byteArrayOf(1)) }

        assertTrue(closeWasOutsideWriterLock.get())
    }

    @Test fun overflowRejectsWholeChunkPreservesEarlierBytesAndClosesOwner() {
        val closeCalls = AtomicInteger()
        val stream = CallbackDuplexByteStream(
            writeBytes = { _, _, length -> length },
            closeTransport = { closeCalls.incrementAndGet() },
            maxPendingBytes = 4,
            maxWriteChunkBytes = 4,
        )

        assertTrue(stream.onBytes(byteArrayOf(1, 2, 3)))
        assertFalse(stream.onBytes(byteArrayOf(4, 5)))
        assertEquals(1, closeCalls.get())
        assertArrayEquals(byteArrayOf(1, 2), stream.recv(2, 100))
        assertArrayEquals(byteArrayOf(3), stream.recv(2, 100))
        assertTrue(expectIOException { stream.recv(1, 100) }.message.orEmpty().contains("overflow"))
        assertFalse(stream.onBytes(byteArrayOf(6)))
        stream.close()
        assertEquals(1, closeCalls.get())
    }

    @Test fun asynchronousFailureWakesReadersAfterDrainingAcceptedBytes() {
        val stream = CallbackDuplexByteStream(
            writeBytes = { _, _, length -> length },
            closeTransport = {},
            maxPendingBytes = 8,
        )
        assertTrue(stream.onBytes(byteArrayOf(7, 8)))
        val failure = IOException("remote disconnected")

        assertTrue(stream.onFailure(failure))
        assertArrayEquals(byteArrayOf(7, 8), stream.recv(8, 100))
        assertEquals(failure, expectIOException { stream.recv(1, 100) })
        assertFalse(stream.onFailure(IOException("late failure")))
    }

    @Test fun closeWakesBlockedReceiverAndRejectsLateCallbacks() {
        val closeCalls = AtomicInteger()
        val stream = CallbackDuplexByteStream(
            writeBytes = { _, _, length -> length },
            closeTransport = { closeCalls.incrementAndGet() },
        )
        val entered = CountDownLatch(1)
        val received = AtomicReference<ByteArray?>()
        val thread = Thread {
            entered.countDown()
            received.set(stream.recv(1, 5_000))
        }
        thread.start()
        assertTrue(entered.await(1, TimeUnit.SECONDS))
        awaitThreadWaiting(thread)

        stream.close()
        thread.join(1_000)

        assertFalse(thread.isAlive)
        assertArrayEquals(ByteArray(0), received.get())
        assertEquals(1, closeCalls.get())
        assertFalse(stream.onBytes(byteArrayOf(1)))
        assertFalse(stream.onEndOfInput())
        assertFalse(stream.onFailure(IOException("late callback")))
        stream.close()
        assertEquals(1, closeCalls.get())
    }

    @Test fun closeCallbackRunsOutsideBlockedWriterAndPreventsFurtherWrites() {
        val writerEntered = CountDownLatch(1)
        val releaseWriter = CountDownLatch(1)
        val writeCalls = AtomicInteger()
        val closeCalls = AtomicInteger()
        val stream = CallbackDuplexByteStream(
            writeBytes = { _, _, length ->
                writeCalls.incrementAndGet()
                writerEntered.countDown()
                check(releaseWriter.await(2, TimeUnit.SECONDS))
                length
            },
            closeTransport = {
                closeCalls.incrementAndGet()
                releaseWriter.countDown()
            },
            maxWriteChunkBytes = 2,
        )
        val sendFailure = AtomicReference<Throwable?>()
        val sender = Thread {
            try {
                stream.send(byteArrayOf(1, 2, 3, 4))
            } catch (error: Throwable) {
                sendFailure.set(error)
            }
        }
        sender.start()
        assertTrue(writerEntered.await(1, TimeUnit.SECONDS))

        stream.close()
        sender.join(1_000)

        assertFalse(sender.isAlive)
        assertEquals(1, closeCalls.get())
        assertEquals(1, writeCalls.get())
        assertTrue(sendFailure.get() is IOException)
    }

    private fun expectIOException(action: () -> Unit): IOException {
        val error = try {
            action()
            null
        } catch (failure: IOException) {
            failure
        }
        return error ?: throw AssertionError("Expected IOException")
    }

    private fun awaitThreadWaiting(thread: Thread) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(1)
        while (thread.state != Thread.State.WAITING &&
            thread.state != Thread.State.TIMED_WAITING &&
            System.nanoTime() < deadline
        ) {
            Thread.yield()
        }
        assertTrue(thread.state == Thread.State.WAITING || thread.state == Thread.State.TIMED_WAITING)
    }
}
