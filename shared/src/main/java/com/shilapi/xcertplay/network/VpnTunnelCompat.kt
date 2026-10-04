package com.shilapi.xcertplay.network

import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.locks.LockSupport

/**
 * The VpnService tun lifecycle shared by the CarPlay bridge and by debug probes, so both exercise
 * the same establishment and idle-read behaviour on Android 4.3.
 *
 * VpnService.Builder itself is API 14; only setBlocking() is API 21. On API 18-20 the tun
 * descriptor therefore stays non-blocking and an idle link surfaces as IOException/EAGAIN, which
 * [read] absorbs and retries instead of treating it as a fatal tunnel error.
 */
object VpnTunnelCompat {

    /** True when the descriptor returned by [establish] blocks reads, i.e. API 21+. */
    val blockingSupported: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP

    /** Nanoseconds to back off after an idle non-blocking read. */
    const val WOULD_BLOCK_BACKOFF_NANOS = 2_000_000L

    /**
     * Applies the API-21 setBlocking() call when the platform has it. Returns true when the caller
     * may rely on blocking reads; false means the descriptor stays non-blocking.
     */
    fun configureBlocking(builder: VpnService.Builder): Boolean {
        if (!blockingSupported) return false
        return runCatching { builder.setBlocking(true); true }.getOrDefault(false)
    }

    /**
     * Establishes the tun for [linkLocal] with the CarPlay link-local prefix and route, applying
     * setBlocking() where supported.
     *
     * @throws IOException when the platform refuses to establish.
     */
    fun establish(builder: VpnService.Builder, linkLocal: String): ParcelFileDescriptor {
        builder.addAddress(linkLocal, CarPlayVpnService.LINK_PREFIX)
            .addRoute(CarPlayVpnService.LINK_LOCAL_ROUTE, CarPlayVpnService.LINK_PREFIX)
            .setSession(CarPlayVpnService.SESSION_NAME)
            .setMtu(CarPlayVpnService.TUN_MTU)
        configureBlocking(builder)
        return builder.establish() ?: throw IOException("VpnService.establish returned null")
    }

    /**
     * Reads one packet from [input] into [buffer], looping across idle non-blocking reads.
     *
     * Returns the packet length, or -1 once [running] turns false so callers can exit their loop.
     * A genuine I/O failure still propagates. Android's tun may also report a zero-byte read while
     * its network is registering; that is idle too, not end of stream.
     */
    fun read(input: FileInputStream, buffer: ByteArray, running: () -> Boolean): Int {
        while (running()) {
            val length = try {
                input.read(buffer)
            } catch (error: IOException) {
                if (isWouldBlock(error)) {
                    LockSupport.parkNanos(WOULD_BLOCK_BACKOFF_NANOS)
                    continue
                }
                throw error
            }
            if (length == -1) return -1
            if (length == 0) {
                LockSupport.parkNanos(WOULD_BLOCK_BACKOFF_NANOS)
                continue
            }
            return length
        }
        return -1
    }

    /**
     * Non-blocking counterpart of read for a descriptor that is known to be idle-prone. Returns the
     * packet length, 0 when there is nothing ready, or -1 once [running] turns false.
     */
    fun readNonBlocking(input: FileInputStream, buffer: ByteArray, running: () -> Boolean): Int {
        if (!running()) return -1
        val length = try {
            input.read(buffer)
        } catch (error: IOException) {
            return if (isWouldBlock(error)) 0 else throw error
        }
        return if (length == -1) -1 else length
    }

    /**
     * Writes one whole tun packet, retrying only while the descriptor reports EAGAIN. A tun write
     * is all-or-nothing at the packet level: FileOutputStream.write either consumes the full
     * buffer or throws, so this never assembles a packet from partial writes.
     *
     * @return true once the packet was written, false if [running] turned false first.
     */
    fun write(output: FileOutputStream, buffer: ByteArray, length: Int, running: () -> Boolean): Boolean =
        write(output, buffer, 0, length, running)

    /** Whole-packet write of buffer[offset, offset+length) with EAGAIN retry. */
    fun write(
        output: FileOutputStream,
        buffer: ByteArray,
        offset: Int,
        length: Int,
        running: () -> Boolean,
    ): Boolean {
        while (running()) {
            try {
                output.write(buffer, offset, length)
                return true
            } catch (error: IOException) {
                if (!isWouldBlock(error)) throw error
                LockSupport.parkNanos(WOULD_BLOCK_BACKOFF_NANOS)
            }
        }
        return false
    }

    /** True when a tun read or write failed only because the descriptor had nothing ready. */
    fun isWouldBlock(error: IOException): Boolean {
        val message = error.message?.uppercase() ?: return false
        return message.contains("EAGAIN") || message.contains("WOULDBLOCK")
    }

    /** Closes a tun descriptor without relying on Closeable, which Socket-like types lack pre-19. */
    fun closeQuietly(descriptor: ParcelFileDescriptor?) {
        if (descriptor == null) return
        runCatching { descriptor.close() }
    }
}