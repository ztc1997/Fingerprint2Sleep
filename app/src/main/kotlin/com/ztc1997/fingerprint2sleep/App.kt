package com.ztc1997.fingerprint2sleep

import android.app.Application
import android.content.Context
import com.eightbitlab.rxbus.Bus
import com.orhanobut.logger.LogLevel
import com.orhanobut.logger.Logger
import com.ztc1997.fingerprint2sleep.activity.SettingsActivity
import com.ztc1997.fingerprint2sleep.extra.StartVerifyEvent
import com.ztc1997.fingerprint2sleep.util.QuickActions
import me.dozen.dpreference.DPreference
import org.jetbrains.anko.defaultSharedPreferences

class App : Application() {
    val defaultDPreference by lazy { DPreference(this, BuildConfig.APPLICATION_ID + "_preferences") }

    override fun onCreate() {
        super.onCreate()

        QuickActions.verify3()

        QuickActions.inject(this)

        if (defaultSharedPreferences.contains(SettingsActivity.PREF_ENABLE_FINGERPRINT2SLEEP)) {
            defaultSharedPreferences.edit().putBoolean(SettingsActivity.PREF_ENABLE_FINGERPRINT_QUICK_ACTION,
                    defaultSharedPreferences.getBoolean(SettingsActivity.PREF_ENABLE_FINGERPRINT2SLEEP, false))
                    .remove(SettingsActivity.PREF_ENABLE_FINGERPRINT2SLEEP)
                    .apply()
        }

        Logger.init().logLevel(if (BuildConfig.DEBUG) LogLevel.FULL else LogLevel.NONE)

        Bus.send(StartVerifyEvent(this))
    }
}

val APP_ID = BuildConfig.APPLICATION_ID

val SOURCE_ENC by lazy { byteArrayOf(-79, 39, -66, 32, 67, -125, 38, 79, -67, -38, 30, 57, -122, 44, 47, 56) }

val Context.app: App
    get() = applicationContext as App

val Context.defaultDPreference: DPreference
    get() = app.defaultDPreference

val Any.hashCode: Any get() {
    try {
        return Any::hashCode.invoke(this)
    } catch (e: Exception) {
        return Unit
    }
}