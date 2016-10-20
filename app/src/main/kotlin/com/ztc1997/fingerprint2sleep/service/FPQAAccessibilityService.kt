package com.ztc1997.fingerprint2sleep.service

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.view.accessibility.AccessibilityEvent
import com.eightbitlab.rxbus.Bus
import com.ztc1997.fingerprint2sleep.BuildConfig
import com.ztc1997.fingerprint2sleep.extra.ActivityChangedEvent
import com.ztc1997.fingerprint2sleep.extra.PerformGlobalActionEvent

class FPQAAccessibilityService : AccessibilityService() {
    companion object {
        const val ID = BuildConfig.APPLICATION_ID + "/.service.FPQAAccessibilityService"

        var isNotificationPanelExpanded = false

        var isRunning = false
            private set

        val CLASS_BLACK_LIST by lazy {
            setOf(
                    /* AOSP */
                    "com.android.settings.fingerprint.FingerprintSettings",
                    "com.android.settings.fingerprint.FingerprintEnrollEnrolling",
                    /* MIUI */
                    "com.android.settings.NewFingerprintInternalActivity",
                    "com.miui.applicationlock.ConfirmAccessControl",
                    /* AliPay */
                    "com.alipay.android.app.flybird.ui.window.FlyBirdWindowActivity"
            )
        }
    }

    var lastPkg = ""

    var ignoreOnce = false

    override fun onCreate() {
        super.onCreate()
        Bus.observe<PerformGlobalActionEvent>()
                .subscribe { performGlobalAction(it.action) }
    }

    override fun onDestroy() {
        super.onDestroy()
        Bus.unregister(this)

        isRunning = false
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
                event.className !in CLASS_BLACK_LIST) {

            val componentName = ComponentName(
                    event.packageName.toString(),
                    event.className.toString())

            val activityInfo = tryGetActivity(componentName)

            activityInfo?.let {
                if (ignoreOnce) {
                    ignoreOnce = false
                    return
                }

                val currPkg = activityInfo.packageName

                if (lastPkg != BuildConfig.APPLICATION_ID && currPkg != BuildConfig.APPLICATION_ID)
                    Bus.send(ActivityChangedEvent)

                lastPkg = currPkg
            }

            if (event.packageName == "com.android.systemui") {
                isNotificationPanelExpanded = true
                ignoreOnce = true
            } else isNotificationPanelExpanded = false
        }
    }

    private fun tryGetActivity(componentName: ComponentName): ActivityInfo? {
        try {
            return packageManager.getActivityInfo(componentName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            return null
        }

    }

    override fun onServiceConnected() {
        super.onServiceConnected()

        isRunning = true
    }

    override fun onInterrupt() {
    }
}