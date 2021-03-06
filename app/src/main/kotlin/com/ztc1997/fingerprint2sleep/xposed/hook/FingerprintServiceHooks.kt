package com.ztc1997.fingerprint2sleep.xposed.hook

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraManager
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
import me.dozen.dpreference.DPreference
import org.jetbrains.anko.cameraManager
import org.jetbrains.anko.fingerprintManager
import org.jetbrains.anko.powerManager
import java.util.concurrent.TimeUnit

@SuppressLint("StaticFieldLeak")
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

                        when (intent.action) {
                            Intent.ACTION_USER_PRESENT -> checkAndStartScanning()

                            SettingsActivity.ACTION_PREF_CHANGED -> {

                                val key = intent.getStringExtra("key")
                                if (key != null) {
                                    preference.update(intent)

                                    if (key == SettingsActivity.PREF_ENABLE_FINGERPRINT_QUICK_ACTION)
                                        checkAndStartScanning()
                                }

                            }

                            Intent.ACTION_BOOT_COMPLETED -> {
                                val prefs = DPreference(ctx, BuildConfig.APPLICATION_ID + "_preferences")
                                SettingsActivity.PREF_KEYS_BOOLEAN.forEach {
                                    if (it in preference) return@forEach
                                    preference.setPrefBoolean(it, prefs.getPrefBoolean(it, false))
                                }
                                SettingsActivity.PREF_KEYS_STRING.forEach {
                                    if (it in preference) return@forEach
                                    preference.setPrefString(it, prefs.getPrefString(it, ""))
                                }
                                SettingsActivity.PREF_KEYS_STRING_SET.forEach {
                                    if (it in preference) return@forEach
                                    preference.setPrefStringSet(it, prefs.getPrefStringSet(it, null))
                                }
                            }
                        }
                    }
                }

                val intentFilter = IntentFilter()
                intentFilter.addAction(SettingsActivity.ACTION_PREF_CHANGED)
                intentFilter.addAction(Intent.ACTION_USER_PRESENT)
                if (SettingsActivity.PREF_ENABLE_FINGERPRINT_QUICK_ACTION !in preference.prefs)
                    intentFilter.addAction(Intent.ACTION_BOOT_COMPLETED)

                context.registerReceiver(receiver, intentFilter)

                val torchCallback = object : CameraManager.TorchCallback() {
                    override fun onTorchModeChanged(cameraId: String?, enabled: Boolean) {
                        super.onTorchModeChanged(cameraId, enabled)
                        quickActions.flashState = enabled
                    }
                }
                context.cameraManager.registerTorchCallback(torchCallback, null)

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