package com.ztc1997.fingerprint2sleep.xposed.hook

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.ztc1997.fingerprint2sleep.quickactions.IQuickActions
import com.ztc1997.fingerprint2sleep.quickactions.XposedQuickActions
import com.ztc1997.fingerprint2sleep.xposed.FPQAModule
import com.ztc1997.fingerprint2sleep.xposed.extention.KXposedHelpers
import de.robv.android.xposed.XposedHelpers

@SuppressLint("StaticFieldLeak")
object SystemUIHooks : IHooks {
    val ACTION_TOGGLE_NOTIFICATION_PANEL = SystemUIHooks::class.java.name + "ACTION_TOGGLE_NOTIFICATION_PANEL"

    // val CLASS_NOTIFICATION_PANEL_VIEW = "com.android.systemui.statusbar.phone.NotificationPanelView"
    val CLASS_PHONE_STATUS_BAR = "com.android.systemui.statusbar.phone.PhoneStatusBar"

    // lateinit var notificationPanelView: FrameLayout
    lateinit var phoneStatusBar: Any
    private lateinit var context: Context
    private lateinit var quickActions: IQuickActions

    override fun doHook(loader: ClassLoader) {
/*        KXposedBridge.hookAllConstructors(CLASS_NOTIFICATION_PANEL_VIEW, loader) {
            afterHookedMethod {
                notificationPanelView = it.thisObject as FrameLayout
            }
        }*/
        KXposedHelpers.findAndHookMethod(CLASS_PHONE_STATUS_BAR, loader, "makeStatusBarView") {
            afterHookedMethod {
                phoneStatusBar = it.thisObject
                context = XposedHelpers.getObjectField(phoneStatusBar, "mContext") as Context
                quickActions = XposedQuickActions(context, null, loader)
                context.registerReceiver(notificationPanelViewActionReceiver,
                        IntentFilter(ACTION_TOGGLE_NOTIFICATION_PANEL))
            }
        }
    }

    private val notificationPanelViewActionReceiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            val mExpandedVisible = XposedHelpers.getBooleanField(phoneStatusBar, "mExpandedVisible")
            FPQAModule.log("mExpandedVisible = $mExpandedVisible")
            if (mExpandedVisible) quickActions.collapsePanels() else
                quickActions.expandNotificationsPanel()
        }
    }
}
