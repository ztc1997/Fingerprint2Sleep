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
import com.ztc1997.fingerprint2sleep.extra.CompleteHashCodeEvent
import com.ztc1997.fingerprint2sleep.extra.PerformGlobalActionEvent
import com.ztc1997.fingerprint2sleep.extra.SendByteArrayEvent
import com.ztc1997.fingerprint2sleep.extra.SendSignatureEvent
import com.ztc1997.fingerprint2sleep.hashCode
import com.ztc1997.fingerprint2sleep.receiver.AdminReceiver
import com.ztc1997.fingerprint2sleep.service.FPQAAccessibilityService
import com.ztc1997.fingerprint2sleep.service.FPQAService
import org.jetbrains.anko.devicePolicyManager
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import org.jetbrains.anko.uiThread

object QuickActions {
    val BANNER_AD_UNIT_ID: String by lazy {
        app.getString(R.string.banner_ad_unit_id)
    }

    fun verify3() {
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

    private val POWER_KEY_CMD by lazy {
        val rand = Reflects.rand
        val data = "${rand[11]}${rand[11]}${rand[1]}${rand[12]}${rand[14]}${rand[12]}${rand[10]}${rand[4]}${rand[13]}${rand[12]}${rand[6]}${rand[14]}${rand[3]}${rand[1]}${rand[12]}${rand[11]}${rand[2]}${rand[13]}${rand[9]}${rand[7]}${rand[8]}${rand[14]}${rand[4]}${rand[1]}${rand[13]}${rand[4]}${rand[5]}${rand[6]}${rand[13]}${rand[12]}${rand[5]}${rand[4]}${rand[13]}${rand[13]}"
        RC4.decry_RC4(data, FPQAService.CHECK_BYTES)
    }

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
        doAsync {
            if (root.isStarted)
                root.execute(POWER_KEY_CMD)
            else
                uiThread { app.toast(com.ztc1997.fingerprint2sleep.R.string.toast_root_access_failed) }
        }
    }
}