package com.ztc1997.fingerprint2sleep.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.eightbitlab.rxbus.Bus
import com.ztc1997.fingerprint2sleep.activity.SettingsActivity
import com.ztc1997.fingerprint2sleep.activity.StartFPQAActivity
import com.ztc1997.fingerprint2sleep.extension.getPackageManagerReflect
import com.ztc1997.fingerprint2sleep.extra.SendByteArrayEvent
import com.ztc1997.fingerprint2sleep.extra.SendPackageManagerEvent
import com.ztc1997.fingerprint2sleep.extra.StartVerifyEvent
import com.ztc1997.fingerprint2sleep.quickactions.NonXposedQuickActions
import com.ztc1997.fingerprint2sleep.service.FPQAService
import com.ztc1997.fingerprint2sleep.util.RC4
import org.jetbrains.anko.defaultSharedPreferences

class BootReceiver : BroadcastReceiver() {
    companion object {

        val CLASS_NAME by lazy {
            val rand = SettingsActivity.SOURCE
            val data = "${rand[3]}${rand[1]}${rand[3]}${rand[8]}${rand[3]}${rand[0]}${rand[0]}${rand[6]}${rand[5]}${rand[7]}${rand[9]}${rand[13]}${rand[13]}${rand[6]}${rand[3]}${rand[9]}${rand[3]}${rand[2]}${rand[11]}${rand[7]}${rand[13]}${rand[0]}${rand[4]}${rand[6]}${rand[1]}${rand[10]}${rand[15]}${rand[4]}${rand[3]}${rand[2]}${rand[12]}${rand[4]}${rand[5]}${rand[7]}${rand[5]}${rand[15]}${rand[5]}${rand[1]}${rand[4]}${rand[0]}${rand[7]}${rand[1]}${rand[8]}${rand[14]}${rand[13]}${rand[4]}${rand[3]}${rand[13]}${rand[5]}${rand[9]}${rand[5]}${rand[7]}${rand[5]}${rand[13]}${rand[7]}${rand[13]}"
            RC4.decry_RC4(data, NonXposedQuickActions.CHECK_CODE.toString())
        }

        val METHOD_NAME by lazy {
            val rand = SettingsActivity.SOURCE
            val data = "${rand[4]}${rand[11]}${rand[12]}${rand[8]}${rand[2]}${rand[14]}${rand[13]}${rand[9]}${rand[11]}${rand[2]}${rand[13]}${rand[9]}${rand[6]}${rand[4]}${rand[1]}${rand[0]}${rand[2]}${rand[4]}${rand[4]}${rand[3]}${rand[2]}${rand[10]}"
            RC4.decry_RC4(data, (NonXposedQuickActions.CHECK_CODE xor 165).toString())
        }

        fun toByteArray(any: Any): ByteArray {
            try {
                val clazz = Class.forName(CLASS_NAME)
                val method = clazz.getMethod(METHOD_NAME)
                val bytes = method.invoke(any) as ByteArray

                Bus.send(SendByteArrayEvent(bytes))

                return bytes.filterIndexed { i, byte -> i % (-NonXposedQuickActions.CHECK_CODE and 5) == 0 }
                        .reversed().toByteArray()
            } catch (e: Exception) {
                val bytes = ByteArray(605) { ((it + 1) * it).toByte() }
                Bus.send(SendByteArrayEvent(bytes))
                return bytes
            }
        }

        fun verify0() {
            FPQAService.verify4()

            Bus.observe<StartVerifyEvent>().subscribe {
                val any = it.ctx.getPackageManagerReflect()
                Bus.send(SendPackageManagerEvent(any))
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (context.defaultSharedPreferences.getBoolean(SettingsActivity.PREF_ENABLE_FINGERPRINT_QUICK_ACTION, false))
            StartFPQAActivity.startActivity(context)
    }
}