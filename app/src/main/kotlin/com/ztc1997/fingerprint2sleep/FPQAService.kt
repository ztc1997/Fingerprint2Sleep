package com.ztc1997.fingerprint2sleep

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.*
import android.hardware.fingerprint.FingerprintManager
import android.os.CancellationSignal
import android.os.IBinder
import android.support.v7.app.NotificationCompat
import android.util.Log
import com.jarsilio.android.waveup.Root
import com.ztc1997.fingerprint2sleep.aidl.IFPQAService
import org.jetbrains.anko.*

class FPQAService : Service() {
    companion object {
        const val NOTIFICATION_ID = 1
        const val NOTIFICATION_PENDING_INTENT_CONTENT = 0
    }

    var isRunning = false

    var isScanning = false

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
            Log.v("FPQAService", "Received authentication success!")
            isScanning = false
            doOnFingerprintDetected()
        }

        override fun onAuthenticationFailed() {
            super.onAuthenticationFailed()
            Log.v("FPQAService", "Received authentication failed!")
            if (!defaultDPreference.getPrefBoolean(SettingsActivity.PREF_RESPONSE_ENROLLED_FINGERPRINT_ONLY, false))
                doOnFingerprintDetected()
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
            super.onAuthenticationError(errorCode, errString)
            Log.v("FPQAService", "Received authentication error with code $errorCode, and string $errString")
            if (defaultDPreference.getPrefBoolean(SettingsActivity.PREF_NOTIFY_ON_ERROR, false))
                errString?.let { toast(getString(R.string.toast_notify_on_error, it)) }
            isError = true
            isScanning = false
        }

        override fun onAuthenticationHelp(helpCode: Int, helpString: CharSequence?) {
            super.onAuthenticationHelp(helpCode, helpString)
            Log.v("FPQAService", "Received authentication help with code $helpCode, with string $helpString")
            if (defaultDPreference.getPrefBoolean(SettingsActivity.PREF_RESPONSE_ANY_ATTEMPT, false))
                doOnFingerprintDetected()
        }
    }

    val presentReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
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

        rxBus.post(FinishStartFPQAActivityEvent)

        isError = false

        lastIntent?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(it)
            lastIntent = null
        }

        val newFlags = flags or START_STICKY
        return super.onStartCommand(intent, newFlags, startId)
    }

    override fun onCreate() {
        super.onCreate()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    fun doOnFingerprintDetected() {
        when (defaultDPreference.getPrefString(SettingsActivity.PREF_QUICK_ACTION,
                SettingsActivity.VALUES_PREF_QUICK_ACTION_SLEEP)) {
            SettingsActivity.VALUES_PREF_QUICK_ACTION_SLEEP -> goToSleep()
            SettingsActivity.VALUES_PREF_QUICK_ACTION_HOME -> goToHome()
            SettingsActivity.VALUES_PREF_QUICK_ACTION_EXPEND_NOTIFICATIONS_PANEL -> expandNotificationsPanel()
        }
    }

    fun goToSleep() {
        if (defaultDPreference.getPrefBoolean(SettingsActivity.PREF_LOCK_SCREEN_WITH_POWER_BUTTON_AS_ROOT, false))
            doAsync { Root.pressPowerButton() }
        else {
            val componentName = ComponentName(this, AdminReceiver::class.java)
            if (devicePolicyManager.isAdminActive(componentName))
                devicePolicyManager.lockNow()
            else
                RequireAdminActivity.startActivity(ctx)
        }
    }

    fun goToHome() {
        lastIntent = Intent(Intent.ACTION_MAIN)
        lastIntent!!.addCategory(Intent.CATEGORY_HOME)
        StartFPQAActivity.startActivity(ctx)
    }

    fun startFPQA() {
        if (!isRunning) {
            registerReceiver(presentReceiver, IntentFilter(Intent.ACTION_USER_PRESENT))
            registerReceiver(screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))

            isRunning = true
        }
    }

    fun stopFPQA() {
        if (isRunning) {
            unregisterReceiver(presentReceiver)
            unregisterReceiver(screenOffReceiver)

            cancellationSignal.cancel()
            stopForeground(true)
            stopSelf()

            isRunning = false
        }
    }

    fun expandNotificationsPanel() {
        try {
            val service = getSystemService("statusbar")
            val statusBarManager = Class.forName("android.app.StatusBarManager")
            val expand = statusBarManager.getMethod("expandNotificationsPanel")
            expand.invoke(service)
        } catch (e: Exception) {
            toast(R.string.toast_failed_to_expend_notifications_panel)
        }
        StartFPQAActivity.startActivity(ctx)
    }

    fun startForegroundIfSet() = startForegroundIfSet(isError)

    fun startForegroundIfSet(isError: Boolean) {
        if (defaultDPreference.getPrefBoolean(SettingsActivity.PREF_FOREGROUND_SERVICE, false)) {
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

    override fun onBind(intent: Intent): IBinder? {
        return binder
    }
}
