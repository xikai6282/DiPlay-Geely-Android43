package com.shilapi.xcertplay.network

import android.content.Context
import java.util.UUID

/** Only radio settings are stored: never network credentials, phone identifiers or addresses. */
internal class P2pConfigurationMemory(context: Context) {
    private val prefs = context.getSharedPreferences("carplay_wifi_p2p_success", Context.MODE_PRIVATE)
    data class Record(val encoded: String, val request: P2pCreationRequest, val stationMHz: Int?)

    fun read(): Record? = runCatching {
        val encoded = prefs.getString("confirmed", null) ?: return null
        val fields = encoded.split('|')
        if (fields.size != 5) return null
        val requested = fields[1].toInt()
        val request = when (fields[0]) {
            "system" -> if (requested == 0) P2pCreationRequest(P2pCreationMode.SYSTEM_DEFAULT) else return null
            "frequency" -> P2pStartupRecovery.rememberedFrequency(requested) ?: return null
            else -> return null
        }
        if (wifiFrequencyMhzToChannel(fields[2].toInt()) == null) return null
        UUID.fromString(fields[4])
        Record(encoded, request, fields[3].toInt().takeIf { it > 0 })
    }.getOrNull()

    fun remember(request: P2pCreationRequest, actualMHz: Int, stationMHz: Int?): Boolean {
        val kind = if (request.mode == P2pCreationMode.SYSTEM_DEFAULT) "system" else "frequency"
        val encoded = "$kind|${request.frequencyMHz ?: 0}|$actualMHz|${stationMHz ?: 0}|${UUID.randomUUID()}"
        return synchronized(prefs) { prefs.edit().putString("confirmed", encoded).commit() }
    }

    /** A late close from an older manager must not erase a newer successful session. */
    fun forget(record: Record): Boolean = runCatching { synchronized(prefs) {
        if (prefs.getString("confirmed", null) != record.encoded) false
        else prefs.edit().remove("confirmed").commit()
    } }.getOrDefault(false)
}
