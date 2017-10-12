package com.ztc1997.fingerprint2sleep.extension

import android.app.Activity
import android.app.job.JobScheduler
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.ztc1997.fingerprint2sleep.BuildConfig

fun Context.setScreenTimeOut(value: Int): Boolean {
    return try {
        Settings.System.putInt(contentResolver, "screen_off_timeout", value)
        true
    } catch(e: SecurityException) {
        val intent = Intent("android.settings.action.MANAGE_WRITE_SETTINGS")
        intent.data = Uri.parse("package:${BuildConfig.APPLICATION_ID}")
        startActivity(intent)
        false
    }
}

fun Context.getScreenTimeOut(): Int = Settings.System.getInt(contentResolver, "screen_off_timeout")

val Context.jobScheduler: JobScheduler
    get() = getSystemService(Activity.JOB_SCHEDULER_SERVICE) as JobScheduler
