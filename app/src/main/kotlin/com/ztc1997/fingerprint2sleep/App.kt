package com.ztc1997.fingerprint2sleep

import android.app.Application
import org.jetbrains.anko.defaultSharedPreferences

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        if (defaultSharedPreferences.contains(SettingsActivity.PREF_ENABLE_FINGERPRINT2SLEEP)) {
            defaultSharedPreferences.edit().putBoolean(SettingsActivity.PREF_ENABLE_FINGERPRINT_QUICK_ACTION,
                    defaultSharedPreferences.getBoolean(SettingsActivity.PREF_ENABLE_FINGERPRINT2SLEEP, false))
                    .remove(SettingsActivity.PREF_ENABLE_FINGERPRINT2SLEEP)
                    .apply()
        }
    }
}