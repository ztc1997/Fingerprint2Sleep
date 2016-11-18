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
import android.view.WindowManager.LayoutParams.TYPE_SYSTEM_ERROR
import com.eightbitlab.rxbus.Bus
import com.ztc1997.fingerprint2sleep.BuildConfig
import com.ztc1997.fingerprint2sleep.R
import com.ztc1997.fingerprint2sleep.defaultDPreference
import com.ztc1997.fingerprint2sleep.extension.finishWithoutAnim
import com.ztc1997.fingerprint2sleep.extension.getScreenTimeOut
import com.ztc1997.fingerprint2sleep.extension.setScreenTimeOut
import com.ztc1997.fingerprint2sleep.extra.RestartScanningDelayedEvent
import com.ztc1997.fingerprint2sleep.quickactions.NonXposedQuickActions
import com.ztc1997.fingerprint2sleep.service.FPQAAccessibilityService
import kotlinx.android.synthetic.main.activity_shorten_time_out.view.*
import org.jetbrains.anko.fingerprintManager
import org.jetbrains.anko.onUiThread
import org.jetbrains.anko.toast
import java.util.concurrent.TimeUnit


class ShortenTimeOutActivity : Activity() {
    companion object {
        const val REQUEST_CODE = 1

        const val PREF_ORIGINAL_SCREEN_OFF_TIMEOUT = "pref_original_screen_off_timeout"

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
            Bus.send(RestartScanningDelayedEvent)
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
            super.onAuthenticationError(errorCode, errString)
            if (errorCode == FingerprintManager.FINGERPRINT_ERROR_CANCELED)
                Bus.send(RestartScanningEvent)
            else {
                Bus.unregister(this@ShortenTimeOutActivity)
                view?.tv?.text = errString
            }
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Settings.canDrawOverlays(this)) {
            addOverlayToWindows()
        } else {
            val intent = Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
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

        Bus.unregister(this)

        if (viewAdded) view?.let {
            windowManager.removeViewImmediate(it)
        }

        view = null

        val screenTimeout = defaultDPreference.getPrefInt(PREF_ORIGINAL_SCREEN_OFF_TIMEOUT, -1)
        if (screenTimeout > 0)
            setScreenTimeOut(screenTimeout)
        defaultDPreference.setPrefInt(PREF_ORIGINAL_SCREEN_OFF_TIMEOUT, -1)
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


    fun addOverlayToWindows() {
        if (viewAdded) return

        val screenTimeOut = getScreenTimeOut()
        if (screenTimeOut > 0)
            defaultDPreference.setPrefInt(PREF_ORIGINAL_SCREEN_OFF_TIMEOUT, screenTimeOut)

        if (!setScreenTimeOut(0)) {
            finishWithoutAnim()
            return
        }

        val params = WindowManager.LayoutParams()
        with(params) {
            flags = 1808
            type = TYPE_SYSTEM_ERROR
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

        view?.post(collapsePanelsRunnable)

        fingerprintManager.authenticate(null, null, 0, authenticationCallback, null)

        Bus.observe<RestartScanningEvent>()
                .throttleLast(100, TimeUnit.MILLISECONDS)
                .subscribe {
                    onUiThread { view?.tv?.setText(R.string.tv_unlock_via_fingerprint) }
                    fingerprintManager.authenticate(null, null, 0, authenticationCallback, null)
                }
    }

    object RestartScanningEvent
}