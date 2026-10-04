package com.shilapi.xcertplay.hud

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import com.shilapi.xcertplay.iap2.wire.Iap2Frame
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Publishes CarPlay arrows and distance to the BYD instrument cluster through the stock
 * AMap adapter (com.byd.amapservice), independently of the SOME/IP windshield HUD path.
 */
internal object BydClusterBridge {
    private const val TAG = "DiPlay-BYD-Cluster"
    private const val AMAP_PACKAGE = "com.byd.amapservice"
    private const val AMAP_ACTION = "AUTONAVI_STANDARD_BROADCAST_SEND"
    private const val KEY_GUIDANCE = 10001
    private const val KEY_STATE = 10019
    private const val STATE_ENDED = 9
    private const val KEEPALIVE_TICKS = 5
    private const val FLAG_RECEIVER_INCLUDE_BACKGROUND = 0x01000000 // hidden Intent flag, as sent by stock clients

    private val lock = Any()
    private val route = BydHudRouteState()
    private var context: Context? = null
    private var available = false
    private var factory: BydFactoryNavigationOutput? = null
    private var senderStarted = false
    private var lastSent: BydClusterFrame? = null
    private var ticksSinceSend = 0
    private var guidanceLogged = false

    fun initialize(appContext: Context) = synchronized(lock) {
        if (context != null) return@synchronized
        context = appContext.applicationContext
        available = try {
            appContext.packageManager.getPackageInfo(AMAP_PACKAGE, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
        if (!available && appContext.packageName.endsWith(".hudtest")) {
            factory = BydFactoryNavigationOutput(appContext.applicationContext)
            available = true
        }
        Log.i(TAG, "cluster adapter available=$available factoryTest=${factory != null}")
        if (available && !senderStarted) {
            senderStarted = true
            Executors.newSingleThreadScheduledExecutor { runnable ->
                Thread(runnable, "diplay-byd-cluster").apply { isDaemon = true }
            }.scheduleAtFixedRate(::tick, 1, 1, TimeUnit.SECONDS)
        }
    }

    fun onFrame(frame: Iap2Frame) = synchronized(lock) {
        if (!available) return@synchronized
        when (route.accept(frame.messageId, frame.payload)) {
            BydHudRouteChange.GUIDANCE -> sendCurrentLocked(force = false)
            BydHudRouteChange.CLEAR -> if (lastSent != null) sendEndLocked()
            BydHudRouteChange.NONE -> Unit
        }
    }

    fun clear() = synchronized(lock) {
        route.clear() // Prevent the keepalive from restoring guidance after cleanup.
        if (available && lastSent != null) sendEndLocked()
    }

    private fun tick() = synchronized(lock) {
        // Guidance can expire without a frame (a list that stays empty), so check every second.
        if (lastSent != null && route.currentApple() == null) sendEndLocked()
        else if (++ticksSinceSend >= KEEPALIVE_TICKS) sendCurrentLocked(force = true)
    }

    private fun sendCurrentLocked(force: Boolean) {
        if (context?.let(BydOutputSettings::enabled) != true) {
            if (lastSent != null) sendEndLocked()
            return
        }
        val frame = route.currentApple()?.let(BydClusterFrame::from)
        if (frame == null) {
            if (lastSent != null) sendEndLocked()
            return
        }
        if (!force && frame == lastSent) return
        factory?.let {
            it.update(frame.icon, frame.roundaboutExit, frame.distanceMeters)
            lastSent = frame
            ticksSinceSend = 0
            return
        }
        val intent = baseIntent(KEY_GUIDANCE).apply {
            putExtra("TYPE", 0)
            putExtra("EXTRA_STATE", 0)
            putExtra("EXTRA_IS_FOREGROUND", 0)
            putExtra("NEW_ICON", frame.icon)
            putExtra("ROUNG_ABOUT_NUM", frame.roundaboutExit)
            putExtra("SEG_REMAIN_DIS", frame.distanceMeters)
            putExtra("NEXT_ROAD_NAME", frame.road)
            putExtra("ROUTE_REMAIN_DIS", frame.routeRemainingMeters)
            putExtra("ROUTE_REMAIN_TIME", frame.routeRemainingSeconds)
        }
        if (broadcastLocked(intent)) {
            lastSent = frame
            ticksSinceSend = 0
            if (!guidanceLogged) {
                guidanceLogged = true
                Log.i(TAG, "cluster guidance sent $frame")
            }
        }
    }

    private fun sendEndLocked() {
        factory?.let {
            it.clear()
            lastSent = null
            guidanceLogged = false
            return
        }
        val intent = baseIntent(KEY_STATE).apply {
            putExtra("EXTRA_STATE", STATE_ENDED)
            putExtra("EXTRA_IS_FOREGROUND", 1)
            putExtra("NEW_ICON", -1)
            putExtra("SEG_REMAIN_DIS", -1)
            putExtra("NEXT_ROAD_NAME", "")
            putExtra("ROUTE_REMAIN_DIS", -1)
            putExtra("ROUTE_REMAIN_TIME", -1)
        }
        if (!broadcastLocked(intent)) return
        lastSent = null
        guidanceLogged = false
        Log.i(TAG, "cluster guidance ended")
    }

    // IS_BYD_MAP=true is required: the adapter drops foreign frames while it believes the stock map navigates.
    private fun baseIntent(keyType: Int) = Intent(AMAP_ACTION).apply {
        setPackage(AMAP_PACKAGE)
        addFlags(FLAG_RECEIVER_INCLUDE_BACKGROUND)
        putExtra("KEY_TYPE", keyType)
        putExtra("IS_BYD_MAP", true)
        putExtra("IS_BYD_BAIDU_MAP", false)
    }

    private fun broadcastLocked(intent: Intent): Boolean {
        val appContext = context ?: return false
        return try {
            appContext.sendBroadcast(intent)
            true
        } catch (error: RuntimeException) {
            Log.w(TAG, "cluster broadcast failed", error)
            false
        }
    }
}
