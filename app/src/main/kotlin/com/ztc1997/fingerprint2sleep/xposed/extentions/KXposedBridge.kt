package com.ztc1997.fingerprint2sleep.xposed.extentions

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge

object KXposedBridge {
    fun hookAllConstructors(clazz: Class<*>, callback: _XC_MethodHook.() -> Unit): MutableSet<XC_MethodHook.Unhook>?
            = XposedBridge.hookAllConstructors(clazz, methodHookCallback(callback))
}