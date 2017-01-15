package com.ztc1997.fingerprint2sleep.xposed.extention

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers

object KXposedHelpers {
    fun findAndHookMethod(clazz: Class<*>, methodName: String, vararg parameterTypes: Any,
                          callback: _XC_MethodHook.() -> Unit): XC_MethodHook.Unhook?
            = XposedHelpers.findAndHookMethod(clazz, methodName, *parameterTypes, methodHookCallback(callback))

    fun findAndHookMethod(className: String, classLoader: ClassLoader, methodName: String,
                          vararg parameterTypes: Any, callback: _XC_MethodHook.() -> Unit): XC_MethodHook.Unhook?
            = XposedHelpers.findAndHookMethod(className, classLoader, methodName, *parameterTypes, methodHookCallback(callback))
}