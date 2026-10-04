package com.shilapi.xcertplay.hud

import android.content.Context
import android.util.Log
import com.shilapi.xcertplay.iap2.wire.Iap2Frame
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

internal object BydStandaloneNavigationBridge {
    private val lock = Any()
    private val route = BydHudRouteState()
    private var context: Context? = null
    private var output: BydStandaloneHudOutput? = null
    private var started = false

    fun initialize(appContext: Context) = synchronized(lock) {
        context = appContext.applicationContext
        if (output == null) output = BydStandaloneHudOutput.create(appContext)
        if (output != null && !started) {
            started = true
            Executors.newSingleThreadScheduledExecutor { task ->
                Thread(task, "diplay-standalone-navi").apply { isDaemon = true }
            }.scheduleWithFixedDelay(::tick, 0, 500, TimeUnit.MILLISECONDS)
        }
    }

    fun onFrame(frame: Iap2Frame) = synchronized(lock) {
        route.accept(frame.messageId, frame.payload)
        Unit
    }

    fun clear() = synchronized(lock) {
        route.clear() // A keepalive must never resurrect ended guidance.
        output?.clear()
        Unit
    }

    private fun tick() = synchronized(lock) {
        if (BydStandaloneHudOutput.syntheticHold) return@synchronized
        try {
            val frame = if (context?.let(BydOutputSettings::enabled) == true)
                route.currentApple()?.let(BydClusterFrame::from) else null
            if (frame == null) output?.clear()
            else output?.update(frame.icon, frame.roundaboutExit, frame.distanceMeters, frame.road)
        } catch (error: Exception) {
            Log.w("DiPlay-Standalone", "HUD update/cleanup will retry", error)
        }
    }
}
