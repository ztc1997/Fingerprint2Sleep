package com.ztc1997.fingerprint2sleep.quickactions

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.view.KeyEvent
import com.ztc1997.fingerprint2sleep.base.IPreference
import com.ztc1997.fingerprint2sleep.xposed.FPQAModule
import com.ztc1997.fingerprint2sleep.xposed.extention.tryAndPrintStackTrace
import com.ztc1997.fingerprint2sleep.xposed.hook.SystemUIHooks
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import org.jetbrains.anko.inputManager
import org.jetbrains.anko.powerManager


@SuppressLint("WrongConstant")
class XposedQuickActions(override val ctx: Context, override val preference: IPreference?, val loader: ClassLoader) : IQuickActions {
    private val statusBar: Any? by lazy { ctx.getSystemService("statusbar") }
    private val LOCAL_SERVICES_CLASS: Class<*> by lazy { XposedHelpers.findClass("com.android.server.LocalServices", loader) }
    private val windowManagerService: Any? by lazy {
        val WINDOW_MANAGER_INTERNAL_CLASS = XposedHelpers.findClass("android.view.WindowManagerInternal", loader)
        XposedHelpers.callStaticMethod(LOCAL_SERVICES_CLASS, "getService", WINDOW_MANAGER_INTERNAL_CLASS)
    }
    private val statusBarService: Any? by lazy {
        val STATUS_BAR_MANAGER_INTERNAL_CLASS = XposedHelpers.findClass("com.android.server.statusbar.StatusBarManagerInternal", loader)
        XposedHelpers.callStaticMethod(LOCAL_SERVICES_CLASS, "getService", STATUS_BAR_MANAGER_INTERNAL_CLASS)
    }
    override var flashState: Boolean = false

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

    private val mScreenshotLock = Object()
    private var mScreenshotConnection: ServiceConnection? = null
    override fun actionTakeScreenshot() {
        val handler = XposedHelpers.getObjectField(FPQAModule.phoneWindowManager,
                "mHandler") as Handler? ?: return
        synchronized(mScreenshotLock) {
            if (mScreenshotConnection != null) {
                return
            }
            val cn = ComponentName("com.android.systemui",
                    "com.android.systemui.screenshot.TakeScreenshotService")
            val intent = Intent()
            intent.component = cn
            val conn = object : ServiceConnection {
                override fun onServiceDisconnected(name: ComponentName?) {
                }

                override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                    synchronized(mScreenshotLock) {
                        if (mScreenshotConnection !== this) {
                            return
                        }
                        val messenger = Messenger(service)
                        val msg = Message.obtain(null, 1)
                        val myConn = this

                        val h = object : Handler(handler.looper) {
                            override fun handleMessage(msg: Message) {
                                synchronized(mScreenshotLock) {
                                    if (mScreenshotConnection === myConn) {
                                        ctx.unbindService(mScreenshotConnection)
                                        mScreenshotConnection = null
                                        handler.removeCallbacks(mScreenshotTimeout)
                                    }
                                }
                            }
                        }
                        msg.replyTo = Messenger(h)
                        msg.arg2 = 0
                        msg.arg1 = msg.arg2
                        h.postDelayed({
                            try {
                                messenger.send(msg)
                            } catch (e: RemoteException) {
                                XposedBridge.log(e)
                            }
                        }, 1000)
                    }
                }
            }

            if (ctx.bindService(intent, conn, Context.BIND_AUTO_CREATE)) {
                mScreenshotConnection = conn
                handler.postDelayed(mScreenshotTimeout, 10000)
            }
        }
    }

    private val mScreenshotTimeout = Runnable {
        synchronized(mScreenshotLock) {
            if (mScreenshotConnection != null) {
                ctx.unbindService(mScreenshotConnection)
                mScreenshotConnection = null
            }
        }
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