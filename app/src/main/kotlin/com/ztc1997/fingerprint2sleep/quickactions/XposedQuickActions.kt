package com.ztc1997.fingerprint2sleep.quickactions

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.view.KeyEvent
import com.ztc1997.fingerprint2sleep.xposed.extention.tryAndPrintStackTrace
import com.ztc1997.fingerprint2sleep.xposed.hook.SystemUIHooks
import de.robv.android.xposed.XposedHelpers
import me.dozen.dpreference.DPreference
import org.jetbrains.anko.inputManager
import org.jetbrains.anko.powerManager

class XposedQuickActions(override val ctx: Context, override val dPreference: DPreference?, val loader: ClassLoader) : IQuickActions {
    val statusBar: Any? by lazy { ctx.getSystemService("statusbar") }
    val LOCAL_SERVICES_CLASS: Class<*> by lazy { XposedHelpers.findClass("com.android.server.LocalServices", loader) }
    val windowManagerService: Any? by lazy {
        val WINDOW_MANAGER_INTERNAL_CLASS = XposedHelpers.findClass("android.view.WindowManagerInternal", loader)
        XposedHelpers.callStaticMethod(LOCAL_SERVICES_CLASS, "getService", WINDOW_MANAGER_INTERNAL_CLASS)
    }
    val statusBarService: Any? by lazy {
        val STATUS_BAR_MANAGER_INTERNAL_CLASS = XposedHelpers.findClass("com.android.server.statusbar.StatusBarManagerInternal", loader)
        XposedHelpers.callStaticMethod(LOCAL_SERVICES_CLASS, "getService", STATUS_BAR_MANAGER_INTERNAL_CLASS)
    }

    override fun collapsePanels() {
        tryAndPrintStackTrace { XposedHelpers.callMethod(statusBar, "collapsePanels") }
    }

    override fun expandNotificationsPanel() {
        tryAndPrintStackTrace { XposedHelpers.callMethod(statusBar, "expandNotificationsPanel") }
    }

    override fun toggleNotificationsPanel() {
        tryAndPrintStackTrace { ctx.sendBroadcast(Intent(SystemUIHooks.ACTION_TOGGLE_NOTIFICATION_PANEL)) }
    }

    override fun actionHome() {
        tryAndPrintStackTrace { injectKey(ctx, KeyEvent.KEYCODE_HOME) }
    }

    override fun actionBack() {
        tryAndPrintStackTrace { injectKey(ctx, KeyEvent.KEYCODE_BACK) }
    }

    override fun actionRecents() {
        tryAndPrintStackTrace { injectKey(ctx, KeyEvent.KEYCODE_APP_SWITCH) }
    }

    override fun actionPowerDialog() {
        tryAndPrintStackTrace { XposedHelpers.callMethod(windowManagerService, "showGlobalActions") }
    }

    override fun actionToggleSplitScreen() {
        tryAndPrintStackTrace { XposedHelpers.callMethod(statusBarService, "toggleSplitScreen") }
    }

    override fun actionQuickSettings() {
        tryAndPrintStackTrace { XposedHelpers.callMethod(statusBar, "expandSettingsPanel") }
    }

    override fun actionTakeScreenshot() {
        // TODO: Implement take screenshot for Xposed mode
    }

    override fun goToSleep() {
        val pm = ctx.powerManager
        tryAndPrintStackTrace { XposedHelpers.callMethod(pm, "goToSleep", SystemClock.uptimeMillis()) }
    }

    fun injectKey(ctx: Context, keyCode: Int) {
        val eventTime = SystemClock.uptimeMillis()
        val inputManager = ctx.inputManager
        XposedHelpers.callMethod(inputManager, "injectInputEvent",
                KeyEvent(eventTime - 50, eventTime - 50, KeyEvent.ACTION_DOWN,
                        keyCode, 0), 0)
        XposedHelpers.callMethod(inputManager, "injectInputEvent",
                KeyEvent(eventTime - 50, eventTime - 25, KeyEvent.ACTION_UP,
                        keyCode, 0), 0)
    }
}