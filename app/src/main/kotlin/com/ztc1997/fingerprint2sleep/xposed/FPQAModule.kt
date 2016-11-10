package com.ztc1997.fingerprint2sleep.xposed

import com.ztc1997.fingerprint2sleep.BuildConfig
import com.ztc1997.fingerprint2sleep.xposed.extention.tryAndPrintStackTrace
import com.ztc1997.fingerprint2sleep.xposed.hook.AuthenticationCallbackHooks
import com.ztc1997.fingerprint2sleep.xposed.hook.FingerprintServiceHooks
import com.ztc1997.fingerprint2sleep.xposed.hook.SystemUIHooks
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage

class FPQAModule : IXposedHookLoadPackage {
    companion object {
        val TAG: String = FPQAModule::class.java.simpleName
        val ACTION_START_SCANNING = FPQAModule::class.java.name + ".ACTION_START_SCANNING"

        fun log(log: Any?) {
            if (BuildConfig.DEBUG) XposedBridge.log("/$TAG: $log")
        }
    }

    override fun handleLoadPackage(lpp: XC_LoadPackage.LoadPackageParam?) {
        if (lpp == null) return

        if (lpp.packageName == "android" && lpp.processName == "android") {
            tryAndPrintStackTrace { FingerprintServiceHooks.doHook(lpp.classLoader) }
        } else if (lpp.packageName == "com.android.systemui" && lpp.processName == "com.android.systemui") {
            tryAndPrintStackTrace { SystemUIHooks.doHook(lpp.classLoader) }
        }

        tryAndPrintStackTrace { AuthenticationCallbackHooks.doHook(lpp.classLoader) }
    }
}