package com.ztc1997.fingerprint2sleep.util

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import com.eightbitlab.rxbus.Bus
import com.ztc1997.fingerprint2sleep.App
import com.ztc1997.fingerprint2sleep.R
import com.ztc1997.fingerprint2sleep.activity.RequireAdminActivity
import com.ztc1997.fingerprint2sleep.activity.SettingsActivity
import com.ztc1997.fingerprint2sleep.extension.execute
import com.ztc1997.fingerprint2sleep.extension.root
import com.ztc1997.fingerprint2sleep.extra.PerformGlobalActionEvent
import com.ztc1997.fingerprint2sleep.receiver.AdminReceiver
import com.ztc1997.fingerprint2sleep.service.FPQAAccessibilityService
import org.jetbrains.anko.devicePolicyManager
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import org.jetbrains.anko.uiThread

object QuickActions {
    private lateinit var app: App

    fun inject(app: App) {
        this.app = app
    }

    fun collapsePanels() {
        try {
            val service = app.getSystemService("statusbar")
            val statusBarManager = Class.forName("android.app.StatusBarManager")
            val method = statusBarManager.getMethod("collapsePanels")
            method.invoke(service)
        } catch (e: Exception) {
            app.toast(R.string.toast_failed_to_collapse_panel)
        }
    }

    fun expandNotificationsPanel() {
        Bus.send(PerformGlobalActionEvent(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS))
    }

    fun toggleNotificationsPanel() {
        if (FPQAAccessibilityService.isNotificationPanelExpanded)
            collapsePanels()
        else
            expandNotificationsPanel()
    }

    fun goToHome() {
        Bus.send(PerformGlobalActionEvent(AccessibilityService.GLOBAL_ACTION_HOME))
    }

    fun goToSleep() {
        if (app.defaultDPreference.getPrefBoolean(SettingsActivity.PREF_LOCK_SCREEN_WITH_POWER_BUTTON_AS_ROOT, false))
            pressPowerButton()
        else {
            val componentName = ComponentName(app, AdminReceiver::class.java)
            if (app.devicePolicyManager.isAdminActive(componentName))
                app.devicePolicyManager.lockNow()
            else
                RequireAdminActivity.startActivity(app)
        }
    }

    fun pressPowerButton() {
        doAsync {
            if (root.isStarted)
                root.execute("input keyevent 26")
            else
                uiThread { app.toast(com.ztc1997.fingerprint2sleep.R.string.toast_root_access_failed) }
        }
    }
}