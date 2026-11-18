package com.example.shakelight

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED &&
            Prefs.get(context).getBoolean(Prefs.KEY_ENABLED, false)
        ) {
            ShakeService.start(context)
        }
    }
}
