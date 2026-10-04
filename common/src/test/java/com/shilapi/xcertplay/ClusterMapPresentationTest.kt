package com.shilapi.xcertplay

import android.hardware.display.DisplayManager
import android.view.Display
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowDisplayManager
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29], manifest = Config.NONE)
class ClusterMapPresentationTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private val manager get() = context.getSystemService(DisplayManager::class.java)

    private fun display(name: String): Int {
        // Display.TYPE_VIRTUAL (5) is hidden from the public Android SDK.
        val id = ShadowDisplayManager.addDisplay("w960dp-h360dp", 5)
        shadowOf(manager.getDisplay(id)).apply {
            setName(name)
            setFlags(Display.FLAG_PRESENTATION)
        }
        return id
    }

    @Test fun legacyFirmwareKeepsOriginalBaseDisplayPreference() {
        val base = display("fission_bg_XDJAScreenProjection")
        val shared = display("shared_fission_bg_XDJAScreenProjection_0")
        try {
            assertEquals(base, ClusterMapPresentation.findDisplay(context)?.displayId)
        } finally {
            ShadowDisplayManager.removeDisplay(shared)
            ShadowDisplayManager.removeDisplay(base)
        }
    }

    @Test fun baseDisplayStillWorksWhenNoSharedLayerExists() {
        val base = display("fission_bg_XDJAScreenProjection")
        try {
            assertEquals(base, ClusterMapPresentation.findDisplay(context)?.displayId)
        } finally {
            ShadowDisplayManager.removeDisplay(base)
        }
    }

    @Test fun unrelatedPresentationDisplayIsNotUsedForTheCluster() {
        val other = display("Passenger display")
        try {
            assertNull(ClusterMapPresentation.findDisplay(context))
        } finally {
            ShadowDisplayManager.removeDisplay(other)
        }
    }
}
