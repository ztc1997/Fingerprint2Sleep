package com.ztc1997.fingerprint2sleep.xposed.hook

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.CancellationSignal
import com.eightbitlab.rxbus.Bus
import com.ztc1997.fingerprint2sleep.BuildConfig
import com.ztc1997.fingerprint2sleep.activity.SettingsActivity
import com.ztc1997.fingerprint2sleep.extra.GestureAuthenticationCallback
import com.ztc1997.fingerprint2sleep.extra.StartScanningEvent
import com.ztc1997.fingerprint2sleep.quickactions.IQuickActions
import com.ztc1997.fingerprint2sleep.quickactions.XposedQuickActions
import com.ztc1997.fingerprint2sleep.xposed.FPQAModule
import com.ztc1997.fingerprint2sleep.xposed.extention.KXposedBridge
import com.ztc1997.fingerprint2sleep.xposed.extention.KXposedHelpers
import com.ztc1997.fingerprint2sleep.xposed.extention.tryAndPrintStackTrace
import de.robv.android.xposed.XposedHelpers
import me.dozen.dpreference.DPreference
import org.jetbrains.anko.fingerprintManager
import java.util.concurrent.TimeUnit

object FingerprintServiceHooks : IHooks {
    val ACTION_START_SCANNING = FingerprintServiceHooks::class.java.name + ".ACTION_START_SCANNING"
    val ACTION_ENABLED_STATE_CHANGED = FingerprintServiceHooks::class.java.name + ".ACTION_ENABLED_STATE_CHANGED"
    val ACTION_DOUBLE_TAP_PARAMS_CHANGED = FingerprintServiceHooks::class.java.name + ".ACTION_DOUBLE_TAP_PARAMS_CHANGED"

    class Callback(quickActions: IQuickActions) : GestureAuthenticationCallback(quickActions) {
        override var doubleTapInterval =
                dPreference.getPrefString(SettingsActivity.PREF_DOUBLE_TAP_INTERVAL, "500").toLong()

        override var doubleTapEnabled =
                dPreference.getPrefBoolean(SettingsActivity.PREF_ENABLE_DOUBLE_TAP, false)

        override fun restartScanning(action: String?) {
        }
    }

    private var CLASS_FINGERPRINT_SERVICE: Class<*>? = null
    private var cancellationSignal: CancellationSignal? = null
    private lateinit var context: Context
    private lateinit var quickActions: IQuickActions
    private lateinit var dPreference: DPreference
    private val callback by lazy { Callback(quickActions) }

    private var forceAccessOnce = false

    override fun doHook(loader: ClassLoader) {
        CLASS_FINGERPRINT_SERVICE = XposedHelpers.findClass(
                "com.android.server.fingerprint.FingerprintService", loader)

        KXposedBridge.hookAllConstructors(CLASS_FINGERPRINT_SERVICE!!) {
            afterHookedMethod {
                val fingerprintService = it.thisObject

                context = XposedHelpers.getObjectField(fingerprintService, "mContext") as Context

                dPreference = DPreference(context, BuildConfig.APPLICATION_ID + "_preferences")

                quickActions = XposedQuickActions(context, dPreference, loader)

                Bus.observe<StartScanningEvent>()
                        .throttleLast(100, TimeUnit.MILLISECONDS)
                        .subscribe { startScanning(context, fingerprintService) }

                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(ctx: Context, intent: Intent?) {
                        if (intent?.action in
                                arrayOf(ACTION_START_SCANNING, ACTION_ENABLED_STATE_CHANGED, Intent.ACTION_USER_PRESENT)) {
                            if (dPreference.getPrefBoolean(SettingsActivity.PREF_ENABLE_FINGERPRINT_QUICK_ACTION, false) and
                                    !dPreference.getPrefBoolean(SettingsActivity.PREF_FORCE_NON_XPOSED_MODE, false))
                                Bus.send(StartScanningEvent)
                            else if (!(cancellationSignal?.isCanceled ?: true))
                                cancellationSignal?.cancel()
                        } else if (intent?.action == ACTION_DOUBLE_TAP_PARAMS_CHANGED) {
                            callback.doubleTapEnabled =
                                    dPreference.getPrefBoolean(SettingsActivity.PREF_ENABLE_DOUBLE_TAP, false)
                            callback.doubleTapInterval =
                                    dPreference.getPrefString(SettingsActivity.PREF_DOUBLE_TAP_INTERVAL, "500").toLong()
                        }
                    }
                }

                val intentFilter = IntentFilter()
                intentFilter.addAction(ACTION_ENABLED_STATE_CHANGED)
                intentFilter.addAction(ACTION_START_SCANNING)
                intentFilter.addAction(ACTION_DOUBLE_TAP_PARAMS_CHANGED)
                intentFilter.addAction(Intent.ACTION_USER_PRESENT)

                context.registerReceiver(receiver, intentFilter)

                FPQAModule.log("CLASS_FINGERPRINT_SERVICE Constructor")
            }
        }

        tryAndPrintStackTrace {
            KXposedHelpers.findAndHookMethod(CLASS_FINGERPRINT_SERVICE!!, "canUseFingerprint",
                    String::class.java, Boolean::class.java) {
                beforeHookedMethod {
                    if (forceAccessOnce && it.args[0] == "android")
                        it.result = true
                }
            }
        }

        tryAndPrintStackTrace {
            KXposedHelpers.findAndHookMethod(CLASS_FINGERPRINT_SERVICE!!, "canUseFingerprint",
                    String::class.java) {
                beforeHookedMethod {
                    if (forceAccessOnce && it.args[0] == "android")
                        it.result = true
                }
            }
        }

        FPQAModule.log("fingerprintServiceHooks")
    }

    fun startScanning(context: Context, fingerprintService: Any) {
        FPQAModule.log("startScanning invoke")

        if (!hasClientMonitor(fingerprintService)) {
            cancellationSignal = CancellationSignal()

            forceAccessOnce = true
            context.fingerprintManager.authenticate(null, cancellationSignal, 0, callback, null)
            forceAccessOnce = false

            FPQAModule.log("startScanning")
        }
    }

    fun hasClientMonitor(fingerprintService: Any): Boolean {
        fun hasFieldAndNonNull(obj: Any, name: String): Boolean {
            val field = XposedHelpers.findFieldIfExists(CLASS_FINGERPRINT_SERVICE, name)
            val fieldInstance = field?.get(obj)
            if (fieldInstance != null) return true
            return false
        }

        return arrayOf("mAuthClient", "mEnrollClient", "mRemoveClient", "mCurrentClient", "mPendingClient")
                .any { hasFieldAndNonNull(fingerprintService, it) }
    }
}