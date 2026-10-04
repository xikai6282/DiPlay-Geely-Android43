package com.shilapi.xcertplay.compat

import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress

/**
 * Wildcard binds for the CarPlay listeners.
 *
 * Android 4.3 cannot bind an IPv6 socket to the unspecified address. libcore's IoBridge treats any
 * Inet6Address whose scopeId is 0 as needing a NetworkInterface lookup and throws when it finds
 * none, so "::", a zero-byte Inet6Address, and Inet6Address.ANY all fail identically — building the
 * address explicitly does not help, because the scope id is still 0.
 *
 * A socket created on Android defaults to AF_INET6 with IPV6_V6ONLY off, so binding the IPv4
 * wildcard accepts IPv6 traffic as well. That is what these helpers do, and it keeps the IPv6
 * capability the CarPlay transport needs without a scope id.
 */
object WildcardBind {

    /** The IPv4 any-address, used as the dual-stack wildcard on Android. */
    fun anyAddress(): InetAddress = InetAddress.getByAddress(ByteArray(4))

    /** Binds [socket] to the dual-stack wildcard on [port]; 0 selects an ephemeral port. */
    fun bind(socket: DatagramSocket, port: Int = 0, reuseAddress: Boolean = true): DatagramSocket {
        socket.reuseAddress = reuseAddress
        socket.bind(InetSocketAddress(anyAddress(), port))
        return socket
    }

    /** Binds [server] to the dual-stack wildcard on [port]; 0 selects an ephemeral port. */
    fun bind(server: java.net.ServerSocket, port: Int = 0, backlog: Int = 50): java.net.ServerSocket {
        server.reuseAddress = true
        server.bind(InetSocketAddress(anyAddress(), port), backlog)
        return server
    }

    /**
     * A listening address for outbound-style sockets that must accept IPv6 peers. The IPv4 wildcard
     * is returned because Android's dual-stack socket already carries IPv6 traffic.
     */
    fun listenAddress(): InetAddress = anyAddress()
}