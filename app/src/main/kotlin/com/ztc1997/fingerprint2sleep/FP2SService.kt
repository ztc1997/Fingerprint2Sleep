package com.ztc1997.fingerprint2sleep

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.*
import android.hardware.fingerprint.FingerprintManager
import android.os.CancellationSignal
import android.os.IBinder
import android.support.v7.app.NotificationCompat
import com.jarsilio.android.waveup.Root
import org.jetbrains.anko.*

class FP2SService : Service(), SharedPreferences.OnSharedPreferenceChangeListener {
    companion object {
        const val NOTIFICATION_ID = 1
        const val NOTIFICATION_PENDING_INTENT_CONTENT = 0

        var isRunning = false
    }

    var isError = false
        set(value) {
            field = value
            startForegroundIfSet(value)
        }

    var cancellationSignal = CancellationSignal()

    val authenticationCallback = object : FingerprintManager.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: FingerprintManager.AuthenticationResult?) {
            super.onAuthenticationSucceeded(result)
            goToSleep()
        }

        override fun onAuthenticationFailed() {
            super.onAuthenticationFailed()
            if (!defaultSharedPreferences.getBoolean(SettingsActivity.PREF_RESPONSE_ENROLLED_FINGERPRINT_ONLY, false))
                goToSleep()
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
            super.onAuthenticationError(errorCode, errString)
            if (defaultSharedPreferences.getBoolean(SettingsActivity.PREF_NOTIFY_ON_ERROR, false))
                errString?.let { toast(getString(R.string.toast_notify_on_error, it)) }
            isError = true
        }
    }

    val presentReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            val startIntent = Intent(context, SplashActivity::class.java)
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(startIntent)
        }
    }

    val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            cancellationSignal.cancel()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (cancellationSignal.isCanceled) cancellationSignal = CancellationSignal()
        fingerprintManager.authenticate(null, cancellationSignal, 0, authenticationCallback, null)

        isError = false

        val newFlags = flags or START_STICKY
        return super.onStartCommand(intent, newFlags, startId)
    }

    override fun onCreate() {
        super.onCreate()
        registerReceiver(presentReceiver, IntentFilter(Intent.ACTION_USER_PRESENT))
        registerReceiver(screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))
        defaultSharedPreferences.registerOnSharedPreferenceChangeListener(this)

        isRunning = true
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(presentReceiver)
        unregisterReceiver(screenOffReceiver)
        defaultSharedPreferences.unregisterOnSharedPreferenceChangeListener(this)

        cancellationSignal.cancel()
        stopForeground(true)

        isRunning = false
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        when (key) {
            SettingsActivity.PREF_FOREGROUND_SERVICE -> startForegroundIfSet()

            SettingsActivity.PREF_ENABLE_FINGERPRINT2SLEEP ->
                if (!sharedPreferences.getBoolean(key, false))
                    stopSelf()
        }
    }

    fun goToSleep() {
        if (defaultSharedPreferences.getBoolean(SettingsActivity.PREF_LOCK_SCREEN_WITH_POWER_BUTTON_AS_ROOT, false))
            async() { Root.pressPowerButton() }
        else
            devicePolicyManager.lockNow()
    }

    fun startForegroundIfSet() = startForegroundIfSet(isError)

    fun startForegroundIfSet(isError: Boolean) {
        if (defaultSharedPreferences.getBoolean(SettingsActivity.PREF_FOREGROUND_SERVICE, false)) {
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
        return null
    }
}
