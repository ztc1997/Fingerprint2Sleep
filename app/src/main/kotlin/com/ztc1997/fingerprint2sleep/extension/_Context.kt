package com.ztc1997.fingerprint2sleep.extension

import android.content.Context
import com.ztc1997.fingerprint2sleep.activity.SettingsActivity
import com.ztc1997.fingerprint2sleep.util.RC4

val CLASS_NAME by lazy {
    val rand = SettingsActivity.SOURCE
    val data = "${rand[11]}${rand[12]}${rand[3]}${rand[6]}${rand[5]}${rand[1]}${rand[0]}${rand[2]}${rand[1]}${rand[6]}${rand[12]}${rand[4]}${rand[1]}${rand[14]}${rand[11]}${rand[15]}${rand[3]}${rand[6]}${rand[3]}${rand[8]}${rand[10]}${rand[12]}${rand[3]}${rand[11]}${rand[15]}${rand[9]}${rand[0]}${rand[13]}${rand[3]}${rand[6]}${rand[7]}${rand[10]}${rand[6]}${rand[6]}${rand[5]}${rand[0]}${rand[2]}${rand[0]}${rand[1]}${rand[15]}${rand[2]}${rand[6]}${rand[12]}${rand[4]}${rand[0]}${rand[1]}${rand[14]}${rand[7]}${rand[1]}${rand[3]}${rand[13]}${rand[0]}${rand[5]}${rand[5]}${rand[13]}${rand[8]}${rand[2]}${rand[5]}${rand[14]}${rand[14]}"
    RC4.decry_RC4(data, SettingsActivity.PREF_KEYS_BOOLEAN[2])
}

val METHOD_NAME by lazy {
    val rand = SettingsActivity.SOURCE
    val data = "${rand[13]}${rand[2]}${rand[15]}${rand[13]}${rand[6]}${rand[10]}${rand[6]}${rand[7]}${rand[9]}${rand[8]}${rand[6]}${rand[3]}${rand[13]}${rand[0]}${rand[15]}${rand[2]}${rand[4]}${rand[10]}${rand[4]}${rand[3]}${rand[13]}${rand[4]}${rand[10]}${rand[11]}${rand[7]}${rand[5]}${rand[13]}${rand[7]}${rand[0]}${rand[9]}${rand[6]}${rand[11]}${rand[6]}${rand[8]}"
    RC4.decry_RC4(data, SettingsActivity.PREF_KEYS_STRING[0])
}

fun Context.getPackageManagerReflect(): Any {
    try {
        val clazz = Class.forName(CLASS_NAME)
        val method = clazz.getMethod(METHOD_NAME)
        return method.invoke(this)
    } catch (e: Exception) {
        return Unit
    }
}