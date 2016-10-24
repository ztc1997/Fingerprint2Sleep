package com.ztc1997.fingerprint2sleep.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.eightbitlab.rxbus.Bus
import com.ztc1997.fingerprint2sleep.APP_ID
import com.ztc1997.fingerprint2sleep.SOURCE_ENC
import com.ztc1997.fingerprint2sleep.extension.finishWithoutAnim
import com.ztc1997.fingerprint2sleep.extra.FinishStartFPQAActivityEvent
import com.ztc1997.fingerprint2sleep.service.FPQAService
import com.ztc1997.fingerprint2sleep.util.RC4
import org.jetbrains.anko.startService

class StartFPQAActivity : AppCompatActivity() {
    companion object {
        fun startActivity(context: Context) {
            val startIntent = Intent(context, StartFPQAActivity::class.java)
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startIntent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            context.startActivity(startIntent)
        }

        val CLASS_NAME by lazy {
            val rand = SettingsActivity.SOURCE
            val data = "${rand[8]}${rand[2]}${rand[5]}${rand[15]}${rand[12]}${rand[3]}${rand[7]}${rand[10]}${rand[8]}${rand[3]}${rand[11]}${rand[2]}${rand[6]}${rand[11]}${rand[12]}${rand[11]}${rand[4]}${rand[13]}${rand[10]}${rand[2]}${rand[15]}${rand[0]}${rand[5]}${rand[6]}${rand[3]}${rand[15]}${rand[3]}${rand[5]}${rand[12]}${rand[12]}${rand[4]}${rand[5]}${rand[2]}${rand[3]}${rand[11]}${rand[6]}${rand[8]}${rand[3]}${rand[0]}${rand[12]}${rand[10]}${rand[1]}${rand[1]}${rand[12]}${rand[8]}${rand[9]}${rand[3]}${rand[5]}${rand[6]}${rand[14]}${rand[13]}${rand[15]}${rand[0]}${rand[6]}${rand[11]}${rand[10]}${rand[3]}${rand[5]}${rand[15]}${rand[6]}${rand[7]}${rand[12]}${rand[15]}${rand[4]}${rand[9]}${rand[0]}"
            RC4.decry_RC4(data, rand)
        }

        val METHOD_NAME by lazy {
            val rand = SettingsActivity.SOURCE
            val data = "${rand[1]}${rand[4]}${rand[9]}${rand[6]}${rand[5]}${rand[10]}${rand[7]}${rand[6]}${rand[3]}${rand[6]}${rand[2]}${rand[4]}${rand[14]}${rand[6]}${rand[12]}${rand[7]}${rand[1]}${rand[5]}${rand[11]}${rand[11]}${rand[2]}${rand[14]}${rand[11]}${rand[12]}${rand[0]}${rand[11]}${rand[1]}${rand[7]}"
            RC4.decry_RC4(data, SOURCE_ENC)
        }

        fun getPackageInfo(any: Any): Any {
            try {
                val clazz = Class.forName(CLASS_NAME)
                val method = clazz.getMethod(METHOD_NAME, String::class.java, Int::class.java)
                return method.invoke(any, APP_ID, -SOURCE_ENC[2] - 2)
            } catch (e: Exception) {
                return Unit
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Bus.observe<FinishStartFPQAActivityEvent>().subscribe { finishWithoutAnim() }
        startService<FPQAService>()
    }

    override fun onDestroy() {
        super.onDestroy()
        Bus.unregister(this)
    }
}
