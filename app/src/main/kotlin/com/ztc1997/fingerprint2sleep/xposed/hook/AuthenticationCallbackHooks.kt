package com.ztc1997.fingerprint2sleep.xposed.hook

import android.content.Context
import android.content.Intent
import android.hardware.fingerprint.FingerprintManager
import android.os.CancellationSignal
import android.os.Handler
import com.ztc1997.fingerprint2sleep.xposed.FPQAModule
import com.ztc1997.fingerprint2sleep.xposed.extention.KXposedHelpers
import de.robv.android.xposed.XposedHelpers

object AuthenticationCallbackHooks : IHooks {
    override fun doHook(loader: ClassLoader) {
        fun sendBroadcast(obj: Any) {
            val ctx = XposedHelpers.getAdditionalInstanceField(obj, "context") as Context
            ctx.sendBroadcast(Intent(FPQAModule.ACTION_START_SCANNING))
        }

        KXposedHelpers.findAndHookMethod(FingerprintManager::class.java, "authenticate",
                FingerprintManager.CryptoObject::class.java, CancellationSignal::class.java,
                Int::class.java, FingerprintManager.AuthenticationCallback::class.java,
                Handler::class.java) {
            afterHookedMethod {
                XposedHelpers.setAdditionalInstanceField(it.args[3], "context",
                        XposedHelpers.getObjectField(it.thisObject, "mContext"))
            }
        }

        KXposedHelpers.findAndHookMethod(FingerprintManager.AuthenticationCallback::class.java,
                "onAuthenticationSucceeded", FingerprintManager.AuthenticationResult::class.java) {
            afterHookedMethod {
                sendBroadcast(it.thisObject)
            }
        }
        KXposedHelpers.findAndHookMethod(FingerprintManager.AuthenticationCallback::class.java,
                "onAuthenticationError", Int::class.java, CharSequence::class.java) {
            afterHookedMethod {
                if (it.thisObject is FingerprintServiceHooks.MyAuthenticationCallback) return@afterHookedMethod
                sendBroadcast(it.thisObject)
            }
        }
    }
}
