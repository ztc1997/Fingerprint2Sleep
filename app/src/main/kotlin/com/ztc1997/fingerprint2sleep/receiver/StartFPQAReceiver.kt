package com.ztc1997.fingerprint2sleep.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ztc1997.fingerprint2sleep.activity.SettingsActivity
import com.ztc1997.fingerprint2sleep.activity.StartFPQAActivity
import com.ztc1997.fingerprint2sleep.service.FPQAService
import org.jetbrains.anko.defaultSharedPreferences

class StartFPQAReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (!FPQAService.isRunning && context.defaultSharedPreferences
                .getBoolean(SettingsActivity.PREF_ENABLE_FINGERPRINT_QUICK_ACTION, false))
            StartFPQAActivity.startActivity(context)
    }
}
