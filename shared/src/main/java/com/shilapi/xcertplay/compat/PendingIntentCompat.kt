package com.shilapi.xcertplay.compat

import android.app.PendingIntent
import android.os.Build

/** FLAG_IMMUTABLE is API 23; keep the underlying creation flags valid on Android 4.3. */
object PendingIntentCompat {
    fun updateCurrentImmutableFlags(): Int = PendingIntent.FLAG_UPDATE_CURRENT or
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
}
