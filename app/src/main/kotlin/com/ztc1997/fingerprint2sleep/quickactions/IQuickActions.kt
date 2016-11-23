package com.ztc1997.fingerprint2sleep.quickactions

import android.content.Context
import android.content.Intent
import com.orhanobut.logger.Logger
import com.ztc1997.fingerprint2sleep.activity.SettingsActivity
import me.dozen.dpreference.DPreference


interface IQuickActions {
    enum class ActionType {
        SingleTap,
        FastSwipe,
    }

    val ctx: Context

    val dPreference: DPreference?

    fun collapsePanels()

    fun expandNotificationsPanel()

    fun toggleNotificationsPanel()

    fun actionHome()

    fun actionBack()

    fun actionRecents()

    fun actionPowerDialog()

    fun actionToggleSplitScreen()

    fun actionQuickSettings()

    fun goToSleep()

    fun performQuickAction(action: String, type: ActionType) {
        when (action) {
            SettingsActivity.VALUES_PREF_QUICK_ACTION_SLEEP ->
                goToSleep()

            SettingsActivity.VALUES_PREF_QUICK_ACTION_BACK ->
                actionBack()

            SettingsActivity.VALUES_PREF_QUICK_ACTION_RECENTS ->
                actionRecents()

            SettingsActivity.VALUES_PREF_QUICK_ACTION_HOME ->
                actionHome()

            SettingsActivity.VALUES_PREF_QUICK_ACTION_EXPEND_NOTIFICATIONS_PANEL ->
                expandNotificationsPanel()

            SettingsActivity.VALUES_PREF_QUICK_ACTION_TOGGLE_NOTIFICATIONS_PANEL ->
                toggleNotificationsPanel()

            SettingsActivity.VALUES_PREF_QUICK_ACTION_POWER_DIALOG ->
                actionPowerDialog()

            SettingsActivity.VALUES_PREF_QUICK_ACTION_EXPAND_QUICK_SETTINGS ->
                actionQuickSettings()

            SettingsActivity.VALUES_PREF_QUICK_ACTION_TOGGLE_SPLIT_SCREEN ->
                actionToggleSplitScreen()

            SettingsActivity.VALUES_PREF_QUICK_ACTION_LAUNCH_APP -> launchAppOrShortcut(type)
        }
    }

    private fun launchAppOrShortcut(type: ActionType) {
        dPreference?.let {
            when (type) {
                ActionType.SingleTap -> launchIntentUri(it.getPrefString(SettingsActivity.PREF_ACTION_SINGLE_TAP_APP, ""))
                ActionType.FastSwipe -> launchIntentUri(it.getPrefString(SettingsActivity.PREF_ACTION_FAST_SWIPE_APP, ""))
            }
        }
    }

    private fun launchIntentUri(uri: String) {
        try {
            val intent = Intent.parseUri(uri, 0)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                ctx.startActivity(intent)
            }
        } catch (t: Throwable) {
            Logger.d(t)
            Logger.d(uri)
        }

    }
}