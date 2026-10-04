package com.shilapi.xcertplay.media

import org.junit.Assert.*
import org.junit.Test

class GeelyAudioCapabilitiesTest {
    private val h52 = mapOf("STREAM_CARPLAYAUDIO" to 23, "STREAM_NAVI_TTS" to 11,
        "MODE_CARPLAYAUDIO" to 14, "STREAM_NAVI_ALERT" to 25)

    @Test fun h52ProvidesSeparateMediaSpeechAndAlertRoutes() {
        val profile = GeelyAudioCapabilities.fromFields(h52::get)!!
        assertEquals(23, profile.carPlay)
        assertEquals(11, profile.navigationStream("default", separateAlerts = true))
        assertEquals(11, profile.navigationStream("compatibility", separateAlerts = true))
        assertEquals(25, profile.navigationStream("alert", separateAlerts = true))
        assertEquals(11, profile.navigationStream("alert", separateAlerts = false))
    }

    @Test fun stockAndroidDoesNotEnableVendorRouting() {
        assertNull(GeelyAudioCapabilities.fromFields { null })
    }

    @Test fun h41NavigationConstantAloneIsInsufficient() {
        assertNull(GeelyAudioCapabilities.fromFields(mapOf("STREAM_NAVI_TTS" to 11)::get))
    }

    @Test fun differingVendorMediaNumberIsRejected() {
        assertNull(GeelyAudioCapabilities.fromFields((h52 + ("STREAM_CARPLAYAUDIO" to 12))::get))
    }

    @Test fun missingOrWrongModeDoesNotEnableProfile() {
        assertNull(GeelyAudioCapabilities.fromFields((h52 - "MODE_CARPLAYAUDIO")::get))
        assertNull(GeelyAudioCapabilities.fromFields((h52 + ("MODE_CARPLAYAUDIO" to 3))::get))
    }

    @Test fun missingAlertExtensionFallsBackToSpeech() {
        val profile = GeelyAudioCapabilities.fromFields((h52 - "STREAM_NAVI_ALERT")::get)!!
        assertNull(profile.navigationAlert)
        assertEquals(11, profile.navigationStream("alert", separateAlerts = true))
    }

    @Test fun unrelatedAlertNumberIsNotExposed() {
        assertNull(GeelyAudioCapabilities.fromFields((h52 + ("STREAM_NAVI_ALERT" to 9))::get)!!.navigationAlert)
    }

    @Test fun alertStreamIsUsedOnlyForAlertAudioType() {
        val profile = GeelyAudioCapabilities.fromFields(h52::get)!!
        assertEquals(11, AudioStreamRouting.override(AudioChannel.NAVIGATION, "default", profile, true, 0, 0))
        assertEquals(11, AudioStreamRouting.override(AudioChannel.NAVIGATION, "compatibility", profile, true, 0, 0))
        assertEquals(25, AudioStreamRouting.override(AudioChannel.NAVIGATION, "alert", profile, true, 0, 0))
        assertEquals(11, AudioStreamRouting.override(AudioChannel.NAVIGATION, "alert", profile, false, 0, 0))
    }

    @Test fun automaticRouteAndRealVoiceCallStreamZeroAreDistinct() {
        assertNull(AudioStreamRouting.override(AudioChannel.PHONE, "telephony", null, false, 0, 0))
        assertEquals(0, AudioStreamRouting.legacyStreamType(AudioChannel.PHONE, null))
        assertEquals(0, AudioStreamRouting.legacyStreamType(AudioChannel.MEDIA, 0))
        assertNull(AudioStreamRouting.override(AudioChannel.MEDIA, "media", null, false, 0, 0))
        assertEquals(3, AudioStreamRouting.legacyStreamType(AudioChannel.MEDIA, null))
    }
}
