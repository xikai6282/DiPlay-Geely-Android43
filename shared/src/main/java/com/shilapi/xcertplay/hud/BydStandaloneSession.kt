package com.shilapi.xcertplay.hud

/** Single-worker lifecycle; all vehicle writes and persistence stay off the phone thread. */
internal class BydStandaloneSession(
    private val send: (String) -> Unit,
    private val rememberPendingClear: (Boolean) -> Unit,
    needsRecovery: Boolean = false,
    private val nanoTime: () -> Long = System::nanoTime,
) {
    private var showing = needsRecovery
    private var recovering = needsRecovery
    private var lastPacket: String? = null
    private var lastSendNs = 0L

    fun update(icon: Int, exit: Int, distanceMeters: Int, road: String? = null) {
        if (recovering) clear()
        val packet = BydStandalonePackets.guidance(icon, exit, distanceMeters, road)
        if (packet == null) { clear(); return }
        try {
            if (!showing) {
                // Commit before publishing a start, so the next app launch can recover a crash.
                rememberPendingClear(true)
                showing = true
                send(BydStandalonePackets.start())
            }
            val now = nanoTime()
            if (lastPacket != packet || now - lastSendNs >= 1_000_000_000L) {
                send(packet)
                lastPacket = packet
                lastSendNs = now
            }
        } catch (error: Exception) {
            // A failed update may follow a successful start; clear before trying again.
            recovering = showing
            runCatching { clear() }
            throw error
        }
    }

    fun clear() {
        if (!showing) return
        recovering = true
        send(BydStandalonePackets.clear())
        // sendBroadcast returning is dispatch success, not hardware acknowledgement.
        rememberPendingClear(false)
        showing = false
        recovering = false
        lastPacket = null
        lastSendNs = 0L
    }
}
