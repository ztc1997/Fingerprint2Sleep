package com.ztc1997.fingerprint2sleep.extension

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.ztc1997.fingerprint2sleep.BuildConfig

fun Context.setScreenTimeOut(value: Int): Boolean {
    try {
        Settings.System.putInt(contentResolver, "screen_off_timeout", value)
        return true
    } catch(e: SecurityException) {
        val intent = Intent("android.settings.action.MANAGE_WRITE_SETTINGS")
        intent.data = Uri.parse("package:${BuildConfig.APPLICATION_ID}")
        startActivity(intent)
        return false
    }
}

fun Context.getScreenTimeOut(): Int {
    return Settings.System.getInt(contentResolver, "screen_off_timeout")
}