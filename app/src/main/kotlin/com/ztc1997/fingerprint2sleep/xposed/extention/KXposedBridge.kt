package com.ztc1997.fingerprint2sleep.xposed.extention

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

object KXposedBridge {
    fun hookAllConstructors(clazz: Class<*>, callback: _XC_MethodHook.() -> Unit): MutableSet<XC_MethodHook.Unhook>?
            = XposedBridge.hookAllConstructors(clazz, methodHookCallback(callback))

    fun hookAllConstructors(className: String, classLoader: ClassLoader, callback: _XC_MethodHook.() -> Unit): MutableSet<XC_MethodHook.Unhook>?
            = hookAllConstructors(XposedHelpers.findClass(className, classLoader), callback)

    fun hookAllMethods(clazz: Class<*>, methodName: String, callback: _XC_MethodHook.() -> Unit)
            = XposedBridge.hookAllMethods(clazz, methodName, methodHookCallback(callback))

    fun hookAllMethods(className: String, classLoader: ClassLoader, methodName: String, callback: _XC_MethodHook.() -> Unit)
            = XposedBridge.hookAllMethods(XposedHelpers.findClass(className, classLoader), methodName, methodHookCallback(callback))
}