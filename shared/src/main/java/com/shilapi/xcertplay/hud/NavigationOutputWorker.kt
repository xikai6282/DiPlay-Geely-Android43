package com.shilapi.xcertplay.hud

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/** Each output owns a worker: a blocked vendor service cannot block the phone or the other output. */
internal class NavigationOutputWorker(name: String, private val clearOutput: () -> Unit) {
    private val executor = ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS,
        ArrayBlockingQueue<Runnable>(128), { task -> Thread(task, name).apply { isDaemon = true } })
    private var accepting = false

    @Synchronized fun start(initialize: () -> Unit) {
        if (accepting) return
        accepting = true
        executor.queue.clear()
        executor.execute { runCatching { clearOutput(); initialize() } }
    }

    @Synchronized fun submit(action: () -> Unit) {
        if (!accepting) return
        try {
            executor.execute { runCatching(action) }
        } catch (_: java.util.concurrent.RejectedExecutionException) {
            // Incremental protocol frames cannot safely be dropped individually. Stop this output
            // until the next session, and clear after any in-flight vendor call returns.
            clear()
        }
    }

    @Synchronized fun clear() {
        accepting = false
        executor.queue.clear()
        executor.execute { runCatching(clearOutput) }
    }
}
