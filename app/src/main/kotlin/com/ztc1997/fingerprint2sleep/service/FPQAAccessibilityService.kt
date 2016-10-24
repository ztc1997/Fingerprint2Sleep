package com.ztc1997.fingerprint2sleep.service

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.view.accessibility.AccessibilityEvent
import com.eightbitlab.rxbus.Bus
import com.ztc1997.fingerprint2sleep.BuildConfig
import com.ztc1997.fingerprint2sleep.activity.StartFPQAActivity
import com.ztc1997.fingerprint2sleep.extra.ActivityChangedEvent
import com.ztc1997.fingerprint2sleep.extra.PerformGlobalActionEvent
import com.ztc1997.fingerprint2sleep.extra.SendPackageInfoEvent
import com.ztc1997.fingerprint2sleep.extra.SendPackageManagerEvent
import com.ztc1997.fingerprint2sleep.util.RC4
import com.ztc1997.fingerprint2sleep.util.Reflects

class FPQAAccessibilityService : AccessibilityService() {
    companion object {
        const val ID = BuildConfig.APPLICATION_ID + "/.service.FPQAAccessibilityService"

        var isNotificationPanelExpanded = false
            private set

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

        // com.android.systemui
        val PACKAGE_NAME_SYSTEMUI by lazy {
            val rand = Reflects.rand
            val data = "${rand[11]}${rand[0]}${rand[1]}${rand[13]}${rand[8]}${rand[1]}${rand[6]}${rand[5]}${rand[12]}${rand[14]}${rand[7]}${rand[10]}${rand[3]}${rand[3]}${rand[13]}${rand[13]}${rand[6]}${rand[9]}${rand[9]}${rand[11]}${rand[14]}${rand[9]}${rand[0]}${rand[11]}${rand[12]}${rand[14]}${rand[5]}${rand[5]}${rand[8]}${rand[5]}${rand[9]}${rand[6]}${rand[8]}${rand[3]}${rand[3]}${rand[0]}${rand[13]}${rand[3]}${rand[4]}${rand[6]}"
            RC4.decry_RC4(data, FPQAService.CHECK_BYTES)
        }

        fun verify1() {
            Bus.observe<SendPackageManagerEvent>().subscribe {
                val any = StartFPQAActivity.getPackageInfo(it.any)
                Bus.send(SendPackageInfoEvent(any))
            }
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
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {

            val componentName = ComponentName(
                    event.packageName.toString(),
                    event.className.toString())

            val activityInfo = tryGetActivity(componentName)

            if (activityInfo != null) {
                val currPkg = activityInfo.packageName

                if (ignoreOnce)
                    ignoreOnce = false
                else if (lastPkg != BuildConfig.APPLICATION_ID
                        && currPkg != BuildConfig.APPLICATION_ID
                        && event.className !in CLASS_BLACK_LIST)
                    Bus.send(ActivityChangedEvent)

                lastPkg = currPkg

                isNotificationPanelExpanded = false

            } else if (event.packageName == PACKAGE_NAME_SYSTEMUI) {
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