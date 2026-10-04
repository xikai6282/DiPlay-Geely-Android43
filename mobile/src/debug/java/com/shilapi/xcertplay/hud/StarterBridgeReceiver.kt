package com.shilapi.xcertplay.hud

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Only the shell (DUMP permission) can provision this temporary test bridge. */
class StarterBridgeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "byd.hud.STARTER_CONFIG" -> {
                BydStarterBridge.configure(context, intent.getStringExtra("token") ?: return)
                resultData = "Starter configured"
            }
            "byd.hud.STARTER_DEMO" -> BydStarterBridge.demonstrate(context)
        }
    }
}
