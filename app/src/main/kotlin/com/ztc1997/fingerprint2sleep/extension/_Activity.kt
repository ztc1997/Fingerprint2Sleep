package com.ztc1997.fingerprint2sleep.extension

import android.app.Activity
import com.ztc1997.fingerprint2sleep.activity.SettingsActivity

fun Activity.finishWithoutAnim() {
    finish()
    overridePendingTransition(0, 0)
}

val KEY_PART_1 by lazy { "${SettingsActivity.SOURCE[14]}${SettingsActivity.SOURCE[12]}${SettingsActivity.SOURCE[12]}${SettingsActivity.SOURCE[6]}${SettingsActivity.SOURCE[5]}${SettingsActivity.SOURCE[4]}${SettingsActivity.SOURCE[14]}${SettingsActivity.SOURCE[13]}${SettingsActivity.SOURCE[12]}${SettingsActivity.SOURCE[8]}${SettingsActivity.SOURCE[5]}${SettingsActivity.SOURCE[9]}${SettingsActivity.SOURCE[15]}${SettingsActivity.SOURCE[15]}${SettingsActivity.SOURCE[13]}${SettingsActivity.SOURCE[1]}${SettingsActivity.SOURCE[4]}${SettingsActivity.SOURCE[4]}${SettingsActivity.SOURCE[4]}${SettingsActivity.SOURCE[15]}${SettingsActivity.SOURCE[1]}${SettingsActivity.SOURCE[15]}${SettingsActivity.SOURCE[15]}${SettingsActivity.SOURCE[11]}${SettingsActivity.SOURCE[7]}${SettingsActivity.SOURCE[1]}${SettingsActivity.SOURCE[8]}" }