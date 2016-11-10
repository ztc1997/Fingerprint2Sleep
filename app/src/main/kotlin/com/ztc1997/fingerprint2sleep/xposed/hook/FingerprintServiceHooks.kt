package com.ztc1997.fingerprint2sleep.xposed.hook

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.fingerprint.FingerprintManager
import com.eightbitlab.rxbus.Bus
import com.ztc1997.fingerprint2sleep.BuildConfig
import com.ztc1997.fingerprint2sleep.activity.SettingsActivity
import com.ztc1997.fingerprint2sleep.extra.StartScanningEvent
import com.ztc1997.fingerprint2sleep.quickactions.IQuickActions
import com.ztc1997.fingerprint2sleep.quickactions.XposedQuickActions
import com.ztc1997.fingerprint2sleep.xposed.FPQAModule
import com.ztc1997.fingerprint2sleep.xposed.extention.KXposedBridge
import de.robv.android.xposed.XposedHelpers
import me.dozen.dpreference.DPreference
import org.jetbrains.anko.async
import org.jetbrains.anko.fingerprintManager
import java.util.concurrent.TimeUnit

object FingerprintServiceHooks : IHooks {
    object MyAuthenticationCallback : FingerprintManager.AuthenticationCallback() {
        /*override fun onAuthenticationSucceeded(result: FingerprintManager.AuthenticationResult?) {
            super.onAuthenticationSucceeded(result)
            isScanning = false
            performSingleTapAction()
        }

        override fun onAuthenticationFailed() {
            super.onAuthenticationFailed()
            if (!dPreference.getPrefBoolean(SettingsActivity.PREF_RESPONSE_ENROLLED_FINGERPRINT_ONLY, false))
                performSingleTapAction()
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
            super.onAuthenticationError(errorCode, errString)
            if (dPreference.getPrefBoolean(SettingsActivity.PREF_NOTIFY_ON_ERROR, false))
                errString?.let { toast(getString(R.string.toast_notify_on_error, it)) }
            isError = true
            isScanning = false
        }

        override fun onAuthenticationHelp(helpCode: Int, helpString: CharSequence?) {
            super.onAuthenticationHelp(helpCode, helpString)

            Logger.d("helpCode = $helpCode, helpString = $helpString")

            // if (helpCode == FingerprintManager.FINGERPRINT_ACQUIRED_TOO_FAST)
            performFastSwipeAction()
        }*/
        override fun onAuthenticationSucceeded(result: FingerprintManager.AuthenticationResult?) {
            super.onAuthenticationSucceeded(result)
            FPQAModule.log("onAuthenticationSucceeded($result)")
            quickActions.performQuickAction(dPreference.getPrefString(SettingsActivity.PREF_ACTION_SINGLE_TAP,
                    SettingsActivity.VALUES_PREF_QUICK_ACTION_NONE))
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
            super.onAuthenticationError(errorCode, errString)
            FPQAModule.log("onAuthenticationError($errString)")
        }

        override fun onAuthenticationFailed() {
            super.onAuthenticationFailed()
            FPQAModule.log("onAuthenticationFailed()")
        }

        override fun onAuthenticationHelp(helpCode: Int, helpString: CharSequence?) {
            super.onAuthenticationHelp(helpCode, helpString)
            FPQAModule.log("onAuthenticationFailed()")
            quickActions.performQuickAction(dPreference.getPrefString(SettingsActivity.PREF_ACTION_FAST_SWIPE,
                    SettingsActivity.VALUES_PREF_QUICK_ACTION_NONE))
        }
    }

    private var CLASS_FINGERPRINT_SERVICE: Class<*>? = null
    private lateinit var context: Context
    private lateinit var quickActions: IQuickActions
    private lateinit var dPreference: DPreference

    override fun doHook(loader: ClassLoader) {
        CLASS_FINGERPRINT_SERVICE = XposedHelpers.findClass(
                "com.android.server.fingerprint.FingerprintService", loader)

        KXposedBridge.hookAllConstructors(CLASS_FINGERPRINT_SERVICE!!) {
            afterHookedMethod {
                val fingerprintService = it.thisObject

                context = XposedHelpers.getObjectField(fingerprintService, "mContext") as Context

                dPreference = DPreference(context, BuildConfig.APPLICATION_ID + "_preferences")

                quickActions = XposedQuickActions(context, loader)

                Bus.observe<StartScanningEvent>()
                        .throttleLast(100, TimeUnit.MILLISECONDS)
                        .subscribe { authenticate(context, fingerprintService) }

                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(ctx: Context, intent: Intent?) {
                        Bus.send(StartScanningEvent)
                    }
                }

                context.registerReceiver(receiver, IntentFilter(FPQAModule.ACTION_START_SCANNING))

                FPQAModule.log("CLASS_FINGERPRINT_SERVICE Constructor")
            }
        }

        FPQAModule.log("fingerprintServiceHooks")
    }

    fun authenticate(context: Context, fingerprintService: Any) {
        context.async() {
            Thread.sleep(100)
            if (!hasClientMonitor(fingerprintService))
                context.fingerprintManager.authenticate(null, null, 0, MyAuthenticationCallback, null)
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