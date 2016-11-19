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
import com.ztc1997.fingerprint2sleep.app
import com.ztc1997.fingerprint2sleep.defaultDPreference
import com.ztc1997.fingerprint2sleep.extension.root
import com.ztc1997.fingerprint2sleep.extension.setScreenTimeOut
import com.ztc1997.fingerprint2sleep.extra.ActivityChangedEvent
import com.ztc1997.fingerprint2sleep.extra.FinishStartFPQAActivityEvent
import com.ztc1997.fingerprint2sleep.extra.IsScanningChangedEvent
import com.ztc1997.fingerprint2sleep.extra.RestartScanningDelayedEvent
import com.ztc1997.fingerprint2sleep.quickactions.NonXposedQuickActions
import org.jetbrains.anko.*
import java.util.concurrent.TimeUnit

class FPQAService : Service() {
    companion object {
        const val NOTIFICATION_ID = 1
        const val NOTIFICATION_PENDING_INTENT_CONTENT = 0

        val ACTION_RESTART_SCANNING = FPQAService::class.java.name + "ACTION_RESTART_SCANNING"

        val CLASS_BLACK_LIST by lazy {
            setOf(
                    ShortenTimeOutActivity::class.java.name,
                    /* AOSP */
                    "com.android.settings.fingerprint.FingerprintSettings",
                    "com.android.settings.fingerprint.FingerprintEnrollEnrolling",
                    "com.android.systemui.recents.RecentsActivity",
                    /* MIUI */
                    "com.android.settings.NewFingerprintInternalActivity",
                    "com.miui.applicationlock.ConfirmAccessControl",
                    /* AliPay */
                    "com.alipay.android.app.flybird.ui.window.FlyBirdWindowActivity"
            )
        }

        var isServiceRunning = false
            private set
    }

    var isRunning = false

    // var delayIsScanning = false

    var isScanning = false
        set(value) {
            field = value
            if (value)
            // delayIsScanning = value
            else
                Bus.send(IsScanningChangedEvent(value))
        }

    var isError = false
        set(value) {
            field = value
            if (value)
                Bus.send(OnAuthenticationErrorEvent)
            else
                startForegroundIfSet()
        }

    var errString = ""

    var lastPkgName = ""

    var errorPkgName = ""

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

            this@FPQAService.errString = errString?.toString().orEmpty()

            isError = true
            isScanning = false

            errorPkgName = lastPkgName
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

            Bus.send(RestartScanningDelayedEvent)

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

    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true
    }

    override fun onDestroy() {
        super.onDestroy()

        stopFPQA()

        isServiceRunning = false
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

            val restartIntentFilter = IntentFilter(Intent.ACTION_USER_PRESENT)
            restartIntentFilter.addAction(ACTION_RESTART_SCANNING)
            registerReceiver(presentReceiver, restartIntentFilter)

            registerReceiver(screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))

            Bus.observe<ActivityChangedEvent>()
                    .doOnEach { lastPkgName = (it.value as ActivityChangedEvent).event.packageName.toString() }
                    .filter { it.event.packageName.toString() != errorPkgName }
                    .filter { it.event.className !in CLASS_BLACK_LIST }
                    // .filter { !delayIsScanning }
                    .throttleLast(200, TimeUnit.MILLISECONDS)
                    .filter { !isScanning && isRunning }
                    .subscribe { onActivityChanged() }

            // Bus.observe<IsScanningChangedEvent>()
            //         .throttleLast(THROTTLE_DELAY, TimeUnit.MILLISECONDS)
            //         .subscribe { delayIsScanning = it.value }

            Bus.observe<OnAuthenticationErrorEvent>()
                    .delay(200, TimeUnit.MILLISECONDS)
                    .filter { !isScanning }
                    .subscribe { startForegroundIfSet() }

            Bus.observe<RestartScanningDelayedEvent>()
                    .delay(200, TimeUnit.MILLISECONDS)
                    .filter { !isScanning && isRunning }
                    .subscribe { StartFPQAActivity.startActivity(ctx, true) }
        }
    }

    fun stopFPQA() {
        if (isRunning) {
            isRunning = false

            unregisterReceiver(presentReceiver)
            unregisterReceiver(screenOffReceiver)

            Bus.unregister(this)
            // delayIsScanning = isScanning

            cancellationSignal.cancel()
            stopForeground(true)
            stopSelf()
        }
    }

    fun onActivityChanged() {
        StartFPQAActivity.startActivity(ctx)
    }

    fun startForegroundIfSet() {
        if (isRunning && defaultDPreference.getPrefBoolean(SettingsActivity.PREF_FOREGROUND_SERVICE, false)) {

            val notification = if (isError)
                generateNotification(getString(R.string.notification_content_text_retry) + errString, true)
            else generateNotification(R.string.notification_content_text)

            startForeground(NOTIFICATION_ID, notification)
        } else {
            stopForeground(true)
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    inline fun generateNotification(textRes: Int) = generateNotification(getString(textRes))

    fun generateNotification(text: CharSequence, restart: Boolean = false): Notification {
        val pendingIntent = if (restart)
            PendingIntent.getBroadcast(app,
                    NOTIFICATION_PENDING_INTENT_CONTENT, Intent(ACTION_RESTART_SCANNING),
                    PendingIntent.FLAG_UPDATE_CURRENT)
        else
            PendingIntent.getActivity(app,
                    NOTIFICATION_PENDING_INTENT_CONTENT, Intent(app, SettingsActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT)

        val notification = Notification.Builder(applicationContext)
                .setSmallIcon(R.drawable.ic_fingerprint_white_24dp)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(text)
                .setPriority(Notification.PRIORITY_MIN)
                .setContentIntent(pendingIntent)
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

    object OnAuthenticationErrorEvent
}
