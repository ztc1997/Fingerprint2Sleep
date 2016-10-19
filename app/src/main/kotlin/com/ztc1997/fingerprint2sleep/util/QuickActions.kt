package com.ztc1997.fingerprint2sleep.util

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import com.eightbitlab.rxbus.Bus
import com.jarsilio.android.waveup.Root
import com.ztc1997.fingerprint2sleep.App
import com.ztc1997.fingerprint2sleep.R
import com.ztc1997.fingerprint2sleep.activity.RequireAdminActivity
import com.ztc1997.fingerprint2sleep.activity.SettingsActivity
import com.ztc1997.fingerprint2sleep.extra.PerformGlobalActionEvent
import com.ztc1997.fingerprint2sleep.receiver.AdminReceiver
import org.jetbrains.anko.devicePolicyManager
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast

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
            app.toast(R.string.toast_failed_to_expend_notifications_panel)
        }
    }

    fun expandNotificationsPanel() {
        Bus.send(PerformGlobalActionEvent(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS))
    }

    fun goToHome() {
        Bus.send(PerformGlobalActionEvent(AccessibilityService.GLOBAL_ACTION_HOME))
    }

    fun goToSleep() {
        if (app.defaultDPreference.getPrefBoolean(SettingsActivity.PREF_LOCK_SCREEN_WITH_POWER_BUTTON_AS_ROOT, false))
            doAsync { Root.pressPowerButton() }
        else {
            val componentName = ComponentName(app, AdminReceiver::class.java)
            if (app.devicePolicyManager.isAdminActive(componentName))
                app.devicePolicyManager.lockNow()
            else
                RequireAdminActivity.startActivity(app)
        }
    }
}