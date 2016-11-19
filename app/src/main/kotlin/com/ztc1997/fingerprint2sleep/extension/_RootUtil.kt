package com.ztc1997.fingerprint2sleep.extension

import com.ztc1997.fingerprint2sleep.util.RootUtil

val root by lazy { RootUtil() }

fun RootUtil.execute(command: String) = execute(command, null)

