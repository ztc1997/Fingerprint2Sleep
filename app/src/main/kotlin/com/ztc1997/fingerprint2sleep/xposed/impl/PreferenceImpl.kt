package com.ztc1997.fingerprint2sleep.xposed.impl

import android.content.Intent
import android.content.SharedPreferences
import com.ztc1997.fingerprint2sleep.activity.SettingsActivity
import com.ztc1997.fingerprint2sleep.base.IPreference
import com.ztc1997.fingerprint2sleep.xposed.FPQAModule
import java.util.*

class PreferenceImpl(val prefs: SharedPreferences) : IPreference {
    init {
        FPQAModule.log(prefs.all)
    }

    private val strings: MutableMap<String, String?> = HashMap()
    private val booleans: MutableMap<String, Boolean> = HashMap()
    private val stringSets: MutableMap<String, MutableSet<String>?> = HashMap()

    override fun setPrefString(key: String, value: String?) {
        strings[key] = value
    }

    override fun getPrefString(key: String?, defaultValue: String?): String? {
        if (strings.containsKey(key))
            return strings[key]
        return prefs.getString(key, defaultValue)
    }

    override fun setPrefBoolean(key: String, value: Boolean) {
        booleans[key] = value
    }

    override fun getPrefBoolean(key: String?, defaultValue: Boolean): Boolean {
        return booleans[key] ?: prefs.getBoolean(key, defaultValue)
    }

    override fun setPrefStringSet(key: String, value: MutableSet<String>?) {
        stringSets[key] = value
    }

    override fun getPrefStringSet(key: String?, defaultValue: MutableSet<String>?): MutableSet<String>? {
        if (stringSets.containsKey(key))
            return stringSets[key]
        return prefs.getStringSet(key, defaultValue)
    }

    operator infix fun contains(key: String)
            = (key in strings) or (key in booleans) or (key in stringSets) or (key in prefs)

    fun update(intent: Intent) {
        if (intent.action != SettingsActivity.ACTION_PREF_CHANGED)
            return

        val key = intent.getStringExtra("key")
        when (key) {
            in SettingsActivity.PREF_KEYS_STRING ->
                setPrefString(key, intent.getStringExtra("value"))

            in SettingsActivity.PREF_KEYS_BOOLEAN -> {
                val default = getPrefBoolean(key, false)
                setPrefBoolean(key, intent.getBooleanExtra("value", default))
            }

            in SettingsActivity.PREF_KEYS_STRING_SET ->
                setPrefStringSet(key, intent.getStringArrayExtra("value").toMutableSet())

        }
    }
}