package com.ztc1997.fingerprint2sleep.xposed.hook

interface IHooks {
    fun doHook(loader: ClassLoader)
}