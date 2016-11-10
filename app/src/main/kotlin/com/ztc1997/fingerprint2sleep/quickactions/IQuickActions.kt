package com.ztc1997.fingerprint2sleep.quickactions

import com.ztc1997.fingerprint2sleep.activity.SettingsActivity

interface IQuickActions {
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

    fun performQuickAction(action: String) {
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
        }
    }
}