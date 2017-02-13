package com.ztc1997.fingerprint2sleep.xposed.hook

import android.content.pm.ActivityInfo
import com.ztc1997.fingerprint2sleep.xposed.extention.KXposedBridge
import de.robv.android.xposed.XposedHelpers

object ApplicationThreadProxyHooks : IHooks {
    override fun doHook(loader: ClassLoader) {
        KXposedBridge.hookAllMethods("android.app.ApplicationThreadProxy", loader,
                "scheduleResumeActivity") {
            afterHookedMethod {
                val packageName = XposedHelpers
                        .getAdditionalInstanceField(it.thisObject, "packageName")
                if (packageName is String)
                    FingerprintServiceHooks.onActivityChanged(packageName)
            }
        }
        KXposedBridge.hookAllMethods("android.app.ApplicationThreadProxy", loader,
                "scheduleLaunchActivity") {
            afterHookedMethod {
                val ai = it.args[3]
                if (ai is ActivityInfo) {
                    val packageName = ai.packageName
                    XposedHelpers.setAdditionalInstanceField(it.thisObject, "packageName", packageName)
                    FingerprintServiceHooks.onActivityChanged(packageName)
                }
            }
        }
    }
}