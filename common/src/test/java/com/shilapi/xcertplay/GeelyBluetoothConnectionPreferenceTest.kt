package com.shilapi.xcertplay

import android.content.Context
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class GeelyBluetoothConnectionPreferenceTest {
    private val context get() = RuntimeEnvironment.getApplication()

    @Before fun clearPreferences() {
        context.getSharedPreferences("xcertplay_airplay", Context.MODE_PRIVATE)
            .edit().clear().commit()
        context.getSharedPreferences("diplay", Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @Test fun connectionOptInDefaultsOffAndIsIndependentFromReadonlyDiagnostics() {
        assertFalse(AirPlayPersistence.loadGeelyBluetoothConnectionEnabled(context))
        assertFalse(AirPlayPersistence.loadGeelyBluetoothDiagnosticsEnabled(context))

        AirPlayPersistence.saveGeelyBluetoothDiagnosticsEnabled(context, true)
        assertTrue(AirPlayPersistence.loadGeelyBluetoothDiagnosticsEnabled(context))
        assertFalse(AirPlayPersistence.loadGeelyBluetoothConnectionEnabled(context))

        AirPlayPersistence.saveGeelyBluetoothConnectionEnabled(context, true)
        assertTrue(AirPlayPersistence.loadGeelyBluetoothConnectionEnabled(context))
        assertTrue(AirPlayPersistence.loadGeelyBluetoothDiagnosticsEnabled(context))

        AirPlayPersistence.saveGeelyBluetoothConnectionEnabled(context, false)
        assertFalse(AirPlayPersistence.loadGeelyBluetoothConnectionEnabled(context))
        assertTrue(AirPlayPersistence.loadGeelyBluetoothDiagnosticsEnabled(context))
    }

    @Test fun savedPhoneMustMatchSelectedBluetoothBackendAfterModeSwitch() {
        assertFalse(DiPlayPreferences.phoneSelectedForTransport(context, geelyBluetooth = false))
        assertFalse(DiPlayPreferences.phoneSelectedForTransport(context, geelyBluetooth = true))

        DiPlayPreferences.savePhone(context, "AA:BB:CC:DD:EE:FF", "iPhone", geelyBluetooth = false)
        assertTrue(DiPlayPreferences.phoneSelectedForTransport(context, geelyBluetooth = false))
        assertFalse(DiPlayPreferences.phoneSelectedForTransport(context, geelyBluetooth = true))

        DiPlayPreferences.savePhone(context, "11:22:33:44:55:66", "Factory paired iPhone", geelyBluetooth = true)
        assertFalse(DiPlayPreferences.phoneSelectedForTransport(context, geelyBluetooth = false))
        assertTrue(DiPlayPreferences.phoneSelectedForTransport(context, geelyBluetooth = true))
        assertTrue(DiPlayPreferences.phoneUsesGeelyBluetooth(context))
    }
}
