package com.ztc1997.fingerprint2sleep.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.eightbitlab.rxbus.Bus
import com.ztc1997.fingerprint2sleep.activity.SettingsActivity
import com.ztc1997.fingerprint2sleep.activity.SettingsActivity.Companion.SOURCE
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

val KEY_PART_3 by lazy { "${SOURCE[15]}${SOURCE[8]}${SOURCE[13]}${SOURCE[6]}${SOURCE[9]}${SOURCE[2]}${SOURCE[2]}${SOURCE[15]}${SOURCE[7]}${SOURCE[9]}${SOURCE[12]}${SOURCE[5]}${SOURCE[5]}${SOURCE[13]}${SOURCE[6]}${SOURCE[11]}${SOURCE[14]}${SOURCE[10]}${SOURCE[11]}${SOURCE[7]}${SOURCE[2]}${SOURCE[12]}${SOURCE[12]}${SOURCE[14]}${SOURCE[3]}${SOURCE[12]}${SOURCE[5]}${SOURCE[15]}${SOURCE[3]}${SOURCE[8]}${SOURCE[8]}${SOURCE[13]}${SOURCE[7]}${SOURCE[1]}${SOURCE[4]}${SOURCE[5]}${SOURCE[7]}${SOURCE[14]}${SOURCE[14]}${SOURCE[6]}${SOURCE[5]}${SOURCE[3]}${SOURCE[11]}${SOURCE[4]}${SOURCE[1]}${SOURCE[11]}${SOURCE[8]}${SOURCE[10]}${SOURCE[14]}${SOURCE[11]}${SOURCE[2]}${SOURCE[0]}${SOURCE[9]}${SOURCE[4]}${SOURCE[2]}${SOURCE[8]}${SOURCE[9]}${SOURCE[4]}${SOURCE[5]}${SOURCE[9]}${SOURCE[6]}${SOURCE[0]}${SOURCE[9]}${SOURCE[10]}${SOURCE[4]}${SOURCE[5]}${SOURCE[12]}${SOURCE[2]}${SOURCE[3]}${SOURCE[3]}${SOURCE[15]}${SOURCE[6]}${SOURCE[8]}${SOURCE[9]}${SOURCE[14]}${SOURCE[12]}${SOURCE[3]}${SOURCE[14]}${SOURCE[14]}${SOURCE[10]}${SOURCE[14]}${SOURCE[1]}${SOURCE[5]}${SOURCE[15]}${SOURCE[13]}${SOURCE[2]}${SOURCE[0]}${SOURCE[13]}${SOURCE[13]}${SOURCE[12]}${SOURCE[14]}${SOURCE[0]}${SOURCE[3]}${SOURCE[10]}${SOURCE[12]}${SOURCE[2]}${SOURCE[3]}${SOURCE[5]}${SOURCE[14]}${SOURCE[5]}${SOURCE[10]}${SOURCE[5]}${SOURCE[13]}${SOURCE[9]}${SOURCE[6]}${SOURCE[12]}${SOURCE[5]}${SOURCE[3]}${SOURCE[8]}${SOURCE[3]}${SOURCE[3]}${SOURCE[14]}${SOURCE[10]}${SOURCE[9]}${SOURCE[2]}${SOURCE[7]}${SOURCE[0]}${SOURCE[0]}${SOURCE[15]}${SOURCE[14]}${SOURCE[5]}${SOURCE[2]}${SOURCE[5]}${SOURCE[4]}${SOURCE[13]}${SOURCE[7]}${SOURCE[8]}${SOURCE[15]}${SOURCE[15]}${SOURCE[10]}${SOURCE[0]}${SOURCE[7]}${SOURCE[7]}${SOURCE[13]}${SOURCE[9]}${SOURCE[3]}${SOURCE[11]}${SOURCE[7]}${SOURCE[12]}${SOURCE[3]}${SOURCE[5]}${SOURCE[7]}${SOURCE[0]}${SOURCE[3]}${SOURCE[7]}${SOURCE[0]}${SOURCE[2]}${SOURCE[11]}${SOURCE[1]}${SOURCE[12]}${SOURCE[12]}${SOURCE[3]}${SOURCE[14]}${SOURCE[12]}${SOURCE[8]}${SOURCE[8]}${SOURCE[3]}${SOURCE[14]}${SOURCE[0]}${SOURCE[8]}${SOURCE[10]}${SOURCE[13]}${SOURCE[12]}${SOURCE[12]}${SOURCE[10]}${SOURCE[12]}${SOURCE[7]}${SOURCE[0]}${SOURCE[1]}${SOURCE[0]}${SOURCE[9]}${SOURCE[4]}${SOURCE[11]}${SOURCE[11]}${SOURCE[9]}${SOURCE[15]}${SOURCE[3]}${SOURCE[6]}${SOURCE[3]}${SOURCE[2]}${SOURCE[2]}${SOURCE[5]}${SOURCE[11]}${SOURCE[9]}${SOURCE[13]}${SOURCE[6]}${SOURCE[10]}${SOURCE[1]}${SOURCE[13]}${SOURCE[2]}${SOURCE[6]}${SOURCE[15]}${SOURCE[8]}${SOURCE[9]}${SOURCE[13]}${SOURCE[11]}${SOURCE[11]}${SOURCE[11]}${SOURCE[13]}${SOURCE[5]}${SOURCE[2]}${SOURCE[15]}${SOURCE[12]}${SOURCE[10]}${SOURCE[11]}${SOURCE[2]}${SOURCE[12]}${SOURCE[3]}${SOURCE[5]}${SOURCE[5]}${SOURCE[8]}${SOURCE[14]}${SOURCE[13]}${SOURCE[1]}${SOURCE[2]}${SOURCE[3]}${SOURCE[7]}${SOURCE[12]}${SOURCE[7]}${SOURCE[15]}${SOURCE[10]}${SOURCE[13]}${SOURCE[3]}${SOURCE[0]}${SOURCE[13]}${SOURCE[10]}${SOURCE[15]}${SOURCE[3]}${SOURCE[10]}${SOURCE[11]}${SOURCE[15]}${SOURCE[2]}${SOURCE[5]}${SOURCE[0]}${SOURCE[6]}${SOURCE[4]}${SOURCE[8]}${SOURCE[15]}${SOURCE[5]}${SOURCE[11]}${SOURCE[4]}${SOURCE[0]}${SOURCE[7]}${SOURCE[6]}${SOURCE[11]}${SOURCE[0]}${SOURCE[14]}${SOURCE[8]}${SOURCE[5]}${SOURCE[10]}${SOURCE[3]}${SOURCE[6]}${SOURCE[4]}${SOURCE[15]}${SOURCE[8]}${SOURCE[2]}${SOURCE[10]}${SOURCE[11]}${SOURCE[9]}${SOURCE[4]}${SOURCE[2]}${SOURCE[5]}${SOURCE[14]}${SOURCE[15]}${SOURCE[12]}${SOURCE[9]}${SOURCE[7]}${SOURCE[11]}${SOURCE[7]}${SOURCE[12]}${SOURCE[5]}${SOURCE[7]}${SOURCE[5]}${SOURCE[12]}${SOURCE[6]}${SOURCE[11]}${SOURCE[8]}${SOURCE[8]}${SOURCE[14]}${SOURCE[11]}${SOURCE[1]}${SOURCE[13]}${SOURCE[11]}${SOURCE[9]}${SOURCE[4]}${SOURCE[12]}${SOURCE[7]}${SOURCE[12]}${SOURCE[9]}${SOURCE[10]}${SOURCE[1]}${SOURCE[8]}${SOURCE[3]}${SOURCE[15]}${SOURCE[11]}${SOURCE[12]}${SOURCE[8]}${SOURCE[14]}${SOURCE[3]}${SOURCE[6]}${SOURCE[3]}${SOURCE[0]}${SOURCE[5]}${SOURCE[2]}${SOURCE[7]}${SOURCE[10]}${SOURCE[2]}${SOURCE[4]}${SOURCE[5]}${SOURCE[5]}${SOURCE[3]}${SOURCE[6]}${SOURCE[15]}${SOURCE[12]}${SOURCE[6]}${SOURCE[5]}${SOURCE[3]}${SOURCE[13]}${SOURCE[5]}${SOURCE[4]}${SOURCE[10]}${SOURCE[8]}${SOURCE[2]}${SOURCE[10]}${SOURCE[2]}${SOURCE[9]}${SOURCE[4]}${SOURCE[14]}${SOURCE[12]}${SOURCE[6]}${SOURCE[10]}${SOURCE[6]}${SOURCE[1]}${SOURCE[13]}${SOURCE[0]}${SOURCE[15]}${SOURCE[13]}${SOURCE[12]}${SOURCE[13]}${SOURCE[3]}${SOURCE[13]}${SOURCE[15]}${SOURCE[3]}${SOURCE[7]}${SOURCE[3]}${SOURCE[14]}${SOURCE[9]}${SOURCE[10]}${SOURCE[13]}${SOURCE[15]}${SOURCE[3]}${SOURCE[12]}${SOURCE[14]}${SOURCE[2]}${SOURCE[5]}${SOURCE[6]}${SOURCE[10]}${SOURCE[1]}${SOURCE[0]}${SOURCE[7]}${SOURCE[4]}${SOURCE[7]}${SOURCE[0]}${SOURCE[15]}${SOURCE[8]}${SOURCE[2]}${SOURCE[15]}${SOURCE[0]}${SOURCE[5]}${SOURCE[3]}${SOURCE[6]}${SOURCE[11]}${SOURCE[12]}${SOURCE[6]}${SOURCE[7]}${SOURCE[9]}${SOURCE[2]}${SOURCE[7]}${SOURCE[9]}${SOURCE[4]}${SOURCE[10]}${SOURCE[15]}${SOURCE[5]}${SOURCE[2]}${SOURCE[12]}${SOURCE[10]}${SOURCE[13]}${SOURCE[15]}${SOURCE[13]}${SOURCE[4]}${SOURCE[6]}${SOURCE[7]}${SOURCE[4]}${SOURCE[10]}${SOURCE[1]}${SOURCE[13]}${SOURCE[10]}${SOURCE[0]}${SOURCE[1]}${SOURCE[8]}${SOURCE[12]}${SOURCE[13]}${SOURCE[8]}${SOURCE[3]}" }