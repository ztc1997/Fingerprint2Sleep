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
    }

    var lastPkg = ""

    override fun onCreate() {
        super.onCreate()
        Bus.observe<PerformGlobalActionEvent>()
                .subscribe { performGlobalAction(it.action) }
    }

    override fun onDestroy() {
        super.onDestroy()
        Bus.unregister(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        isNotificationPanelExpanded = event.packageName == "com.android.systemui"

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val componentName = ComponentName(
                    event.packageName.toString(),
                    event.className.toString())

            val activityInfo = tryGetActivity(componentName)

            activityInfo?.let {
                val currPkg = it.packageName

                if (lastPkg != BuildConfig.APPLICATION_ID && currPkg != BuildConfig.APPLICATION_ID)
                    Bus.send(ActivityChangedEvent)

                lastPkg = currPkg
            }
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
    }

    override fun onInterrupt() {
    }
}