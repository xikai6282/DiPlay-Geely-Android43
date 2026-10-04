package com.shilapi.xcertplay

import android.content.Context
import com.shilapi.xcertplay.compat.BindingState
import com.shilapi.xcertplay.compat.GeelyBluetoothSnapshot
import com.shilapi.xcertplay.compat.ReadState

/** Stores only coarse Bluetooth states and counts for the opt-in diagnostic export. */
internal object GeelyBluetoothDiagnosticSnapshotStore {
    private const val PREFS = "geely_bluetooth_diagnostics"

    fun save(context: Context, androidAdapterState: String, snapshot: GeelyBluetoothSnapshot) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putLong("checked_at", System.currentTimeMillis())
            .putString("android_adapter", androidAdapterState)
            .putString("anw_binding", snapshot.bindingState.name)
            .putString("anw_power_state", snapshot.power.state.name)
            .putString("anw_power", snapshot.power.value?.name)
            .putString("anw_power_failure", snapshot.power.failure?.name)
            .putString("anw_paired_state", snapshot.pairedCount.state.name)
            .putInt("anw_paired_count", snapshot.pairedCount.value ?: -1)
            .putString("anw_paired_failure", snapshot.pairedCount.failure?.name)
            .putString("anw_spp_state", snapshot.sppInitialized.state.name)
            .putBoolean("anw_spp_initialized", snapshot.sppInitialized.value ?: false)
            .putString("anw_spp_failure", snapshot.sppInitialized.failure?.name)
            .putString("ecarx_read_state", snapshot.ecarxEnabled.state.name)
            .putBoolean("ecarx_enabled", snapshot.ecarxEnabled.value ?: false)
            .putString("ecarx_failure", snapshot.ecarxEnabled.failure?.name)
            .apply()
    }

    fun report(context: Context, optInEnabled: Boolean): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val optIn = if (optInEnabled) "enabled" else "disabled"
        if (!prefs.contains("checked_at")) {
            return buildString {
                appendLine("H52 vendor Bluetooth state diagnostics opt-in: $optIn")
                appendLine("No read-only probe has completed; these states are unknown.")
                appendLine("Android Bluetooth adapter: unknown")
                appendLine("ANW service binding: unknown")
                appendLine("ANW Bluetooth power: unknown")
                appendLine("ANW paired-device records: unknown")
                appendLine("ANW SPP initialization: unknown")
                append("ECarX Bluetooth service: unknown / not probed")
            }
        }
        val power = when (prefs.getString("anw_power_state", null)) {
            ReadState.OK.name -> prefs.getString("anw_power", "UNKNOWN") ?: "UNKNOWN"
            ReadState.UNSUPPORTED.name -> "unsupported transaction"
            else -> "unknown (${prefs.getString("anw_power_failure", "read failed") ?: "read failed"})"
        }
        val paired = when (prefs.getString("anw_paired_state", null)) {
            ReadState.OK.name -> "${prefs.getInt("anw_paired_count", -1)} records"
            ReadState.UNSUPPORTED.name -> "unsupported transaction"
            else -> "unknown (${prefs.getString("anw_paired_failure", "read failed") ?: "read failed"})"
        }
        val spp = when (prefs.getString("anw_spp_state", null)) {
            ReadState.OK.name -> if (prefs.getBoolean("anw_spp_initialized", false)) "initialized" else "not initialized"
            ReadState.UNSUPPORTED.name -> "unsupported transaction"
            else -> "unknown (${prefs.getString("anw_spp_failure", "read failed") ?: "read failed"})"
        }
        val ecarx = when (prefs.getString("ecarx_read_state", null)) {
            ReadState.OK.name -> if (prefs.getBoolean("ecarx_enabled", false)) "enabled" else "not enabled or not ready"
            ReadState.UNSUPPORTED.name -> "unsupported transaction"
            else -> "unknown (${prefs.getString("ecarx_failure", "read failed") ?: "read failed"})"
        }
        val checkedAt = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
            .format(java.util.Date(prefs.getLong("checked_at", 0L)))
        return buildString {
            appendLine("H52 vendor Bluetooth state diagnostics opt-in: $optIn")
            appendLine("Last read-only check: $checkedAt${if (optInEnabled) " (cached snapshot; not live)" else " (historical only; opt-in is disabled)"}")
            appendLine("Android Bluetooth adapter at check: ${prefs.getString("android_adapter", "unknown")}")
            appendLine("ANW service binding: ${prefs.getString("anw_binding", BindingState.BIND_FAILED.name)}")
            appendLine("ANW Bluetooth power: $power")
            appendLine("ANW paired-device records: $paired (names and addresses were discarded)")
            appendLine("ANW SPP initialization: $spp (this is not an RFCOMM connection status)")
            append("ECarX Bluetooth service: $ecarx")
        }
    }
}
