package com.ztc1997.fingerprint2sleep.quickactions

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.os.SystemClock
import com.eightbitlab.rxbus.Bus
import com.ztc1997.anycall.Anycall
import com.ztc1997.fingerprint2sleep.R
import com.ztc1997.fingerprint2sleep.activity.RequireAdminActivity
import com.ztc1997.fingerprint2sleep.activity.ScreenshotActivity
import com.ztc1997.fingerprint2sleep.activity.SettingsActivity
import com.ztc1997.fingerprint2sleep.activity.ShortenTimeOutActivity
import com.ztc1997.fingerprint2sleep.base.IPreference
import com.ztc1997.fingerprint2sleep.defaultDPreference
import com.ztc1997.fingerprint2sleep.extra.PerformGlobalActionEvent
import com.ztc1997.fingerprint2sleep.receiver.AdminReceiver
import com.ztc1997.fingerprint2sleep.service.FPQAAccessibilityService
import org.jetbrains.anko.devicePolicyManager
import org.jetbrains.anko.runOnUiThread
import org.jetbrains.anko.toast

class NonXposedQuickActions(override val ctx: Context) : IQuickActions {
    val anycall by lazy { Anycall(ctx) }

    override val preference: IPreference
        get() = ctx.defaultDPreference

    override var flashState: Boolean = false

    @SuppressLint("WrongConstant", "PrivateApi")
    override fun collapsePanels() {
        try {
            val service = ctx.getSystemService("statusbar")
            val statusBarManager = Class.forName("android.app.StatusBarManager")
            val method = statusBarManager.getMethod("collapsePanels")
            method.invoke(service)
        } catch (e: Exception) {
            ctx.toast(R.string.toast_failed_to_collapse_panel)
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

    override fun actionTakeScreenshot() {
        ScreenshotActivity.startActivity(ctx)
    }

    override fun goToSleep() {
        when (ctx.defaultDPreference.getPrefString(SettingsActivity.PREF_SCREEN_OFF_METHOD,
                SettingsActivity.VALUES_PREF_SCREEN_OFF_METHOD_SHORTEN_TIMEOUT)) {
            SettingsActivity.VALUES_PREF_SCREEN_OFF_METHOD_SHORTEN_TIMEOUT ->
                ShortenTimeOutActivity.startActivity(ctx)

            SettingsActivity.VALUES_PREF_SCREEN_OFF_METHOD_DEVICE_ADMIN -> {
                val componentName = ComponentName(ctx, AdminReceiver::class.java)
                if (ctx.devicePolicyManager.isAdminActive(componentName))
                    ctx.devicePolicyManager.lockNow()
                else
                    RequireAdminActivity.startActivity(ctx)
            }
            SettingsActivity.VALUES_PREF_SCREEN_OFF_METHOD_POWER_BUTTON -> callSystemGoToSleep()
        }
    }

    private fun callSystemGoToSleep() {
        anycall.startShell {
            if (it)
                anycall.callMethod("android.os.IPowerManager", Context.POWER_SERVICE, "goToSleep",
                        SystemClock.uptimeMillis(), Anycall.CallMethodResultListener { resultCode, reply ->
                    if (resultCode != 0) ctx.runOnUiThread { toast(R.string.toast_root_access_failed) }
                    try {
                        reply?.readException()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    true
                })
            else
                ctx.runOnUiThread { toast(R.string.toast_root_access_failed) }
        }

    }
}