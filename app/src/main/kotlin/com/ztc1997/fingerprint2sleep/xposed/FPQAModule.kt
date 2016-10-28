package com.ztc1997.fingerprint2sleep.xposed

import com.ztc1997.fingerprint2sleep.BuildConfig
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
            try {
                fingerprintServiceHooks(lpp.classLoader)
            } catch(e: Exception) {
                e.printStackTrace()
            }
        }

        try {
            authenticationCallbackHooks(lpp.classLoader)
        } catch(e: Exception) {
            e.printStackTrace()
        }
    }
}