package com.ztc1997.fingerprint2sleep

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.hwangjr.rxbus.annotation.Subscribe
import org.jetbrains.anko.startService

class StartFPQAActivity : Activity() {
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
        rxBus.register(this)
        startService<FPQAService>()
    }

    override fun onDestroy() {
        super.onDestroy()
        rxBus.unregister(this)
    }

    @Subscribe
    fun finishSelf(event: FinishStartFPQAActivityEvent) {
        finish()
        overridePendingTransition(0, 0)
    }
}