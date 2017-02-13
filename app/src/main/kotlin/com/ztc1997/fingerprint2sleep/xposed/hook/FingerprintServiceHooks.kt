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
import com.ztc1997.fingerprint2sleep.xposed.extention.tryAndPrintStackTrace
import com.ztc1997.fingerprint2sleep.xposed.impl.PreferenceImpl
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedHelpers
import org.jetbrains.anko.fingerprintManager
import org.jetbrains.anko.powerManager
import java.util.concurrent.TimeUnit

object FingerprintServiceHooks : IHooks {

    class Callback(quickActions: IQuickActions) : GestureAuthenticationCallback(quickActions) {
        override fun restartScanning(action: String?) {
        }
    }

    private var CLASS_FINGERPRINT_SERVICE: Class<*>? = null
    private var cancellationSignal: CancellationSignal? = null
    private lateinit var context: Context
    private lateinit var quickActions: IQuickActions
    private lateinit var preference: PreferenceImpl
    private val callback by lazy { Callback(quickActions) }

    private var forceAccessOnce = false
    private var currPackageName = ""

    override fun doHook(loader: ClassLoader) {
        CLASS_FINGERPRINT_SERVICE = XposedHelpers.findClass(
                "com.android.server.fingerprint.FingerprintService", loader)

        KXposedBridge.hookAllConstructors(CLASS_FINGERPRINT_SERVICE!!) {
            afterHookedMethod {
                val fingerprintService = it.thisObject

                context = XposedHelpers.getObjectField(fingerprintService, "mContext") as Context

                preference = PreferenceImpl(XSharedPreferences(BuildConfig.APPLICATION_ID).apply {
                    makeWorldReadable()
                    reload()
                })

                quickActions = XposedQuickActions(context, preference, loader)

                Bus.observe<StartScanningEvent>()
                        .throttleLast(100, TimeUnit.MILLISECONDS)
                        .subscribe { startScanning(context, fingerprintService) }

                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(ctx: Context, intent: Intent?) {
                        if (intent == null) return

                        if (intent.action == Intent.ACTION_USER_PRESENT) {
                            checkAndStartScanning()
                        } else if (intent.action == SettingsActivity.ACTION_PREF_CHANGED) {

                            val key = intent.getStringExtra("key")
                            if (key != null) {
                                preference.update(intent)

                                if (key == SettingsActivity.PREF_ENABLE_FINGERPRINT_QUICK_ACTION)
                                    checkAndStartScanning()

                            }

                        }
                    }
                }

                val intentFilter = IntentFilter()
                intentFilter.addAction(SettingsActivity.ACTION_PREF_CHANGED)
                intentFilter.addAction(Intent.ACTION_USER_PRESENT)

                context.registerReceiver(receiver, intentFilter)

                FPQAModule.log("CLASS_FINGERPRINT_SERVICE Constructor")
            }
        }

        tryAndPrintStackTrace {
            KXposedBridge.hookAllMethods(CLASS_FINGERPRINT_SERVICE!!, "canUseFingerprint") {
                beforeHookedMethod {
                    if (forceAccessOnce && it.args[0] == "android")
                        it.result = true
                }
            }
        }

        tryAndPrintStackTrace {
            KXposedBridge.hookAllMethods("com.android.server.fingerprint.FingerprintService${'$'}ClientMonitor",
                    loader, "destroy") {
                afterHookedMethod { checkAndStartScanning() }
            }
        }

        FPQAModule.log("fingerprintServiceHooks")
    }

    fun checkAndStartScanning() {
        if (preference.getPrefBoolean(SettingsActivity.PREF_ENABLE_FINGERPRINT_QUICK_ACTION, false) and
                !preference.getPrefBoolean(SettingsActivity.PREF_FORCE_NON_XPOSED_MODE, false))
            Bus.send(StartScanningEvent)
        else if (!(cancellationSignal?.isCanceled ?: true))
            cancellationSignal?.cancel()
    }

    fun startScanning(context: Context, fingerprintService: Any) {
        FPQAModule.log("startScanning invoke")

        if (!context.powerManager.isInteractive) return
        val blacklist = preference.getPrefStringSet(SettingsActivity.PREF_BLACK_LIST, null)
        if (blacklist != null && currPackageName in blacklist) return

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

    fun onActivityChanged(packageName: String) {
        currPackageName = packageName
        val blacklist = preference.getPrefStringSet(SettingsActivity.PREF_BLACK_LIST, null) ?: return
        if (packageName in blacklist) {
            if (!(cancellationSignal?.isCanceled ?: true))
                cancellationSignal?.cancel()
        } else if (cancellationSignal?.isCanceled ?: true) {
            Bus.send(StartScanningEvent)
        }

    }
}