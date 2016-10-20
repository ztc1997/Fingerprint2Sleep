package com.ztc1997.fingerprint2sleep.extension

import android.app.Activity

fun Activity.finishWithoutAnim() {
    finish()
    overridePendingTransition(0, 0)
}