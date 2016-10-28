package com.ztc1997.fingerprint2sleep.xposed.extentions

import de.robv.android.xposed.XC_MethodHook

fun methodHookCallback(init: _XC_MethodHook.() -> Unit) = _XC_MethodHook().apply(init)

class _XC_MethodHook : XC_MethodHook() {
    private var _beforeHookedMethod: ((MethodHookParam) -> Unit)? = null
    private var _afterHookedMethod: ((MethodHookParam) -> Unit)? = null

    fun beforeHookedMethod(listener: (MethodHookParam) -> Unit) {
        _beforeHookedMethod = listener
    }

    fun afterHookedMethod(listener: (MethodHookParam) -> Unit) {
        _afterHookedMethod = listener
    }

    override fun beforeHookedMethod(param: MethodHookParam) {
        super.beforeHookedMethod(param)
        _beforeHookedMethod?.invoke(param)
    }

    override fun afterHookedMethod(param: MethodHookParam) {
        super.afterHookedMethod(param)
        _afterHookedMethod?.invoke(param)
    }
}