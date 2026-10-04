package com.shilapi.xcertplay.hud

import android.content.Context
import com.shilapi.xcertplay.transport.EvChargingConnectors

/**
 * One user switch for BYD navigation output. On the tested car the windshield HUD mirrors what the
 * instrument cluster receives, so separate HUD/cluster switches cannot behave independently.
 */
object BydOutputSettings {
    private const val PREFS = "diplay_byd_outputs"
    private const val KEY_ENABLED = "navigation_enabled"
    private const val KEY_CLUSTER_STREAM_PAUSE = "cluster_stream_pause"
    private const val KEY_BATTERY_TO_IPHONE = "battery_to_iphone"
    private const val KEY_LOW_CHARGE_PERCENT = "low_charge_percent"
    private const val KEY_CHARGING_CONNECTORS = "charging_connectors"
    private const val KEY_WHEEL_SPEED_TO_IPHONE = "wheel_speed_to_iphone"
    private const val KEY_VIDEO_WHILE_PARKED = "video_while_parked"
    private const val KEY_CLUSTER_SONG = "cluster_song"
    const val DEFAULT_LOW_CHARGE_PERCENT = 20
    val lowChargePresets = listOf(10, 15, 20, 25, 30)

    fun enabled(context: Context): Boolean = prefs(context).getBoolean(KEY_ENABLED, true)

    fun setEnabled(context: Context, enabled: Boolean) = prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()

    /** Ask the iPhone to stop drawing the cluster map while the cluster hides it (needs ADB over network). */
    fun clusterStreamPause(context: Context): Boolean = prefs(context).getBoolean(KEY_CLUSTER_STREAM_PAUSE, false)

    fun setClusterStreamPause(context: Context, enabled: Boolean) =
        prefs(context).edit().putBoolean(KEY_CLUSTER_STREAM_PAUSE, enabled).apply()

    /** Tell the iPhone the car's charge and range (needs ADB over network); applies on the next connection. */
    fun batteryToIphone(context: Context): Boolean = prefs(context).getBoolean(KEY_BATTERY_TO_IPHONE, false)

    fun setBatteryToIphone(context: Context, enabled: Boolean) =
        prefs(context).edit().putBoolean(KEY_BATTERY_TO_IPHONE, enabled).apply()

    /** The charging inlets the iPhone is told about; applies on the next connection. */
    fun chargingConnectors(context: Context): EvChargingConnectors =
        prefs(context).getString(KEY_CHARGING_CONNECTORS, null)
            ?.let { saved -> EvChargingConnectors.entries.firstOrNull { it.name == saved } }
            ?: EvChargingConnectors.CCS2_TYPE2

    fun setChargingConnectors(context: Context, connectors: EvChargingConnectors) =
        prefs(context).edit().putString(KEY_CHARGING_CONNECTORS, connectors.name).apply()

    /** Send wheel speed and gear with the car's GPS (needs ADB over network); applies on the next connection. */
    fun wheelSpeedToIphone(context: Context): Boolean = prefs(context).getBoolean(KEY_WHEEL_SPEED_TO_IPHONE, false)

    fun setWheelSpeedToIphone(context: Context, enabled: Boolean) =
        prefs(context).edit().putBoolean(KEY_WHEEL_SPEED_TO_IPHONE, enabled).apply()
    /** Offer iOS 27 video in car, played only while the gear reads P (needs ADB over network). */
    fun videoWhileParked(context: Context): Boolean = prefs(context).getBoolean(KEY_VIDEO_WHILE_PARKED, false)

    fun setVideoWhileParked(context: Context, enabled: Boolean) =
        prefs(context).edit().putBoolean(KEY_VIDEO_WHILE_PARKED, enabled).apply()

    /** Show the CarPlay song in the dashboard's music card (needs ADB over network); applies at once. */
    fun clusterSong(context: Context): Boolean = prefs(context).getBoolean(KEY_CLUSTER_SONG, false)

    fun setClusterSong(context: Context, enabled: Boolean) =
        prefs(context).edit().putBoolean(KEY_CLUSTER_SONG, enabled).apply()

    /** At or below this charge the iPhone gets the low-range warning. */
    fun lowChargePercent(context: Context): Int = prefs(context).getInt(KEY_LOW_CHARGE_PERCENT, DEFAULT_LOW_CHARGE_PERCENT)

    fun setLowChargePercent(context: Context, percent: Int) =
        prefs(context).edit().putInt(KEY_LOW_CHARGE_PERCENT, percent).apply()

    /** Whether the head unit has a BYD navigation receiver, so settings can hide a switch that cannot work. */
    fun available(context: Context): Boolean =
        BydStandaloneHudOutput.available(context) || installed(context, "com.byd.amapservice") || installed(context, "com.ts.car.someip.service")

    private fun installed(context: Context, pkg: String): Boolean =
        runCatching { context.packageManager.getPackageInfo(pkg, 0) }.isSuccess

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
