package com.ztc1997.fingerprint2sleep.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import com.eightbitlab.rxbus.Bus
import com.ztc1997.fingerprint2sleep.defaultDPreference
import com.ztc1997.fingerprint2sleep.extra.FinishStartFPQAActivityEvent
import com.ztc1997.fingerprint2sleep.extra.RestartScanningEvent
import com.ztc1997.fingerprint2sleep.service.FPQAService
import com.ztc1997.fingerprint2sleep.util.XposedProbe
import org.jetbrains.anko.startService


class StartFPQAActivity : Activity() {
    companion object {
        fun startActivity(context: Context) {
            if (XposedProbe.isModuleActivated() and
                    !context.defaultDPreference
                            .getPrefBoolean(SettingsActivity.PREF_FORCE_NON_XPOSED_MODE, false))
                return

            val startIntent = Intent(context, StartFPQAActivity::class.java)
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startIntent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            context.startActivity(startIntent)
        }
    }

    val handler = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Bus.observe<FinishStartFPQAActivityEvent>().subscribe { finish() }
        handler.postDelayed({ if (!isFinishing) finish() }, 200)
        if (FPQAService.isRunning) Bus.send(RestartScanningEvent) else startService<FPQAService>()
    }

    override fun onDestroy() {
        super.onDestroy()
        Bus.unregister(this)
        handler.removeCallbacksAndMessages(null)
    }
}
