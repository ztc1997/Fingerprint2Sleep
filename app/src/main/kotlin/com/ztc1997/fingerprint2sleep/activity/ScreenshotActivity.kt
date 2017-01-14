package com.ztc1997.fingerprint2sleep.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.tbruyelle.rxpermissions.RxPermissions
import com.ztc1997.fingerprint2sleep.extension.finishWithoutAnim
import com.ztc1997.fingerprint2sleep.service.ScreenshotService
import org.jetbrains.anko.mediaProjectionManager
import org.jetbrains.anko.startService

class ScreenshotActivity : Activity() {
    companion object {
        private const val REQ_CREATE_SCREEN_CAPTURE = 0

        fun startActivity(context: Context) {
            val startIntent = Intent(context, ScreenshotActivity::class.java)
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            context.startActivity(startIntent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        RxPermissions(this)
                .request(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .subscribe {
                    if (it) {
                        val i = mediaProjectionManager.createScreenCaptureIntent()
                        startActivityForResult(i, REQ_CREATE_SCREEN_CAPTURE)
                    } else
                        finishWithoutAnim()
                }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_CREATE_SCREEN_CAPTURE && resultCode == RESULT_OK && data != null) {
            startService<ScreenshotService>("resultCode" to resultCode, "data" to data)
        }
        finishWithoutAnim()
    }
}