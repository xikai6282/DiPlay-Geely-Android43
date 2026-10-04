package com.shilapi.xcertplay.hud

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.*
import org.junit.Test

class NavigationOutputWorkerTest {
    private fun await(latch: CountDownLatch) = assertTrue(latch.await(3, TimeUnit.SECONDS))

    @Test fun `blocked HUD does not block caller or independent cluster`() {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val clusterRan = CountDownLatch(1)
        val hud = NavigationOutputWorker("test-hud") {}
        val cluster = NavigationOutputWorker("test-cluster") {}
        hud.start {}
        cluster.start {}
        try {
            hud.submit { entered.countDown(); release.await(3, TimeUnit.SECONDS) }
            await(entered)
            cluster.submit { clusterRan.countDown() }
            await(clusterRan)
        } finally { release.countDown(); hud.clear(); cluster.clear() }
    }

    @Test fun `clear discards pending guidance and rejects late frames until restart`() {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val cleared = CountDownLatch(1)
        val starts = AtomicInteger()
        val frames = AtomicInteger()
        val worker = NavigationOutputWorker("test-cleanup") { if (starts.get() > 0) cleared.countDown() }
        worker.start { starts.incrementAndGet() }
        try {
            worker.submit { entered.countDown(); release.await(3, TimeUnit.SECONDS) }
            await(entered)
            worker.submit { frames.incrementAndGet() }
            worker.clear()
            worker.submit { frames.incrementAndGet() }
            release.countDown()
            await(cleared)
            assertEquals(0, frames.get())
            val resumed = CountDownLatch(1)
            worker.start {}
            worker.submit { resumed.countDown() }
            await(resumed)
        } finally { release.countDown(); worker.clear() }
    }

    @Test fun `overflow clears incremental stream instead of replaying partial route`() {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val cleared = CountDownLatch(1)
        val initialized = CountDownLatch(1)
        val frames = AtomicInteger()
        val worker = NavigationOutputWorker("test-overflow") { if (initialized.count == 0L) cleared.countDown() }
        worker.start { initialized.countDown() }
        await(initialized)
        try {
            worker.submit { entered.countDown(); release.await(3, TimeUnit.SECONDS) }
            await(entered)
            repeat(140) { worker.submit { frames.incrementAndGet() } }
            release.countDown()
            await(cleared)
            assertEquals(0, frames.get())
        } finally { release.countDown(); worker.clear() }
    }
}
