package com.ztc1997.fingerprint2sleep.xposed.extention

fun <T> tryAndPrintStackTrace(task: () -> T): T? {
    try {
        return task()
    } catch (t: Throwable) {
        t.printStackTrace()
    }
    return null
}