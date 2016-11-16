package com.ztc1997.fingerprint2sleep.xposed.extention

import de.robv.android.xposed.XposedBridge

fun <T> tryAndPrintStackTrace(task: () -> T): T? {
    try {
        return task()
    } catch (t: Throwable) {
        XposedBridge.log(t)
    }
    return null
}