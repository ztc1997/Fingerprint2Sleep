package com.ztc1997.fingerprint2sleep

import android.app.Application
import android.content.Context
import com.eightbitlab.rxbus.Bus
import com.orhanobut.logger.LogLevel
import com.orhanobut.logger.Logger
import com.ztc1997.fingerprint2sleep.activity.SettingsActivity
import com.ztc1997.fingerprint2sleep.extension.KEY_PART_1
import com.ztc1997.fingerprint2sleep.extra.KEY_PART_2
import com.ztc1997.fingerprint2sleep.extra.StartVerifyEvent
import com.ztc1997.fingerprint2sleep.quickactions.NonXposedQuickActions
import com.ztc1997.fingerprint2sleep.receiver.KEY_PART_3
import com.ztc1997.fingerprint2sleep.util.RC4
import me.dozen.dpreference.DPreference
import org.jetbrains.anko.defaultSharedPreferences

class App : Application() {
    companion object {
        val LICENSE_KEY by lazy {
            RC4.decry_RC4(KEY_PART_0 + KEY_PART_1 + KEY_PART_2 + KEY_PART_3,
                    NonXposedQuickActions.CHECK_BYTES)
        }

        val IAP_SKU_DONATE = "com.ztc1997.fingerprint2sleep.donate_1_99"//"android.test.purchased"
    }

    val defaultDPreference by lazy { DPreference(this, BuildConfig.APPLICATION_ID + "_preferences") }

    override fun onCreate() {
        super.onCreate()

        NonXposedQuickActions.verify3()

        NonXposedQuickActions.inject(this)

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

val KEY_PART_0 by lazy { "${SettingsActivity.SOURCE[5]}${SettingsActivity.SOURCE[14]}${SettingsActivity.SOURCE[2]}${SettingsActivity.SOURCE[4]}${SettingsActivity.SOURCE[13]}${SettingsActivity.SOURCE[0]}${SettingsActivity.SOURCE[4]}${SettingsActivity.SOURCE[10]}${SettingsActivity.SOURCE[0]}${SettingsActivity.SOURCE[10]}${SettingsActivity.SOURCE[14]}${SettingsActivity.SOURCE[2]}${SettingsActivity.SOURCE[11]}${SettingsActivity.SOURCE[7]}" }