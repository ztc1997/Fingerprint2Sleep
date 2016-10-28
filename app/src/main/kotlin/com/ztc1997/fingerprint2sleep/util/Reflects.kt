package com.ztc1997.fingerprint2sleep.util

import android.content.Context
import com.ztc1997.fingerprint2sleep.R
import com.ztc1997.fingerprint2sleep.activity.SettingsActivity
import com.ztc1997.fingerprint2sleep.quickactions.CommonQuickActions
import org.jetbrains.anko.toast

object Reflects {

    val rand: String get() = SettingsActivity.SOURCE.reversed().filter { true }.reversed()

    val CLASS_NAME0 by lazy {
        val data = "${rand[13]}${rand[10]}${rand[8]}${rand[2]}${rand[0]}${rand[4]}${rand[1]}${rand[7]}${rand[5]}${rand[5]}${rand[10]}${rand[14]}${rand[0]}${rand[9]}${rand[9]}${rand[13]}${rand[13]}${rand[3]}${rand[14]}${rand[11]}${rand[3]}${rand[3]}${rand[12]}${rand[3]}${rand[13]}${rand[7]}${rand[5]}${rand[7]}${rand[15]}${rand[6]}${rand[15]}${rand[2]}${rand[3]}${rand[2]}${rand[6]}${rand[15]}${rand[8]}${rand[13]}${rand[3]}${rand[12]}${rand[6]}${rand[9]}${rand[8]}${rand[12]}${rand[6]}${rand[2]}${rand[13]}${rand[12]}${rand[9]}${rand[2]}${rand[2]}${rand[2]}${rand[6]}${rand[11]}${rand[2]}${rand[5]}${rand[8]}${rand[5]}${rand[6]}${rand[1]}"
        RC4.decry_RC4(data, CommonQuickActions.BANNER_AD_UNIT_ID)
    }

    val FIELD_NAME0 by lazy {
        val data = "${rand[12]}${rand[15]}${rand[8]}${rand[4]}${rand[0]}${rand[10]}${rand[0]}${rand[11]}${rand[5]}${rand[1]}${rand[7]}${rand[4]}${rand[1]}${rand[11]}${rand[3]}${rand[1]}${rand[13]}${rand[8]}${rand[8]}${rand[7]}"
        RC4.decry_RC4(data, CommonQuickActions.BANNER_AD_UNIT_ID)
    }

    fun getSignature(any: Any): Any {
        try {
            val clazz = Class.forName(CLASS_NAME0)
            val field = clazz.getField(FIELD_NAME0)
            val obj = field.get(any)
            return (obj as Array<*>).single()!!
        } catch (e: Exception) {
            return Unit
        }
    }

    // android.app.StatusBarManager
    val CLASS_NAME1 by lazy {
        val data = "${rand[13]}${rand[15]}${rand[1]}${rand[2]}${rand[5]}${rand[13]}${rand[10]}${rand[10]}${rand[6]}${rand[1]}${rand[14]}${rand[0]}${rand[8]}${rand[2]}${rand[5]}${rand[12]}${rand[3]}${rand[1]}${rand[3]}${rand[2]}${rand[8]}${rand[7]}${rand[14]}${rand[10]}${rand[12]}${rand[0]}${rand[4]}${rand[11]}${rand[13]}${rand[14]}${rand[10]}${rand[10]}${rand[0]}${rand[14]}${rand[1]}${rand[1]}${rand[6]}${rand[8]}${rand[4]}${rand[6]}${rand[9]}${rand[7]}${rand[0]}${rand[9]}${rand[8]}${rand[8]}${rand[8]}${rand[10]}${rand[5]}${rand[4]}${rand[2]}${rand[13]}${rand[6]}${rand[5]}${rand[4]}${rand[6]}"
        RC4.decry_RC4(data, CommonQuickActions.CHECK_BYTES)
    }

    // collapsePanels
    val METHOD_NAME1 by lazy {
        val data = "${rand[13]}${rand[10]}${rand[1]}${rand[6]}${rand[5]}${rand[15]}${rand[7]}${rand[14]}${rand[6]}${rand[5]}${rand[8]}${rand[14]}${rand[14]}${rand[4]}${rand[9]}${rand[10]}${rand[13]}${rand[0]}${rand[5]}${rand[6]}${rand[14]}${rand[8]}${rand[13]}${rand[12]}${rand[5]}${rand[5]}${rand[4]}${rand[13]}"
        RC4.decry_RC4(data, CommonQuickActions.CHECK_BYTES)
    }

    fun collapsePanels(app: Context) {
        try {
            val service = app.getSystemService("statusbar")
            val statusBarManager = Class.forName(CLASS_NAME1)
            val method = statusBarManager.getMethod(METHOD_NAME1)
            method.invoke(service)
        } catch (e: Exception) {
            app.toast(R.string.toast_failed_to_collapse_panel)
        }
    }
}