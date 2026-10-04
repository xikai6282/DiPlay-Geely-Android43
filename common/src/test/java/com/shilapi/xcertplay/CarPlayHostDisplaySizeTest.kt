package com.shilapi.xcertplay

import android.graphics.Matrix
import android.os.Looper
import android.view.MotionEvent
import android.view.Surface
import android.view.TextureView
import android.view.View
import com.shilapi.xcertplay.airplay.*
import com.shilapi.xcertplay.media.AndroidMediaSink
import com.shilapi.xcertplay.media.CarPlayVideoLayout
import com.shilapi.xcertplay.orchestration.CarPlayController
import com.shilapi.xcertplay.orchestration.CarPlayRuntimeConfig
import com.shilapi.xcertplay.orchestration.MfiTarget
import com.shilapi.xcertplay.transport.Iap2IdentificationConfig
import java.time.Duration
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.util.concurrent.PausedExecutorService
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowLog

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
@LooperMode(LooperMode.Mode.PAUSED)
class CarPlayHostDisplaySizeTest {
    private lateinit var activity: CarPlayHostActivity
    private val sizeClass = Class.forName("com.shilapi.xcertplay.CarPlayHostActivity\$DisplaySize")

    @Before fun setUp() {
        activity = Robolectric.buildActivity(CarPlayHostActivity::class.java).get()
        (getField("teardownExecutor") as ExecutorService).shutdownNow()
        setField("teardownExecutor", PausedExecutorService())
        CarPlayBackgroundSession::class.java.getDeclaredField("owner").apply { isAccessible = true }
            .set(CarPlayBackgroundSession, activity)
        setField("activeDisplaySize", size(1920, 990))
    }

    @After fun tearDown() {
        (getField("shuttingDown") as AtomicBoolean).set(true)
        (getField("teardownExecutor") as ExecutorService).shutdownNow()
        (getField("airPlayCommandExecutor") as ExecutorService).shutdownNow()
        CarPlayBackgroundSession.clear()
    }

    @Test fun surroundViewOpenAndCloseKeepsTheNegotiatedCanvas() {
        val display = startSession()
        applySize(1920, 942)
        assertEquals(size(1920, 942), getField("activeDisplaySize"))
        applySize(1920, 990)
        assertSame(display, getField("sessionDisplay"))
        assertEquals(0, getField("restartGeneration"))
        assertFalse(getField("handshakeResetInProgress") as Boolean)
        assertEquals(2, keepLogs())
    }

    @Test fun aNarrowWindowIsNotTreatedAsScreenRotation() {
        val display = startSession()
        applySize(700, 990)
        assertSame(display, getField("sessionDisplay"))
        assertEquals(0, getField("restartGeneration"))
        assertEquals(1, keepLogs())
    }

    @Test fun connectingInANarrowWindowRebuildsWhenTheCameraCloses() {
        startSession(windowWidth = 700)
        applySize(1920, 990)
        assertEquals(size(1920, 990), getField("activeDisplaySize"))
        assertEquals(1, getField("restartGeneration"))
        assertTrue(getField("handshakeResetInProgress") as Boolean)
        assertNull(getField("sessionDisplay"))
    }

    @Test fun connectingInAReducedHeightWindowRebuildsWhenTheCameraCloses() {
        startSession(windowHeight = 942)
        applySize(1920, 990)
        assertEquals(1, getField("restartGeneration"))
        assertNull(getField("sessionDisplay"))
    }

    @Test fun aScaledDownCanvasKeepsTheSessionWhenTheOriginalWindowReturns() {
        val display = startSession(canvasWidth = 1536, canvasHeight = 792)
        applySize(700, 990)
        applySize(1920, 990)
        assertSame(display, getField("sessionDisplay"))
        assertEquals(0, getField("restartGeneration"))
    }

    @Test fun aScaledUpCanvasStillRebuildsWhenTheStartupWindowGrows() {
        startSession(windowWidth = 700, canvasWidth = 1400, canvasHeight = 1980)
        applySize(1000, 990)
        assertEquals(1, getField("restartGeneration"))
        assertNull(getField("sessionDisplay"))
    }

    @Test fun actualScreenRotationStillRebuildsTheSession() {
        startSession(rotation = Surface.ROTATION_90)
        applySize(990, 1920)
        assertEquals(1, getField("restartGeneration"))
        assertTrue(getField("handshakeResetInProgress") as Boolean)
        assertNull(getField("sessionDisplay"))
    }

    @Test fun rotationIsHandledEvenIfTheViewSizeIsUnchanged() {
        startSession(rotation = Surface.ROTATION_180)
        scheduleSize(1920, 990)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(600))
        assertEquals(1, getField("restartGeneration"))
    }

    @Test fun anExplicitBarLayoutChangeStillRebuildsTheSession() {
        startSession()
        setField("hideTopBar", false)
        applySize(1920, 942)
        assertEquals(1, getField("restartGeneration"))
    }

    @Test fun quickOpenAndCloseCancelsThePendingShrink() {
        startSession()
        scheduleSize(700, 990)
        scheduleSize(1920, 990)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(600))
        assertEquals(size(1920, 990), getField("activeDisplaySize"))
        assertNull(getField("pendingDisplaySize"))
        assertEquals(0, getField("restartGeneration"))
    }

    @Test fun anOngoingHandshakeResetOnlyRecordsTheNewSize() {
        setField("handshakeResetInProgress", true)
        applySize(700, 990)
        assertEquals(size(700, 990), getField("activeDisplaySize"))
        assertEquals(0, getField("restartGeneration"))
    }

    @Test fun initialSizeDetectionKeepsTheNormalStartupPath() {
        setField("activeDisplaySize", null)
        applySize(1920, 990)
        assertEquals(size(1920, 990), getField("activeDisplaySize"))
        assertEquals(0, getField("restartGeneration"))
        assertEquals(0, keepLogs())
    }

    @Test fun resizeWithoutASessionKeepsTheExistingRestartPath() {
        applySize(700, 990)
        assertEquals(1, getField("restartGeneration"))
    }

    @Test fun textureTransformFitsTheNegotiatedCanvas() {
        startSession()
        val view = TextureView(activity).apply { layout(0, 0, 1920, 942) }
        setField("videoView", view)
        activity.javaClass.getDeclaredMethod("updateVideoLayout", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
            .apply { isAccessible = true }.invoke(activity, 1920, 942)
        val points = floatArrayOf(0f, 0f, 1920f, 942f)
        view.getTransform(Matrix()).mapPoints(points)
        val content = CarPlayVideoLayout.fit(1920, 990, 1920, 942)
        assertArrayEquals(floatArrayOf(content.left, content.top, content.left + content.width,
            content.top + content.height), points, 0.001f)
    }

    @Test fun touchesStartingInABarStaySuppressedUntilRelease() {
        startSession()
        val view = View(activity).apply { layout(0, 0, 1920, 942) }
        touch(view, MotionEvent.ACTION_DOWN, 1f, 471f)
        assertEquals(true, getField("touchOutsideContent"))
        touch(view, MotionEvent.ACTION_MOVE, 960f, 471f)
        assertEquals(true, getField("touchOutsideContent"))
        touch(view, MotionEvent.ACTION_UP, 960f, 471f)
        assertEquals(false, getField("touchOutsideContent"))
        touch(view, MotionEvent.ACTION_DOWN, 960f, 471f)
        assertEquals(false, getField("touchOutsideContent"))
    }

    @Test fun adoptingABackgroundSessionPreservesItsCanvasOnResize() {
        val display = CarPlaySessionDisplay(1536, 792, Surface.ROTATION_0, true, true, 1920, 990)
        val sink = AndroidMediaSink()
        val controller = CarPlayController(activity,
            CarPlayRuntimeConfig(mfiTarget = MfiTarget.LOCAL, identification = Iap2IdentificationConfig(
                name = "test", modelIdentifier = "test", manufacturer = "test", serialNumber = "test",
                firmwareVersion = "1", hardwareVersion = "1", carPlayUsbInterfaceNumber = 3)),
            AirPlayConfig(deviceName = "test", deviceId = "02:00:00:00:00:02", btMac = "02:00:00:00:00:01",
                sourceVersion = "1", main = AirPlayDisplayConfig(widthPixels = 1536, heightPixels = 792)),
            AirPlayIdentity.generate(), PairingStore(), object : AirPlaySessionListener {},
            object : AirPlayMediaHandler {}, {})
        try {
            CarPlayBackgroundSession.store(controller, sink, 1920, 990, Any(), display) {}
            val adopted = activity.javaClass.getDeclaredMethod("adoptBackgroundSession")
                .apply { isAccessible = true }.invoke(activity)
            assertEquals(true, adopted)
            applySize(700, 990)
            assertSame(controller, getField("controller"))
            assertSame(sink, getField("sink"))
            assertEquals(display, getField("sessionDisplay"))
            assertEquals(display, CarPlayBackgroundSession.snapshot()?.display)
            assertFalse(controller.isClosed())
            assertEquals(0, getField("restartGeneration"))
            applySize(1920, 990)
            assertSame(display, getField("sessionDisplay"))
            assertEquals(0, getField("restartGeneration"))
            applySize(2000, 990)
            assertEquals(1, getField("restartGeneration"))
            assertNull(getField("sessionDisplay"))
        } finally {
            controller.close()
            controller.awaitClosed(1000)
            sink.close()
        }
    }

    private fun startSession(
        rotation: Int = Surface.ROTATION_0,
        windowWidth: Int = 1920,
        windowHeight: Int = 990,
        canvasWidth: Int = windowWidth,
        canvasHeight: Int = windowHeight,
    ): CarPlaySessionDisplay =
        CarPlaySessionDisplay(canvasWidth, canvasHeight, rotation, true, true, windowWidth, windowHeight).also {
            setField("activeDisplaySize", size(windowWidth, windowHeight))
            setField("sessionDisplay", it)
        }

    private fun keepLogs(): Int = ShadowLog.getLogsForTag("xcertplay-usb").count {
        it.msg.contains("keeping CarPlay session")
    }

    private fun size(width: Int, height: Int): Any = sizeClass
        .getDeclaredConstructor(Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
        .apply { isAccessible = true }.newInstance(width, height)

    private fun applySize(width: Int, height: Int) {
        activity.javaClass.getDeclaredMethod("applyDisplaySize", sizeClass)
            .apply { isAccessible = true }.invoke(activity, size(width, height))
    }

    private fun scheduleSize(width: Int, height: Int) {
        activity.javaClass.getDeclaredMethod("scheduleDisplaySize", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
            .apply { isAccessible = true }.invoke(activity, width, height)
    }

    private fun touch(view: View, action: Int, x: Float, y: Float) {
        val event = MotionEvent.obtain(0, 0, action, x, y, 0)
        try {
            activity.javaClass.getDeclaredMethod("onHostTouch", View::class.java, MotionEvent::class.java)
                .apply { isAccessible = true }.invoke(activity, view, event)
        } finally {
            event.recycle()
        }
    }

    private fun getField(name: String): Any? = activity.javaClass.getDeclaredField(name)
        .apply { isAccessible = true }.get(activity)

    private fun setField(name: String, value: Any?) {
        activity.javaClass.getDeclaredField(name).apply { isAccessible = true }.set(activity, value)
    }
}
