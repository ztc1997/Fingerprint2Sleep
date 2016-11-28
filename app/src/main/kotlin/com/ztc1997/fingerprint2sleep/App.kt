package com.ztc1997.fingerprint2sleep

import android.app.Application
import android.app.Fragment
import android.content.Context
import com.orhanobut.logger.LogLevel
import com.orhanobut.logger.Logger
import com.ztc1997.fingerprint2sleep.quickactions.NonXposedQuickActions
import me.dozen.dpreference.DPreference
import org.jetbrains.anko.ctx

class App : Application() {

    val defaultDPreference by lazy { DPreference(this, BuildConfig.APPLICATION_ID + "_preferences") }

    override fun onCreate() {
        super.onCreate()

        NonXposedQuickActions.inject(this)

        Logger.init().logLevel(if (BuildConfig.DEBUG) LogLevel.FULL else LogLevel.NONE)
    }
}

val Context.app: App
    get() = applicationContext as App

val Context.defaultDPreference: DPreference
    get() = app.defaultDPreference

val Fragment.defaultDPreference: DPreference
    get() = ctx.defaultDPreference
