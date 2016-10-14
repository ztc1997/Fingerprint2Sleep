package com.ztc1997.fingerprint2sleep

import android.app.Application
import android.content.Context
import me.dozen.dpreference.DPreference
import org.jetbrains.anko.defaultSharedPreferences

class App : Application() {
    val defaultDPreference by lazy { DPreference(this, BuildConfig.APPLICATION_ID + "_preferences") }

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

val Context.app: App
    get() = applicationContext as App

val Context.defaultDPreference: DPreference
    get() = app.defaultDPreference