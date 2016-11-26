package com.ztc1997.fingerprint2sleep.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.eightbitlab.rxbus.Bus
import com.ztc1997.fingerprint2sleep.BuildConfig
import com.ztc1997.fingerprint2sleep.defaultDPreference
import com.ztc1997.fingerprint2sleep.extension.finishWithoutAnim
import com.ztc1997.fingerprint2sleep.extra.FinishStartFPQAActivityEvent
import com.ztc1997.fingerprint2sleep.service.FPQAService
import com.ztc1997.fingerprint2sleep.util.XposedProbe
import org.jetbrains.anko.startService

class StartFPQAActivity : Activity() {
    companion object {
        private val withAnim = "withAnim"

        fun startActivity(context: Context, withAnim: Boolean = false) {
            try {
                if (XposedProbe.isModuleActivated() and
                        !context.defaultDPreference
                                .getPrefBoolean(SettingsActivity.PREF_FORCE_NON_XPOSED_MODE, false))
                    return

                val startIntent = Intent(context, StartFPQAActivity::class.java)
                startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startIntent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                startIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                startIntent.putExtra(this@Companion.withAnim, withAnim)
                context.startActivity(startIntent)
            } catch(e: Exception) {
                if (BuildConfig.DEBUG)
                    e.printStackTrace()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Bus.observe<FinishStartFPQAActivityEvent>().subscribe {
            if (intent.getBooleanExtra(withAnim, false)) finish() else finishWithoutAnim()
        }
        startService<FPQAService>()
    }

    override fun onDestroy() {
        super.onDestroy()
        Bus.unregister(this)
    }
}
