package com.ztc1997.fingerprint2sleep

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.jetbrains.anko.defaultSharedPreferences
import org.jetbrains.anko.startService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (!FP2SService.isRunning &&
                context.defaultSharedPreferences.getBoolean(SettingsActivity.PREF_ENABLE_FINGERPRINT2ACTION, false))
            context.startService<FP2SService>()
    }
}