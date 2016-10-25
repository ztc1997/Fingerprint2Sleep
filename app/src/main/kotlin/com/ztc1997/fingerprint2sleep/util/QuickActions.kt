package com.ztc1997.fingerprint2sleep.util

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import com.eightbitlab.rxbus.Bus
import com.ztc1997.fingerprint2sleep.App
import com.ztc1997.fingerprint2sleep.R
import com.ztc1997.fingerprint2sleep.activity.RequireAccessibilityActivity
import com.ztc1997.fingerprint2sleep.activity.RequireAdminActivity
import com.ztc1997.fingerprint2sleep.activity.SettingsActivity
import com.ztc1997.fingerprint2sleep.extension.execute
import com.ztc1997.fingerprint2sleep.extension.root
import com.ztc1997.fingerprint2sleep.extra.CompleteHashCodeEvent
import com.ztc1997.fingerprint2sleep.extra.PerformGlobalActionEvent
import com.ztc1997.fingerprint2sleep.extra.SendByteArrayEvent
import com.ztc1997.fingerprint2sleep.extra.SendSignatureEvent
import com.ztc1997.fingerprint2sleep.hashCode
import com.ztc1997.fingerprint2sleep.receiver.AdminReceiver
import com.ztc1997.fingerprint2sleep.service.FPQAAccessibilityService
import org.jetbrains.anko.async
import org.jetbrains.anko.devicePolicyManager
import org.jetbrains.anko.onUiThread
import org.jetbrains.anko.toast

object QuickActions {
    val BANNER_AD_UNIT_ID: String by lazy {
        app.getString(R.string.banner_ad_unit_id)
    }

    fun verify3() {
        RequireAccessibilityActivity.verify2()

        Bus.observe<SendSignatureEvent>().subscribe {
            val code = it.any.hashCode
            if (code is Int)
                CHECK_CODE = code xor 465168998

            Bus.send(CompleteHashCodeEvent(it.any))
        }

        Bus.observe<SendByteArrayEvent>().subscribe {
            CHECK_BYTES = it.byteArray.filterIndexed { i, byte -> i % (-CHECK_CODE and 156) != 0 }
                    .reversed().toByteArray()
        }
    }

    lateinit var CHECK_BYTES: ByteArray
        private set

    var CHECK_CODE: Int = 0
        private set

    private lateinit var app: App

    private val POWER_KEY_CMD = "input keyevent 26"

    fun inject(app: App) {
        this.app = app
    }

    fun collapsePanels() {
        Reflects.collapsePanels(app)
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
        async() {
            if (root.isStarted)
                root.execute(POWER_KEY_CMD)
            else
                app.onUiThread { app.toast(com.ztc1997.fingerprint2sleep.R.string.toast_root_access_failed) }
        }
    }
}