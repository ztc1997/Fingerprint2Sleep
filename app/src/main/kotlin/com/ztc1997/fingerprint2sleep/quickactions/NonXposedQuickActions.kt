package com.ztc1997.fingerprint2sleep.quickactions

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import com.eightbitlab.rxbus.Bus
import com.ztc1997.fingerprint2sleep.App
import com.ztc1997.fingerprint2sleep.R
import com.ztc1997.fingerprint2sleep.activity.RequireAdminActivity
import com.ztc1997.fingerprint2sleep.activity.SettingsActivity
import com.ztc1997.fingerprint2sleep.activity.ShortenTimeOutActivity
import com.ztc1997.fingerprint2sleep.extension.execute
import com.ztc1997.fingerprint2sleep.extension.root
import com.ztc1997.fingerprint2sleep.extra.PerformGlobalActionEvent
import com.ztc1997.fingerprint2sleep.receiver.AdminReceiver
import com.ztc1997.fingerprint2sleep.service.FPQAAccessibilityService
import org.jetbrains.anko.async
import org.jetbrains.anko.devicePolicyManager
import org.jetbrains.anko.onUiThread
import org.jetbrains.anko.toast

object NonXposedQuickActions : IQuickActions {

    private lateinit var app: App

    private val POWER_KEY_CMD = "input keyevent 26"

    fun inject(app: App) {
        this.app = app
    }

    override fun collapsePanels() {
        try {
            val service = app.getSystemService("statusbar")
            val statusBarManager = Class.forName("android.app.StatusBarManager")
            val method = statusBarManager.getMethod("collapsePanels")
            method.invoke(service)
        } catch (e: Exception) {
            app.toast(R.string.toast_failed_to_collapse_panel)
        }
    }

    override fun expandNotificationsPanel() {
        Bus.send(PerformGlobalActionEvent(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS))
    }

    override fun toggleNotificationsPanel() {
        if (FPQAAccessibilityService.isNotificationPanelExpanded)
            collapsePanels()
        else
            expandNotificationsPanel()
    }

    override fun actionHome() {
        Bus.send(PerformGlobalActionEvent(AccessibilityService.GLOBAL_ACTION_HOME))
    }

    override fun actionBack() {
        Bus.send(PerformGlobalActionEvent(AccessibilityService.GLOBAL_ACTION_BACK))
    }

    override fun actionRecents() {
        Bus.send(PerformGlobalActionEvent(AccessibilityService.GLOBAL_ACTION_RECENTS))
    }

    override fun actionPowerDialog() {
        Bus.send(PerformGlobalActionEvent(AccessibilityService.GLOBAL_ACTION_POWER_DIALOG))
    }

    override fun actionToggleSplitScreen() {
        Bus.send(PerformGlobalActionEvent(AccessibilityService.GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN))
    }

    override fun actionQuickSettings() {
        Bus.send(PerformGlobalActionEvent(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS))
    }

    override fun goToSleep() {
        when (app.defaultDPreference.getPrefString(SettingsActivity.PREF_SCREEN_OFF_METHOD,
                SettingsActivity.VALUES_PREF_SCREEN_OFF_METHOD_SHORTEN_TIMEOUT)) {
            SettingsActivity.VALUES_PREF_SCREEN_OFF_METHOD_SHORTEN_TIMEOUT ->
                ShortenTimeOutActivity.startActivity(app)

            SettingsActivity.VALUES_PREF_SCREEN_OFF_METHOD_DEVICE_ADMIN -> {
                val componentName = ComponentName(app, AdminReceiver::class.java)
                if (app.devicePolicyManager.isAdminActive(componentName))
                    app.devicePolicyManager.lockNow()
                else
                    RequireAdminActivity.startActivity(app)
            }
            SettingsActivity.VALUES_PREF_SCREEN_OFF_METHOD_POWER_BUTTON -> pressPowerButton()
        }
    }

    fun pressPowerButton() {
        async() {
            if (root.isStarted)
                root.execute(POWER_KEY_CMD)
            else
                app.onUiThread { app.toast(R.string.toast_root_access_failed) }
        }
    }
}