package com.shilapi.xcertplay.compat

import android.content.Intent
import android.net.VpnService
import android.os.Debug
import android.os.IBinder
import android.util.Log
import com.shilapi.xcertplay.network.VpnTunnelCompat
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/** Debug-only probe of the exact tunnel builder/read helper used by the production USB bridge. */
class Api18VpnProbeService : VpnService() {
    override fun onBind(intent: Intent?): IBinder? = super.onBind(intent)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Thread({ runProbe(); stopSelf(startId) }, "codex-vpn-probe").apply { isDaemon = true; start() }
        return START_NOT_STICKY
    }

    private fun runProbe() {
        var passed = false
        val result = try {
            repeat(3) { cycle ->
                val tun = VpnTunnelCompat.establish(Builder(), "fe80::d1")
                val input = FileInputStream(tun.fileDescriptor)
                val output = FileOutputStream(tun.fileDescriptor)
                val running = AtomicBoolean(true)
                val error = AtomicReference<Throwable?>()
                val cpuNanos = AtomicLong()
                val reads = AtomicLong()
                val echoed = AtomicBoolean(false)
                val reader = Thread({
                    val before = Debug.threadCpuTimeNanos()
                    try {
                        val buffer = ByteArray(4096)
                        while (running.get()) {
                            val length = VpnTunnelCompat.read(input, buffer) { running.get() }
                            if (length < 0) break
                            if (length > 0) reads.incrementAndGet()
                            if (length >= 48 && buffer[6].toInt() and 255 == 58 &&
                                buffer[40].toInt() and 255 == 129 && buffer[44].toInt() and 255 == 1) {
                                echoed.set(true)
                            }
                        }
                    } catch (failure: Throwable) {
                        if (running.get()) error.set(failure)
                    } finally { cpuNanos.set(Debug.threadCpuTimeNanos() - before) }
                }, "codex-tun-reader").apply { isDaemon = true; start() }
                try {
                    // Inject a valid ICMPv6 echo packet addressed to the tunnel itself. The kernel
                    // should answer through the TUN, exercising production writes and packet reads.
                    val packet = echoRequest()
                    check(VpnTunnelCompat.write(output, packet, packet.size) { running.get() })
                    Thread.sleep(2000)
                    check(error.get() == null) { "Idle TUN reader failed: ${error.get()}" }
                    check(reader.isAlive) { "Reader exited before cancellation" }
                    check(echoed.get()) { "Injected IPv6 echo request produced no echo reply through TUN (packets=${reads.get()})" }
                } finally {
                    running.set(false)
                    tun.close()
                    reader.interrupt()
                    reader.join(3000)
                    runCatching { input.close() }
                }
                check(!reader.isAlive) { "TUN reader failed to stop after close" }
                check(error.get() == null) { "TUN reader failed: ${error.get()}" }
                check(cpuNanos.get() < 250_000_000L) { "Idle reader busy-looped cpuMs=${cpuNanos.get() / 1_000_000}" }
                Log.i(Api18CompatProbeActivity.TAG,
                    "VPN cycle=${cycle + 1} idleMs=2000 cpuMs=${cpuNanos.get() / 1_000_000} packets=${reads.get()} stopped=true")
            }
            passed = true
            "PASS VPN establishIdleCloseCycles=3"
        } catch (failure: Throwable) {
            Log.e(Api18CompatProbeActivity.TAG, "VPN probe failed", failure)
            "FAIL VPN ${failure.javaClass.simpleName}: ${failure.message}"
        }
        Log.i(Api18CompatProbeActivity.TAG, result)
        sendBroadcast(Intent(Api18CompatProbeActivity.VPN_RESULT).setPackage(packageName)
            .putExtra("passed", passed).putExtra("result", result))
    }

    private fun echoRequest(): ByteArray {
        val packet = ByteArray(48)
        packet[0] = 0x60
        packet[5] = 8
        packet[6] = 58
        packet[7] = 64
        val source = java.net.InetAddress.getByName("fe80::d2").address
        val destination = java.net.InetAddress.getByName("fe80::d1").address
        source.copyInto(packet, 8)
        destination.copyInto(packet, 24)
        packet[40] = 128.toByte()
        packet[44] = 1
        packet[47] = 1
        var checksum = 8 + 58 // ICMPv6 length and next-header pseudo-header fields.
        for (offset in 8 until 40 step 2) {
            checksum += ((packet[offset].toInt() and 255) shl 8) or (packet[offset + 1].toInt() and 255)
        }
        for (offset in 40 until 48 step 2) {
            checksum += ((packet[offset].toInt() and 255) shl 8) or (packet[offset + 1].toInt() and 255)
        }
        while (checksum > 65535) checksum = (checksum and 65535) + (checksum ushr 16)
        checksum = checksum.inv() and 65535
        packet[42] = (checksum shr 8).toByte()
        packet[43] = checksum.toByte()
        return packet
    }
}
