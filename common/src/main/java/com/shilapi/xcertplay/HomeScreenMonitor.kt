package com.shilapi.xcertplay

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.app.usage.UsageStatsManager
import android.util.Log
import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * Whether a home screen is in front (BYD's normal home, map home or MyCar, or whichever
 * launcher is the default home, such as a third-party car launcher), from the newest resumed
 * activity in the owner's Usage Access events. Overlays and panels are not activities, so they
 * leave the answer as it is.
 */
internal class HomeScreenMonitor(context: Context, private val onChange: (Boolean) -> Unit) {
    private val context = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private var executor: ScheduledExecutorService? = null
    // Poller thread only.
    private var since = 0L
    private var newestTime = 0L
    private var newestPackage: String? = null
    @Volatile private var reported: Boolean? = null
    @Volatile private var homePackages = HOME_PACKAGES

    val running: Boolean get() = executor != null

    /** Main thread. */
    fun start() {
        if (executor != null) return
        // UsageStatsManager is API 21; without it the home screen cannot be observed at all, so no
        // poller is started rather than one that reports a constant answer.
        if (!homeScreenObservable()) {
            Log.i(TAG, "home screen monitoring unavailable below API 21")
            return
        }
        since = System.currentTimeMillis() - FIRST_LOOK_BACK_MILLIS
        newestTime = 0L
        newestPackage = null
        reported = null
        homePackages = HOME_PACKAGES + listOfNotNull(defaultHome(context))
        executor = Executors.newSingleThreadScheduledExecutor { Thread(it, "diplay-home-monitor").apply { isDaemon = true } }
            .also { it.scheduleWithFixedDelay(::poll, 0, POLL_MILLIS, TimeUnit.MILLISECONDS) }
    }

    /** Main thread. */
    fun stop() {
        executor?.shutdownNow()
        executor = null
        main.removeCallbacksAndMessages(null)
    }

    private fun poll() {
        val now = System.currentTimeMillis()
        // UsageStatsManager and UsageEvents are API 21; the holder keeps those types out of this
        // class so Android 4.3 can load it and simply never see a home-screen change.
        if (!homeScreenObservable()) return
        for (event in ModernUsageEvents(context).resumedSince(since, now)) {
            if (event.timeStamp >= newestTime) {
                newestTime = event.timeStamp
                newestPackage = event.packageName
            }
        }
        // Overlap, because events can arrive a little late.
        since = (now - OVERLAP_MILLIS).coerceAtLeast(since)
        val visible = newestPackage in homePackages
        if (visible != reported) {
            reported = visible
            main.post { if (executor != null) onChange(visible) }
        }
    }

    companion object {
        private const val TAG = "DiPlay-HomeMonitor"

        /** UsageStatsManager arrived in API 21; the monitor needs it to answer at all. */
        private fun homeScreenObservable(): Boolean =
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP

        private const val POLL_MILLIS = 500L
        private const val OVERLAP_MILLIS = 2_000L
        private const val FIRST_LOOK_BACK_MILLIS = 10 * 60_000L

        // BYD's home list (Launcher3 HomeHelper): MyCar, the normal home, and the map home.
        val HOME_PACKAGES = setOf("com.android.launcher3", "com.byd.launchermap", "com.byd.naviauto", "com.byd.mycar")

        fun hasAccess(context: Context): Boolean = DiLink51ClusterMonitor.hasAccess(context)

        /** The launcher Android uses as home now, unless that is the chooser or DiPlay itself. */
        fun defaultHome(context: Context): String? = runCatching {
            context.packageManager.resolveActivity(
                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
                PackageManager.MATCH_DEFAULT_ONLY,
            )?.activityInfo?.packageName
        }.getOrNull()?.takeIf { it != "android" && it != context.packageName }
    }
}

/**
 * The API 21+ half of the home-screen monitor. UsageStatsManager and UsageEvents postdate Android
 * 4.3, so they are named only here; below that level [resumedSince] yields nothing and the caller
 * simply keeps its last answer.
 */
@androidx.annotation.RequiresApi(android.os.Build.VERSION_CODES.LOLLIPOP)
private class ModernUsageEvents(appContext: Context) {
    private val context = appContext.applicationContext
    data class Resumed(val packageName: String?, val timeStamp: Long)

    fun resumedSince(since: Long, now: Long): List<Resumed> = runCatching {
        val stats = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return emptyList()
        val events = stats.queryEvents(since, now)
        val result = ArrayList<Resumed>()
        // MOVE_TO_FOREGROUND is ACTIVITY_RESUMED (API 29) under its older name.
        @Suppress("DEPRECATION")
        val resumed = android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND
        val event = android.app.usage.UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == resumed) result.add(Resumed(event.packageName, event.timeStamp))
        }
        result
    }.getOrDefault(emptyList())
}
