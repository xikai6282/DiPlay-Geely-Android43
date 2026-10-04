package com.shilapi.xcertplay

import android.content.Context
import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.SystemClock
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** Uses the owner's standard Android Usage Access grant, never the privileged BYD API. */
/**
 * AppOpsManager is API 19 and UsageStatsManager API 21, so neither type is named here: a 4.3 unit
 * must be able to load this class in order to report "cluster map unavailable" rather than failing
 * verification. The monitor is simply disabled below those levels.
 */
private val usageStatsAvailable: Boolean
    @androidx.annotation.ChecksSdkIntAtLeast(api = 21)
    get() = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP

/**
 * The API 21+ half of the cluster monitor. UsageStatsManager, UsageEvents and AppOpsManager all
 * postdate Android 4.3, and naming them anywhere in DiLink51ClusterMonitor would fail class
 * verification there, so everything that touches them lives in this nested holder instead.
 */
/**
 * The API 21+ half of the cluster monitor.
 *
 * UsageStatsManager, UsageEvents and AppOpsManager all postdate Android 4.3, so naming them in
 * DiLink51ClusterMonitor would fail class verification there. Everything that touches them lives in
 * this holder, which reports plain values: the outer monitor keeps its original filtering,
 * deduplication, overlap and state logic unchanged.
 */
@androidx.annotation.RequiresApi(21)
private class ModernUsage(context: Context) {
    private val appContext = context.applicationContext

    companion object {
        /** Re-exported so the outer monitor's reset check does not name an API-21 type. */
        val DEVICE_STARTUP: Int get() = android.app.usage.UsageEvents.Event.DEVICE_STARTUP
        val DEVICE_SHUTDOWN: Int get() = android.app.usage.UsageEvents.Event.DEVICE_SHUTDOWN
    }

    /** One UsageEvents entry, reduced to what the monitor needs. */
    data class Event(
        val packageName: String?,
        val className: String?,
        val instanceId: Int,
        val type: Int,
        val timeStamp: Long,
    )

    /** Uses the real AppOpsManager constants; their numeric values are never assumed here. */
    fun hasAccess(): Boolean = runCatching {
        val ops = appContext.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        appContext.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return false
        ops.checkOpNoThrow(
            android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            appContext.packageName,
        ) == android.app.AppOpsManager.MODE_ALLOWED
    }.getOrDefault(false)

    /**
     * Every usage event in the window, unfiltered, so the caller applies the same acceptance rules
     * it always did. getInstanceId is hidden on some builds and stays optional: without it an
     * overlapping recreation is conservatively hidden, which is the original behaviour.
     */
    fun eventsSince(since: Long, now: Long): List<Event> = runCatching {
        val stats = appContext.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return emptyList()
        val events = stats.queryEvents(since, now)
        val result = ArrayList<Event>()
        // MOVE_TO_FOREGROUND is ACTIVITY_RESUMED (API 29) under its older name.
        @Suppress("DEPRECATION")
        val moveToForeground = android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND
        val deviceStartup = android.app.usage.UsageEvents.Event.DEVICE_STARTUP
        val deviceShutdown = android.app.usage.UsageEvents.Event.DEVICE_SHUTDOWN
        val instanceIdMethod = runCatching {
            android.app.usage.UsageEvents.Event::class.java.getMethod("getInstanceId")
        }.getOrNull()
        val event = android.app.usage.UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val type = event.eventType
            if (type != moveToForeground && type != deviceStartup && type != deviceShutdown &&
                !ClusterActivityState.accepted(event.packageName, event.className)
            ) {
                continue
            }
            result.add(
                Event(
                    packageName = event.packageName,
                    className = event.className,
                    instanceId = runCatching { instanceIdMethod?.invoke(event) as? Int }
                        .getOrNull() ?: 0,
                    type = type,
                    timeStamp = event.timeStamp,
                ),
            )
        }
        result
    }.getOrDefault(emptyList())
}

internal class DiLink51ClusterMonitor(context: Context, private val onState: (ClusterActivityState.Snapshot) -> Unit) {
    /** Null below API 21; the cluster map is simply unavailable on those units. */
    private val modern: ModernUsage? =
        if (usageStatsAvailable) runCatching { ModernUsage(context) }.getOrNull() else null
    private val context = context.applicationContext
    private val handler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadScheduledExecutor()
    private var state = ClusterActivityState()
    private var since = bootTime()
    private val seen = linkedMapOf<EventKey, Long>()
    @Volatile private var stopped = false
    private data class EventKey(val pkg: String?, val name: String?, val id: Int, val type: Int, val time: Long)

    fun start() {
        if (!usageStatsAvailable) return
        executor.scheduleWithFixedDelay({ poll() }, 0, 500, TimeUnit.MILLISECONDS)
    }

    fun stop() {
        stopped = true
        executor.shutdownNow()
        handler.removeCallbacksAndMessages(null)
    }

    private fun bootTime() = (System.currentTimeMillis() - SystemClock.elapsedRealtime()).coerceAtLeast(0)

    private fun poll() {
        if (stopped || !usageStatsAvailable) return
        val now = System.currentTimeMillis()
        val snapshot = try {
            val usage = modern
            if (usage == null || !usage.hasAccess()) {
                state = ClusterActivityState()
                seen.clear()
                since = bootTime()
            } else {
                if (now < since) { // A vehicle clock correction must not leave an old overlay visible.
                    state = ClusterActivityState()
                    seen.clear()
                    since = bootTime()
                }
                for (event in usage.eventsSince(since, now)) {
                    val reset = event.type == ModernUsage.DEVICE_SHUTDOWN ||
                        event.type == ModernUsage.DEVICE_STARTUP
                    if (!reset && !ClusterActivityState.accepted(event.packageName, event.className)) continue
                    val key = EventKey(
                        event.packageName, event.className,
                        event.instanceId, event.type, event.timeStamp,
                    )
                    if (seen.put(key, event.timeStamp) == null) {
                        state.event(
                            event.packageName, event.className,
                            event.instanceId, event.type, event.timeStamp,
                        )
                    }
                }
                // Overlap handles asynchronously delivered events; deduplicate only the allowed ones.
                since = (now - 2_000).coerceAtLeast(bootTime())
                seen.entries.removeAll { it.value < since }
            }
            state.snapshot()
        } catch (_: RuntimeException) {
            state = ClusterActivityState()
            seen.clear()
            since = bootTime()
            state.snapshot() // No reliable signal means no overlay.
        }
        handler.post { if (!stopped) onState(snapshot) }
    }

    companion object {
        /**
         * UsageStatsManager is API 21, so older units report "no access" without touching any
         * API-21 class, which is what keeps this companion loadable on Android 4.3.
         */
        fun hasAccess(context: Context): Boolean {
            if (!usageStatsAvailable) return false
            return runCatching { ModernUsage(context).hasAccess() }.getOrDefault(false)
        }
    }
}
