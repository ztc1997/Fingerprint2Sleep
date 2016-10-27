package com.ztc1997.fingerprint2sleep.xposed

import android.os.IBinder
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.util.*

class FPQAModule : IXposedHookLoadPackage {
    companion object {
        private val CLASS_FINGERPRINT_SERVICE = "com.android.server.fingerprint.FingerprintService"
        private val TAG = FPQAModule::class.java.simpleName
    }

    override fun handleLoadPackage(lpp: XC_LoadPackage.LoadPackageParam?) {
        if (lpp == null) return

        if (lpp.packageName == "android" && lpp.processName == "android") {
            /*XposedHelpers.findAndHookConstructor(XposedHelpers.findClass(
                    CLASS_FINGERPRINT_SERVICE, lpp.classLoader), object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam?) {
                    super.afterHookedMethod(param)
                }
            })*/

            XposedHelpers.findAndHookMethod(CLASS_FINGERPRINT_SERVICE,
                    lpp.classLoader, "handleAuthenticated", Long::class.java, Int::class.java, Int::class.java,
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: XC_MethodHook.MethodHookParam?) {
                            super.afterHookedMethod(param)
                            XposedBridge.log("CLASS_FINGERPRINT_SERVICE: handleAuthenticated(${Arrays.toString(param?.args)})")
                        }
                    })

            XposedHelpers.findAndHookMethod(CLASS_FINGERPRINT_SERVICE,
                    lpp.classLoader, "cancelAuthentication", IBinder::class.java, String::class.java,
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: XC_MethodHook.MethodHookParam?) {
                            super.afterHookedMethod(param)
                            XposedBridge.log("CLASS_FINGERPRINT_SERVICE: cancelAuthentication(${Arrays.toString(param?.args)})")
                        }
                    })
        }
    }
}