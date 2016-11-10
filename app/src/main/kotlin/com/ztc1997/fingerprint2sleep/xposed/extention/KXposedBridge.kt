package com.ztc1997.fingerprint2sleep.xposed.extention

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

object KXposedBridge {
    fun hookAllConstructors(clazz: Class<*>, callback: _XC_MethodHook.() -> Unit): MutableSet<XC_MethodHook.Unhook>?
            = XposedBridge.hookAllConstructors(clazz, methodHookCallback(callback))

    fun hookAllConstructors(className: String, classLoader: ClassLoader, callback: _XC_MethodHook.() -> Unit): MutableSet<XC_MethodHook.Unhook>?
            = hookAllConstructors(XposedHelpers.findClass(className, classLoader), callback)
}