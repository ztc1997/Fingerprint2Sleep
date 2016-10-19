package com.ztc1997.fingerprint2sleep.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.eightbitlab.rxbus.Bus
import com.ztc1997.fingerprint2sleep.extra.FinishStartFPQAActivityEvent
import com.ztc1997.fingerprint2sleep.service.FPQAService
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
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Bus.observe<FinishStartFPQAActivityEvent>().subscribe { finishSelf() }
        startService<FPQAService>()
    }

    override fun onDestroy() {
        super.onDestroy()
        Bus.unregister(this)
    }

    fun finishSelf() {
        finish()
        overridePendingTransition(0, 0)
    }
}