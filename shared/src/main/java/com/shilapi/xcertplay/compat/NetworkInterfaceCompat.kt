package com.shilapi.xcertplay.compat

import android.os.Build
import java.net.NetworkInterface

/** NetworkInterface.getIndex was added in API 19. */
val NetworkInterface.indexCompat: Int
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) index else -1
