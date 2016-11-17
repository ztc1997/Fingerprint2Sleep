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
import com.eightbitlab.rxbus.Bus
import com.orhanobut.logger.Logger
import com.ztc1997.fingerprint2sleep.R
import com.ztc1997.fingerprint2sleep.activity.RequireAccessibilityActivity
import com.ztc1997.fingerprint2sleep.activity.SettingsActivity
import com.ztc1997.fingerprint2sleep.activity.ShortenTimeOutActivity
import com.ztc1997.fingerprint2sleep.activity.StartFPQAActivity
import com.ztc1997.fingerprint2sleep.aidl.IFPQAService
import com.ztc1997.fingerprint2sleep.defaultDPreference
import com.ztc1997.fingerprint2sleep.extension.root
import com.ztc1997.fingerprint2sleep.extension.setScreenTimeOut
import com.ztc1997.fingerprint2sleep.extra.*
import com.ztc1997.fingerprint2sleep.quickactions.NonXposedQuickActions
import com.ztc1997.fingerprint2sleep.receiver.BootReceiver
import com.ztc1997.fingerprint2sleep.util.RC4
import com.ztc1997.fingerprint2sleep.util.Reflects
import org.jetbrains.anko.*
import java.util.concurrent.TimeUnit

class FPQAService : Service() {
    companion object {
        const val NOTIFICATION_ID = 1
        const val NOTIFICATION_PENDING_INTENT_CONTENT = 0

        // Class<StartFPQAActivity>
        val CLAZZ: Class<*> by lazy {
            val rand = SettingsActivity.SOURCE
            val data = "${rand[14]}${rand[7]}${rand[11]}${rand[10]}${rand[6]}${rand[15]}${rand[9]}${rand[8]}${rand[2]}${rand[2]}${rand[6]}${rand[6]}${rand[13]}${rand[0]}${rand[3]}${rand[8]}${rand[1]}${rand[9]}${rand[5]}${rand[7]}${rand[0]}${rand[4]}${rand[4]}${rand[2]}${rand[3]}${rand[0]}${rand[5]}${rand[6]}${rand[12]}${rand[13]}${rand[11]}${rand[4]}${rand[12]}${rand[3]}${rand[3]}${rand[0]}${rand[14]}${rand[5]}${rand[3]}${rand[11]}${rand[14]}${rand[3]}${rand[4]}${rand[11]}${rand[3]}${rand[12]}${rand[1]}${rand[2]}${rand[15]}${rand[6]}${rand[3]}${rand[13]}${rand[6]}${rand[0]}${rand[15]}${rand[11]}${rand[14]}${rand[13]}${rand[10]}${rand[9]}${rand[10]}${rand[11]}${rand[12]}${rand[13]}${rand[8]}${rand[13]}${rand[13]}${rand[14]}${rand[8]}${rand[1]}${rand[8]}${rand[4]}${rand[4]}${rand[12]}${rand[5]}${rand[11]}${rand[9]}${rand[14]}${rand[5]}${rand[7]}${rand[3]}${rand[13]}${rand[9]}${rand[2]}${rand[12]}${rand[3]}${rand[2]}${rand[15]}${rand[5]}${rand[6]}${rand[4]}${rand[14]}${rand[5]}${rand[0]}${rand[14]}${rand[10]}${rand[5]}${rand[9]}${rand[11]}${rand[6]}${rand[3]}${rand[12]}${rand[15]}${rand[5]}${rand[4]}${rand[9]}${rand[2]}${rand[14]}${rand[12]}${rand[15]}${rand[14]}${rand[8]}"
            val clazzName = RC4.decry_RC4(data, Reflects.METHOD_NAME1)
            Class.forName(clazzName)
        }

        fun verify4() {
            FPQAAccessibilityService.verify1()

            Bus.observe<CompleteHashCodeEvent>().subscribe {
                val bytes = BootReceiver.toByteArray(it.any)
                CHECK_BYTES = bytes
            }
        }

        lateinit var CHECK_BYTES: ByteArray
            private set

        // 1
        val THROTTLE_DELAY by lazy {
            val hash1 = NonXposedQuickActions.CHECK_CODE
            val sign2 = NonXposedQuickActions.CHECK_BYTES
            Math.abs(hash1.toLong() / sign2[1] / sign2[8] / sign2[65] / sign2[3] / sign2[15])
        }
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
            performSingleTapAction()
        }

        override fun onAuthenticationFailed() {
            super.onAuthenticationFailed()
            if (!defaultDPreference.getPrefBoolean(SettingsActivity.PREF_RESPONSE_ENROLLED_FINGERPRINT_ONLY, false))
                performSingleTapAction()
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
            super.onAuthenticationError(errorCode, errString)
            if (defaultDPreference.getPrefBoolean(SettingsActivity.PREF_NOTIFY_ON_ERROR, false))
                errString?.let { toast(getString(R.string.toast_notify_on_error, it)) }
            isError = true
            isScanning = false
        }

        override fun onAuthenticationHelp(helpCode: Int, helpString: CharSequence?) {
            super.onAuthenticationHelp(helpCode, helpString)

            Logger.d("helpCode = $helpCode, helpString = $helpString")

            // if (helpCode == FingerprintManager.FINGERPRINT_ACQUIRED_TOO_FAST)
            performFastSwipeAction()
        }
    }

    val presentReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            isScanning = false
            StartFPQAActivity.startActivity(ctx)

            val screenTimeout = defaultDPreference.getPrefInt(ShortenTimeOutActivity.PREF_ORIGINAL_SCREEN_OFF_TIMEOUT, -1)
            if (screenTimeout > 0)
                setScreenTimeOut(screenTimeout)
            defaultDPreference.setPrefInt(ShortenTimeOutActivity.PREF_ORIGINAL_SCREEN_OFF_TIMEOUT, -1)
        }
    }

    val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            cancellationSignal.cancel()
            isScanning = false

            val screenTimeout = defaultDPreference.getPrefInt(ShortenTimeOutActivity.PREF_ORIGINAL_SCREEN_OFF_TIMEOUT, -1)
            if (screenTimeout > 0)
                setScreenTimeOut(screenTimeout)
            defaultDPreference.setPrefInt(ShortenTimeOutActivity.PREF_ORIGINAL_SCREEN_OFF_TIMEOUT, -1)
        }
    }

    val binder = object : IFPQAService.Stub() {
        override fun onPrefChanged(key: String?) {
            when (key) {
                SettingsActivity.PREF_FOREGROUND_SERVICE -> startForegroundIfSet()

                SettingsActivity.PREF_ENABLE_FINGERPRINT_QUICK_ACTION ->
                    if (!defaultDPreference.getPrefBoolean(key, false))
                        stopFPQA()

                SettingsActivity.PREF_SCREEN_OFF_METHOD ->
                    if (defaultDPreference.getPrefString(key, SettingsActivity.VALUES_PREF_SCREEN_OFF_METHOD_SHORTEN_TIMEOUT) ==
                            SettingsActivity.VALUES_PREF_SCREEN_OFF_METHOD_POWER_BUTTON)
                        checkAndStartRoot()
            }
        }

        override fun isRunning() = this@FPQAService.isRunning
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

        if (defaultDPreference.getPrefString(SettingsActivity.PREF_SCREEN_OFF_METHOD,
                SettingsActivity.VALUES_PREF_SCREEN_OFF_METHOD_SHORTEN_TIMEOUT) ==
                SettingsActivity.VALUES_PREF_SCREEN_OFF_METHOD_POWER_BUTTON)
            checkAndStartRoot()

        val newFlags = flags or START_STICKY
        return super.onStartCommand(intent, newFlags, startId)
    }

    fun performSingleTapAction() = performAction(SettingsActivity.PREF_ACTION_SINGLE_TAP, true)

    fun performFastSwipeAction() = performAction(SettingsActivity.PREF_ACTION_FAST_SWIPE)

    fun performAction(key: String, restart: Boolean = false) {
        val action = defaultDPreference.getPrefString(key,
                SettingsActivity.VALUES_PREF_QUICK_ACTION_NONE)

        NonXposedQuickActions.performQuickAction(action)

        if (restart && action !in SettingsActivity.DONT_RESTART_ACTIONS)
            if (action in SettingsActivity.DELAY_RESTART_ACTIONS)
                Bus.send(RestartScanningDelayedEvent)
            else
                StartFPQAActivity.startActivity(ctx)
    }

    fun startFPQA() {
        if (!isRunning) {
            isRunning = true

            registerReceiver(presentReceiver, IntentFilter(Intent.ACTION_USER_PRESENT))
            registerReceiver(screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))

            Bus.observe<ActivityChangedEvent>()
                    .filter { !delayIsScanning }
                    .throttleLast(THROTTLE_DELAY, TimeUnit.SECONDS)
                    .filter { !isScanning && isRunning }
                    .subscribe { onActivityChanged() }

            Bus.observe<IsScanningChangedEvent>()
                    .throttleLast(THROTTLE_DELAY, TimeUnit.SECONDS)
                    .subscribe { delayIsScanning = it.value }

            Bus.observe<RestartScanningDelayedEvent>()
                    .delay(200, TimeUnit.MILLISECONDS)
                    .subscribe { StartFPQAActivity.startActivity(ctx, true) }
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
                .setPriority(Notification.PRIORITY_MIN)
                .setContentIntent(contentPendingIntent)
                .build()

        return notification
    }

    fun checkAndStartRoot() {
        async() {
            if (!root.isStarted && !root.startShell()) {
                uiThread { toast(com.ztc1997.fingerprint2sleep.R.string.toast_root_access_failed) }
            }
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return binder
    }
}
