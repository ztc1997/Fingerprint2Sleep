package com.ztc1997.fingerprint2sleep.activity

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.fingerprint.FingerprintManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
import android.widget.TextView
import com.eightbitlab.rxbus.Bus
import com.ztc1997.fingerprint2sleep.BuildConfig
import com.ztc1997.fingerprint2sleep.R
import com.ztc1997.fingerprint2sleep.extension.finishWithoutAnim
import com.ztc1997.fingerprint2sleep.extra.RestartScanningDelayedEvent
import com.ztc1997.fingerprint2sleep.quickactions.NonXposedQuickActions
import com.ztc1997.fingerprint2sleep.service.FPQAAccessibilityService
import org.jetbrains.anko.find
import org.jetbrains.anko.fingerprintManager
import org.jetbrains.anko.toast

class ShortenTimeOutActivity : Activity() {
    companion object {
        const val REQUEST_CODE = 1

        fun startActivity(context: Context) {
            val startIntent = Intent(context, ShortenTimeOutActivity::class.java)
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startIntent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            context.startActivity(startIntent)
        }
    }

    val authenticationCallback = object : FingerprintManager.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: FingerprintManager.AuthenticationResult?) {
            super.onAuthenticationSucceeded(result)
            finishWithoutAnim()
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
            super.onAuthenticationError(errorCode, errString)
            view?.find<TextView>(R.id.tv)?.text = errString
        }
    }

    val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            finishWithoutAnim()
        }
    }

    val collapsePanelsRunnable = object : Runnable {
        override fun run() {
            if (FPQAAccessibilityService.isRunning) {
                NonXposedQuickActions.collapsePanels()
            }
            view?.postDelayed(this, 100)
        }
    }

    var viewAdded = false
    var view: View? = null
    var screenTimeout = 60 * 1000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Settings.canDrawOverlays(this)) {
            addOverlayToWindows()
        } else {
            val intent = Intent("android.settings.action.MANAGE_OVERLAY_PERMISSION",
                    Uri.parse("package:${BuildConfig.APPLICATION_ID}"))
            startActivityForResult(intent, REQUEST_CODE)
        }

        val intentFilter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        intentFilter.addAction(Intent.ACTION_SCREEN_ON)
        intentFilter.addAction(Intent.ACTION_USER_PRESENT)
        registerReceiver(screenOffReceiver, intentFilter)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(screenOffReceiver)
        if (viewAdded) view?.let {
            windowManager.removeViewImmediate(it)
            setScreenTimeOut(screenTimeout)
        }
        view = null
        Bus.send(RestartScanningDelayedEvent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE)
            if (Settings.canDrawOverlays(this))
                addOverlayToWindows()
            else {
                toast(R.string.toast_window_overlay_permission_failed)
                finishWithoutAnim()
            }
    }


    fun setScreenTimeOut(value: Int) {
        Settings.System.putInt(contentResolver, "screen_off_timeout", value)
    }

    fun getScreenTimeOut(): Int {
        return Settings.System.getInt(contentResolver, "screen_off_timeout")
    }

    fun addOverlayToWindows() {
        if (viewAdded) return

        val params = WindowManager.LayoutParams()
        with(params) {
            flags = 1808
            type = TYPE_SYSTEM_ALERT
            gravity = Gravity.TOP
            width = -1
            height = -1
            format = -1
            screenBrightness = 0f
        }
        view = layoutInflater.inflate(R.layout.activity_shorten_time_out, null)
        view?.systemUiVisibility = 5894
        view?.setOnSystemUiVisibilityChangeListener {
            view?.systemUiVisibility = 5894
        }
        windowManager.addView(view, params)

        viewAdded = true

        screenTimeout = getScreenTimeOut()
        setScreenTimeOut(0)

        view?.post(collapsePanelsRunnable)

        fingerprintManager.authenticate(null, null, 0, authenticationCallback, null)
    }
}