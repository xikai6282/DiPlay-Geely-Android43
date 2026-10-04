package com.shilapi.xcertplay.compat

import com.shilapi.xcertplay.airplay.NtpClock
import com.shilapi.xcertplay.airplay.ScreenStream
import android.content.Context
import com.shilapi.xcertplay.airplay.AirPlayConfig
import com.shilapi.xcertplay.airplay.AirPlayDisplayConfig
import com.shilapi.xcertplay.airplay.AirPlayIdentity
import com.shilapi.xcertplay.network.CarPlayBonjour
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/** Exercises production listeners with actual IPv6 peers, rather than only successful binds. */
internal object LegacyNetworkProbe {
    fun bonjourDiscovery(context: Context): String {
        val address = NetworkInterface.getNetworkInterfaces().toList()
            .flatMap { it.inetAddresses.toList() }
            .first { it is Inet4Address && !it.isLoopbackAddress }
        val resolved = CountDownLatch(1)
        val probed = CountDownLatch(1)
        val controlName = "Codex-Control-${System.nanoTime()}"
        val controlServer = java.net.ServerSocket(0, 4, address)
        val controlPort = controlServer.localPort
        val responder = Thread({
            try {
                while (!controlServer.isClosed) {
                    val client = controlServer.accept()
                    try {
                        client.soTimeout = 2_000
                        val request = client.getInputStream().bufferedReader(Charsets.US_ASCII).readLine()
                        check(request.startsWith("GET ") || request.startsWith("POST ")) { "Unexpected control probe request" }
                        client.getOutputStream().write("HTTP/1.1 200 OK\r\nContent-Length: 0\r\n\r\n".toByteArray(Charsets.US_ASCII))
                    } finally { client.close() }
                }
            } catch (_: java.net.SocketException) { }
        }, "codex-control-http").apply { isDaemon = true; start() }
        val config = AirPlayConfig("Codex-Discovery-Probe", "02:00:00:00:00:01", "02:00:00:00:00:02",
            "1.0", AirPlayDisplayConfig(320, 240))
        val bonjour = CarPlayBonjour(context, config, AirPlayIdentity.generate(), address.hostAddress,
            useInterfaceMdns = false, onEvent = { event ->
                android.util.Log.i("CodexApi18Probe", "BONJOUR_DISCOVERY_EVENT $event")
                if (event is com.shilapi.xcertplay.network.CarPlayBonjourEvent.Resolved &&
                    event.endpoint.serviceName == controlName && event.endpoint.port == controlPort &&
                    event.endpoint.bluetoothId == "02:00:00:00:00:03") resolved.countDown()
                if (event is com.shilapi.xcertplay.network.CarPlayBonjourEvent.Probed &&
                    event.endpoint.serviceName == controlName && event.endpoint.bluetoothId == "02:00:00:00:00:03" &&
                    event.statusLine == "HTTP/1.1 200 OK" && event.error == null) probed.countDown()
            })
        var peer: javax.jmdns.JmDNS? = null
        try {
            bonjour.start()
            peer = javax.jmdns.JmDNS.create(address, "codex-control-peer")
            val controlInfo = javax.jmdns.ServiceInfo.create("_carplay-ctrl._tcp.local.",
                controlName, controlPort, 0, 0, mapOf("id" to "02:00:00:00:00:03"))
            android.util.Log.i("CodexApi18Probe", "BONJOUR_PEER_TXT raw=${String(controlInfo.textBytes, Charsets.UTF_8)} decoded=${controlInfo.getPropertyString("id")}")
            check(controlInfo.getPropertyString("id") == "02:00:00:00:00:03") { "Local TXT parser rejected control ID" }
            peer.registerService(controlInfo)
            android.util.Log.i("CodexApi18Probe", "BONJOUR_PEER_REGISTERED name=$controlName address=$address")
            check(resolved.await(15, TimeUnit.SECONDS)) { "Production worker did not emit the actual DNS-SD control endpoint" }
            check(probed.await(5, TimeUnit.SECONDS)) { "Production control HTTP probe did not succeed" }
            return "actualMdnsControlResolved=true workerCallbackReceived=true bluetoothIdVerified=true httpProbeSucceeded=true"
        } finally {
            peer?.close(); bonjour.close(); controlServer.close(); responder.join(2_000)
        }
    }

    fun bonjour(context: Context): String {
        val address = NetworkInterface.getNetworkInterfaces().toList()
            .flatMap { it.inetAddresses.toList() }
            .firstOrNull { it is Inet4Address && !it.isLoopbackAddress }
            ?: error("No emulator IPv4 interface for DNS-SD probe")
        val name = "Codex-API18-Probe"
        val config = AirPlayConfig(name, "02:00:00:00:00:01", "02:00:00:00:00:02",
            "1.0", AirPlayDisplayConfig(320, 240))
        val bonjour = CarPlayBonjour(context, config, AirPlayIdentity.generate(), address.hostAddress,
            useInterfaceMdns = false)
        val peer = WildcardBind.bind(DatagramSocket(null))
        try {
            bonjour.start()
            Thread.sleep(1200)
            val query = ArrayList<Byte>()
            // Request a unicast mDNS response (QU bit), including the service PTR and TXT.
            query.addAll(byteArrayOf(0x12, 0x34, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0).toList())
            for (label in "_airplay._tcp.local".split('.')) {
                val bytes = label.toByteArray(Charsets.US_ASCII)
                query.add(bytes.size.toByte()); query.addAll(bytes.toList())
            }
            query.addAll(byteArrayOf(0, 0, 12, 0x80.toByte(), 1).toList()) // root, PTR, IN + QU
            val request = query.toByteArray()
            peer.soTimeout = 2000
            var sawTxt = false
            repeat(6) { attempt ->
                if (!sawTxt) {
                    // A multicast query reaches both coexisting mDNS listeners; a unicast query
                    // may be delivered only to the system daemon when both share UDP5353.
                    val destination = if (attempt % 2 == 0) InetAddress.getByName("224.0.0.251") else address
                    peer.send(DatagramPacket(request, request.size, destination, 5353))
                    val packet = DatagramPacket(ByteArray(9000), 9000)
                    try {
                        peer.receive(packet)
                        val content = String(packet.data, 0, packet.length, Charsets.ISO_8859_1)
                        sawTxt = content.contains(name) && content.contains("deviceid=") && content.contains("features=")
                    } catch (_: java.net.SocketTimeoutException) { }
                }
            }
            check(sawTxt) { "Production Bonjour did not publish required AirPlay TXT over DNS-SD" }
            return "airPlayService=true deviceIdTxt=true featuresTxt=true"
        } finally { peer.close(); bonjour.close() }
    }

    fun run(codecConfig: ByteArray): String {
        val loopback = InetAddress.getByName("::1")
        val listener = WildcardBind.bind(DatagramSocket(null))
        val sender = DatagramSocket()
        try {
            listener.soTimeout = 2000
            sender.send(DatagramPacket(byteArrayOf(1, 2, 3), 3, loopback, listener.localPort))
            val packet = DatagramPacket(ByteArray(32), 32)
            listener.receive(packet)
            check(packet.length == 3 && packet.address is java.net.Inet6Address)
        } finally { listener.close(); sender.close() }

        val arrived = CountDownLatch(1)
        val error = AtomicReference<Throwable?>()
        val stream = ScreenStream(ByteArray(32))
        try {
            val port = stream.listen(object : ScreenStream.Listener {
                override fun onConfig(codecData: ByteArray) {
                    if (codecData.contentEquals(codecConfig)) arrived.countDown()
                }
                override fun onClosed(cause: Throwable?) { if (cause != null) error.set(cause) }
            })
            val peer = Socket()
            try {
                peer.connect(InetSocketAddress(loopback, port), 2000)
                val header = ByteArray(128)
                repeat(4) { header[it] = (codecConfig.size ushr (it * 8)).toByte() }
                header[4] = 1
                peer.getOutputStream().write(header + codecConfig)
                check(arrived.await(2, TimeUnit.SECONDS)) { "ScreenStream failed to receive IPv6 config: ${error.get()}" }
            } finally { peer.close() }
        } finally { stream.close() }

        val clock = NtpClock()
        val timingPeer = DatagramSocket(null)
        timingPeer.bind(InetSocketAddress(loopback, 0))
        timingPeer.soTimeout = 2500
        try {
            check(clock.listen() > 0)
            clock.start(loopback, timingPeer.localPort)
            val packet = DatagramPacket(ByteArray(128), 128)
            timingPeer.receive(packet)
            check(packet.length >= 32 && packet.data[1].toInt() and 255 == 210)
        } finally { clock.close(); timingPeer.close() }
        return "ipv6Udp=true screenConfigReceived=true timingRequestReceived=true"
    }
}
