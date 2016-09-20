package com.ztc1997.fingerprint2sleep

import android.app.Activity
import android.os.Bundle
import org.jetbrains.anko.startService

class SplashActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startService<FP2SService>()
        finish()
    }
}