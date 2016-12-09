package com.ztc1997.fingerprint2sleep.util

import java.net.InetAddress
import java.net.UnknownHostException

val admobHosts = setOf("a.admob.com", "mm.admob.com", "p.admob.com", "r.admob.com",
        "media.admob.com")

val isAdmobHostBanned: Boolean
    get() {
        try {
            if (admobHosts.any { InetAddress.getByName(it).hostAddress == "127.0.0.1" })
                return true
        } catch (e: UnknownHostException) {
        }
        return false
    }
