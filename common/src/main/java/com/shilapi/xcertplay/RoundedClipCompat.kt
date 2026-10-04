package com.shilapi.xcertplay

import android.graphics.Outline
import android.os.Build
import android.view.View
import android.view.ViewOutlineProvider
import androidx.annotation.RequiresApi

/**
 * Rounded clipping through ViewOutlineProvider, which is API 21. Keep references to that API in
 * this isolated class so Android 4.3 can load CenterMapOverlay and use its rectangular fallback.
 */
internal object RoundedClipCompat {
    val supported: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun apply(view: View, radius: Float) {
        view.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(target: View, outline: Outline) {
                outline.setRoundRect(0, 0, target.width, target.height, radius)
            }
        }
        view.clipToOutline = true
    }
}
