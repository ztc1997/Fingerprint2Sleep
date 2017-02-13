package com.ztc1997.fingerprint2sleep.extra

import android.hardware.fingerprint.FingerprintManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.ztc1997.fingerprint2sleep.activity.SettingsActivity
import com.ztc1997.fingerprint2sleep.quickactions.IQuickActions

abstract class GestureAuthenticationCallback(val quickActions: IQuickActions) : FingerprintManager.AuthenticationCallback() {
    enum class EventType {
        SingleTap,
        Unregistered,
        FastSwipe,
        DoubleTap
    }

    private var pendingEvent: EventType? = null

    private val handler = Handler(Looper.getMainLooper())

    private val runnable = Runnable {
        handleEvent(pendingEvent)
    }

    open val doubleTapEnabled: Boolean get() = quickActions.preference?.run {
        getPrefBoolean(SettingsActivity.PREF_ENABLE_DOUBLE_TAP, false)
    } ?: false

    open val responseEnrolledFingerprintOnly: Boolean get() = quickActions.preference?.run {
        getPrefBoolean(SettingsActivity.PREF_RESPONSE_ENROLLED_FINGERPRINT_ONLY, false)
    } ?: false

    open val doubleTapInterval: Long get() = quickActions.preference?.run {
        getPrefString(SettingsActivity.PREF_DOUBLE_TAP_INTERVAL, "500").toLong()
    } ?: 500

    override fun onAuthenticationSucceeded(result: FingerprintManager.AuthenticationResult?) {
        super.onAuthenticationSucceeded(result)
        onEvent(EventType.SingleTap)
    }

    override fun onAuthenticationHelp(helpCode: Int, helpString: CharSequence?) {
        super.onAuthenticationHelp(helpCode, helpString)
        onEvent(EventType.FastSwipe)
    }

    override fun onAuthenticationFailed() {
        super.onAuthenticationFailed()
        onEvent(EventType.Unregistered)
    }

    private fun onEvent(event: EventType) {
        Log.d("GestureAC", "onEvent(${event.name})")
        if (!doubleTapEnabled) {
            val action = handleEvent(event)
            if (event == EventType.SingleTap)
                restartScanning(action)
        } else if (pendingEvent == null) {
            pendingEvent = event
            handler.postDelayed(runnable, doubleTapInterval)
            if (event == EventType.SingleTap)
                restartScanning(null)
        } else {
            pendingEvent = null
            handler.removeCallbacks(runnable)
            val action = handleEvent(EventType.DoubleTap)
            if (event == EventType.SingleTap)
                restartScanning(action)
        }
    }

    private fun handleEvent(event: EventType?): String? {
        var action: String? = null

        event?.let {
            var event1 = event
            if (event == EventType.Unregistered) {
                if (responseEnrolledFingerprintOnly) return@let
                event1 = EventType.SingleTap
            }
            action = quickActions.performQuickAction(event1)
        }

        pendingEvent = null

        return action
    }

    abstract fun restartScanning(action: String?)
}