package com.ztc1997.fingerprint2sleep.service

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.view.accessibility.AccessibilityEvent
import com.eightbitlab.rxbus.Bus
import com.ztc1997.fingerprint2sleep.BuildConfig
import com.ztc1997.fingerprint2sleep.activity.SettingsActivity
import com.ztc1997.fingerprint2sleep.activity.StartFPQAActivity
import com.ztc1997.fingerprint2sleep.extra.ActivityChangedEvent
import com.ztc1997.fingerprint2sleep.extra.PerformGlobalActionEvent
import org.jetbrains.anko.ctx
import org.jetbrains.anko.defaultSharedPreferences

class FPQAAccessibilityService : AccessibilityService() {
    companion object {
        const val ID = BuildConfig.APPLICATION_ID + "/.service.FPQAAccessibilityService"

        var isNotificationPanelExpanded = false
            private set

        var isRunning = false
            private set

        // com.android.systemui
    }

    var lastClass = ""

    var ignoreOnce = false

    var recentsShowing = false

    override fun onCreate() {
        super.onCreate()
        Bus.observe<PerformGlobalActionEvent>()
                .subscribe {
                    val action = it.action
                    if (action == GLOBAL_ACTION_RECENTS)
                        recentsShowing = true
                    performGlobalAction(action)
                }
    }

    override fun onDestroy() {
        super.onDestroy()
        Bus.unregister(this)

        isRunning = false
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {

            if (!FPQAService.isServiceRunning &&
                    defaultSharedPreferences.getBoolean(SettingsActivity.PREF_ENABLE_FINGERPRINT_QUICK_ACTION, false))
                StartFPQAActivity.startActivity(ctx)

            if (event.packageName == null || event.className == null) return

            val componentName = ComponentName(
                    event.packageName.toString(),
                    event.className.toString())

            val activityInfo = tryGetActivity(componentName)

            if (activityInfo != null) {
                val currClass = event.className.toString()
                val clazz = StartFPQAActivity::class.java.name

                if (ignoreOnce)
                    ignoreOnce = false
                else if (currClass != clazz
                        && lastClass != clazz)
                    Bus.send(ActivityChangedEvent(event))


                lastClass = currClass

                isNotificationPanelExpanded = false

            } else if (event.packageName == "com.android.systemui" && !recentsShowing) {
                isNotificationPanelExpanded = true
                ignoreOnce = true
            } else {
                isNotificationPanelExpanded = false
                recentsShowing = false
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

        isRunning = true

        if (defaultSharedPreferences.getBoolean(SettingsActivity.PREF_ENABLE_FINGERPRINT_QUICK_ACTION, false))
            StartFPQAActivity.startActivity(ctx)
    }

    override fun onInterrupt() {
    }
}