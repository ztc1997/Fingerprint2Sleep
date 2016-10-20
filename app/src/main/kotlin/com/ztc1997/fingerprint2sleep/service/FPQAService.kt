package com.ztc1997.fingerprint2sleep.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.fingerprint.FingerprintManager
import android.os.CancellationSignal
import android.os.IBinder
import android.support.v7.app.NotificationCompat
import com.eightbitlab.rxbus.Bus
import com.ztc1997.fingerprint2sleep.R
import com.ztc1997.fingerprint2sleep.activity.RequireAccessibilityActivity
import com.ztc1997.fingerprint2sleep.activity.SettingsActivity
import com.ztc1997.fingerprint2sleep.activity.StartFPQAActivity
import com.ztc1997.fingerprint2sleep.aidl.IFPQAService
import com.ztc1997.fingerprint2sleep.defaultDPreference
import com.ztc1997.fingerprint2sleep.extension.root
import com.ztc1997.fingerprint2sleep.extra.ActivityChangedEvent
import com.ztc1997.fingerprint2sleep.extra.FinishStartFPQAActivityEvent
import com.ztc1997.fingerprint2sleep.extra.IsScanningChangedEvent
import com.ztc1997.fingerprint2sleep.util.QuickActions
import org.jetbrains.anko.*
import java.util.concurrent.TimeUnit

class FPQAService : Service() {
    companion object {
        const val NOTIFICATION_ID = 1
        const val NOTIFICATION_PENDING_INTENT_CONTENT = 0
    }

    var isRunning = false

    var delayIsScanning = false

    var isScanning = false
        set(value) {
            field = value
            if (value)
                delayIsScanning = value
            else
                Bus.send(IsScanningChangedEvent(value))
        }

    var isError = false
        set(value) {
            field = value
            startForegroundIfSet(value)
        }

    var cancellationSignal = CancellationSignal()

    var lastIntent: Intent? = null

    val authenticationCallback = object : FingerprintManager.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: FingerprintManager.AuthenticationResult?) {
            super.onAuthenticationSucceeded(result)
            isScanning = false
            doOnFingerprintDetected()
        }

        override fun onAuthenticationFailed() {
            super.onAuthenticationFailed()
            if (!defaultDPreference.getPrefBoolean(SettingsActivity.PREF_RESPONSE_ENROLLED_FINGERPRINT_ONLY, false))
                doOnFingerprintDetected()
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
            super.onAuthenticationError(errorCode, errString)
            if (defaultDPreference.getPrefBoolean(SettingsActivity.PREF_NOTIFY_ON_ERROR, false))
                errString?.let { toast(getString(R.string.toast_notify_on_error, it)) }
            isError = true
            isScanning = false
        }
    }

    val presentReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            if (!isScanning)
                StartFPQAActivity.startActivity(ctx)
        }
    }

    val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            cancellationSignal.cancel()
            isScanning = false
        }
    }

    val binder = object : IFPQAService.Stub() {
        override fun onPrefChanged(key: String?) {
            when (key) {
                SettingsActivity.PREF_FOREGROUND_SERVICE -> startForegroundIfSet()

                SettingsActivity.PREF_ENABLE_FINGERPRINT_QUICK_ACTION ->
                    if (!defaultDPreference.getPrefBoolean(key, false))
                        stopFPQA()

                SettingsActivity.PREF_LOCK_SCREEN_WITH_POWER_BUTTON_AS_ROOT ->
                    if (defaultDPreference.getPrefBoolean(key, false))
                        checkAndStartRoot()
            }
        }

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startFPQA()

        if (cancellationSignal.isCanceled) cancellationSignal = CancellationSignal()

        if (!isScanning) {
            fingerprintManager.authenticate(null, cancellationSignal, 0, authenticationCallback, null)
            isScanning = true
        }

        Bus.send(FinishStartFPQAActivityEvent)

        isError = false

        lastIntent?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(it)
            lastIntent = null
        }

        if (!FPQAAccessibilityService.isRunning)
            RequireAccessibilityActivity.startActivity(this)

        if (defaultDPreference.getPrefBoolean(SettingsActivity.PREF_LOCK_SCREEN_WITH_POWER_BUTTON_AS_ROOT, false))
            checkAndStartRoot()


        val newFlags = flags or START_STICKY
        return super.onStartCommand(intent, newFlags, startId)
    }

    fun doOnFingerprintDetected() {
        when (defaultDPreference.getPrefString(SettingsActivity.PREF_QUICK_ACTION,
                SettingsActivity.VALUES_PREF_QUICK_ACTION_SLEEP)) {
            SettingsActivity.VALUES_PREF_QUICK_ACTION_SLEEP -> {
                QuickActions.goToSleep()
                return
            }

            SettingsActivity.VALUES_PREF_QUICK_ACTION_HOME ->
                QuickActions.goToHome()

            SettingsActivity.VALUES_PREF_QUICK_ACTION_EXPEND_NOTIFICATIONS_PANEL ->
                QuickActions.expandNotificationsPanel()

            SettingsActivity.VALUES_PREF_QUICK_ACTION_TOGGLE_NOTIFICATIONS_PANEL ->
                QuickActions.toggleNotificationsPanel()
        }

        StartFPQAActivity.startActivity(ctx)
    }

    fun startFPQA() {
        if (!isRunning) {
            isRunning = true

            registerReceiver(presentReceiver, IntentFilter(Intent.ACTION_USER_PRESENT))
            registerReceiver(screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))

            Bus.observe<ActivityChangedEvent>()
                    .filter { !delayIsScanning }
                    .throttleLast(1, TimeUnit.SECONDS)
                    .filter { !isScanning && isRunning }
                    .subscribe { onActivityChanged() }

            Bus.observe<IsScanningChangedEvent>()
                    .throttleLast(1, TimeUnit.SECONDS)
                    .subscribe { delayIsScanning = it.value }
        }
    }

    fun stopFPQA() {
        if (isRunning) {
            isRunning = false

            unregisterReceiver(presentReceiver)
            unregisterReceiver(screenOffReceiver)

            Bus.unregister(this)
            delayIsScanning = isScanning

            cancellationSignal.cancel()
            stopForeground(true)
            stopSelf()
        }
    }

    fun onActivityChanged() {
        StartFPQAActivity.startActivity(ctx)
    }

    fun startForegroundIfSet() = startForegroundIfSet(isError)

    fun startForegroundIfSet(isError: Boolean) {
        if (isRunning && defaultDPreference.getPrefBoolean(SettingsActivity.PREF_FOREGROUND_SERVICE, false)) {
            val notification = generateNotification(if (isError)
                R.string.notification_content_text_error else R.string.notification_content_text)

            startForeground(NOTIFICATION_ID, notification)
        } else {
            stopForeground(true)
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    inline fun generateNotification(textRes: Int) = generateNotification(getString(textRes))

    fun generateNotification(text: CharSequence): Notification {
        val startMainIntent = Intent(applicationContext, SettingsActivity::class.java)
        val contentPendingIntent = PendingIntent.getActivity(applicationContext,
                NOTIFICATION_PENDING_INTENT_CONTENT, startMainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT)

        val notification = Notification.Builder(applicationContext)
                .setSmallIcon(R.drawable.ic_fingerprint_white_24dp)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setContentIntent(contentPendingIntent)
                .build()

        return notification
    }

    fun checkAndStartRoot() {
        doAsync {
            if (!root.isStarted && !root.startShell()) {
                uiThread { toast(com.ztc1997.fingerprint2sleep.R.string.toast_root_access_failed) }
            }
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return binder
    }
}
