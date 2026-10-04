package com.shilapi.xcertplay.hud

import android.content.Context
import com.shilapi.xcertplay.iap2.wire.Iap2Frame

/** Nonblocking boundary between phone control messages and vendor services. */
object BydNavigationOutputs {
    /** Recover a journaled interrupted output when the app opens, even before a phone reconnects. */
    fun onAppOpened(context: Context) {
        if (BydStandaloneHudOutput.available(context)) start(context)
        // Read the battery early, so a reading is ready when CarPlay identifies (see batteryStatus).
        if (BydOutputSettings.batteryToIphone(context)) BydBatteryStatus.start(context)
    }
    fun setDiagnosticHold(hold: Boolean) { BydStandaloneHudOutput.syntheticHold = hold }
    @Volatile private var useStandalone = false
    private val standalone = NavigationOutputWorker("diplay-standalone-output", BydStandaloneNavigationBridge::clear)
    private val hud = NavigationOutputWorker("diplay-hud-output", BydHudBridge::clear)
    private val cluster = NavigationOutputWorker("diplay-cluster-output", BydClusterBridge::clear)

    /** The host reports whether its CarPlay map window is on the cluster (see [BydClusterMapPause]). */
    fun setClusterMapShown(shown: Boolean) { BydClusterMapPause.clusterMapShown = shown }

    /** The running CarPlay session: told every second whether the cluster currently shows the map. */
    fun setClusterStreamControl(control: (Boolean) -> Unit) { BydClusterMapPause.streamControl = control }

    fun clearClusterStreamControl(control: (Boolean) -> Unit) {
        if (BydClusterMapPause.streamControl == control) BydClusterMapPause.streamControl = null
    }

    /**
     * The car's battery for the iPhone's vehicle status; starts reading it over adb. The electric
     * vehicle is declared only once a reading is there (see withVehicleStatusFrom).
     */
    fun batteryStatus(context: Context): com.shilapi.xcertplay.transport.VehicleStatusProvider =
        BydBatteryStatus.also { it.start(context) }

    /** The car's wheel speed and gear for the iPhone's dead reckoning; read over adb while asked for. */
    fun wheelSpeed(context: Context): com.shilapi.xcertplay.transport.VehicleSpeedSource =
        BydWheelSpeedSource.attach(context)
    /** Whether the car is in P (read over adb), or null when it cannot tell. Blocking. */
    fun parked(context: Context): Boolean? = BydParkedState.parked(context.applicationContext)

    fun start(context: Context) {
        val app = context.applicationContext
        useStandalone = BydStandaloneHudOutput.available(app)
        if (useStandalone) standalone.start { BydStandaloneNavigationBridge.initialize(app) }
        else {
            hud.start { BydHudBridge.initialize(app) }
            cluster.start { BydClusterBridge.initialize(app) }
        }
        BydClusterMapPause.initialize(app)
        BydClusterSong.attach(app)
    }

    internal fun onFrame(frame: Iap2Frame) {
        if (frame.messageId == ClusterSongState.NOW_PLAYING_UPDATE) {
            BydClusterSong.onFrame(frame)
            return
        }
        if (frame.messageId != BydHudRouteState.ROUTE_GUIDANCE_UPDATE &&
            frame.messageId != BydHudRouteState.ROUTE_GUIDANCE_MANEUVER_UPDATE) return
        val owned = frame // Iap2Frame is immutable and defensively copies its payload.
        if (useStandalone) standalone.submit { BydStandaloneNavigationBridge.onFrame(owned) }
        else {
            hud.submit { BydHudBridge.onFrame(owned) }
            cluster.submit { BydClusterBridge.onFrame(owned) }
        }
    }

    /** The dashboard song setting changed; applies at once. */
    fun clusterSongChanged(enabled: Boolean) = BydClusterSong.settingChanged(enabled)

    /** Best effort while alive; Android does not guarantee callbacks before force-stop. */
    fun endNow() { standalone.clear(); hud.clear(); cluster.clear(); BydClusterSong.end() }
}
