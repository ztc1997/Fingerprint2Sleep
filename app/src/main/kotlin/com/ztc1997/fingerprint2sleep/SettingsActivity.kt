package com.ztc1997.fingerprint2sleep

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.preference.ListPreference
import android.preference.Preference
import android.preference.PreferenceFragment
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.google.android.gms.ads.AdRequest
import com.jarsilio.android.waveup.Root
import kotlinx.android.synthetic.main.activity_settings.*
import org.jetbrains.anko.*

class SettingsActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {
    companion object {
        const val PREF_ENABLE_FINGERPRINT_QUICK_ACTION = "pref_enable_fingerprint_quick_action"
        const val PREF_ENABLE_FINGERPRINT2ACTION = "pref_enable_fingerprint2action"
        const val PREF_RESPONSE_ENROLLED_FINGERPRINT_ONLY = "pref_response_enrolled_fingerprint_only"
        const val PREF_NOTIFY_ON_ERROR = "pref_notify_on_error"
        const val PREF_DISABLE_ADS = "pref_disable_ads"
        const val PREF_FOREGROUND_SERVICE = "pref_foreground_service"
        const val PREF_DONATE = "pref_donate"
        const val PREF_LOCK_SCREEN_WITH_POWER_BUTTON_AS_ROOT = "pref_lock_screen_with_power_button_as_root"
        const val PREF_QUICK_ACTION = "pref_quick_action"

        const val VALUES_PREF_QUICK_ACTION_SLEEP = "sleep"
        const val VALUES_PREF_QUICK_ACTION_HOME = "home"
        const val VALUES_PREF_QUICK_ACTION_EXPEND_NOTIFICATIONS_PANEL = "expend_notifications_panel"

        const val REQUEST_CODE_DEVICE_ADMIN = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        if (!fingerprintManager.isHardwareDetected) {
            alert(R.string.msg_dialog_device_does_not_support_fingerprint) {
                positiveButton(android.R.string.ok) { finish() }
                onCancel { finish() }
                show()
            }
            return
        }

        if (defaultSharedPreferences.getBoolean(PREF_LOCK_SCREEN_WITH_POWER_BUTTON_AS_ROOT, false))
            checkRootAccess()
        else
            checkDeviceAdmin()

        if (!FP2SService.isRunning && defaultSharedPreferences.getBoolean(PREF_ENABLE_FINGERPRINT2ACTION, false))
            startService<FP2SService>()

        if (defaultSharedPreferences.getBoolean(PREF_DISABLE_ADS, false)) {
            adView.visibility = View.GONE
        } else {
            adView.visibility = View.VISIBLE
            val adRequest = AdRequest.Builder()
                    .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                    .build()
            adView.loadAd(adRequest)
        }
    }

    fun checkDeviceAdmin() {
        val componentName = ComponentName(this, AdminReceiver::class.java)
        if (!devicePolicyManager.isAdminActive(componentName)) {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, getString(R.string.explanation_device_admin))

            startActivityForResult(intent, REQUEST_CODE_DEVICE_ADMIN)
        }
    }

    fun checkRootAccess() {
        doAsync {
            if (!Root.requestSuPermission()) {
                uiThread { toast(R.string.toast_root_access_failed) }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_DEVICE_ADMIN && resultCode != Activity.RESULT_OK) {
            toast(R.string.toast_device_admin_failed)
        }
    }

    override fun onResume() {
        super.onResume()
        defaultSharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        defaultSharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        when (key) {
            PREF_ENABLE_FINGERPRINT2ACTION -> if (sharedPreferences.getBoolean(key, false))
                startService<FP2SService>()

            PREF_DISABLE_ADS -> if (sharedPreferences.getBoolean(PREF_DISABLE_ADS, false)) {
                adView.visibility = View.GONE
            } else {
                adView.visibility = View.VISIBLE
                val adRequest = AdRequest.Builder().build()
                adView.loadAd(adRequest)
            }

            PREF_LOCK_SCREEN_WITH_POWER_BUTTON_AS_ROOT -> if (sharedPreferences.getBoolean(PREF_LOCK_SCREEN_WITH_POWER_BUTTON_AS_ROOT, false))
                checkRootAccess()
            else
                checkDeviceAdmin()
        }
    }

    class SettingsFragment : PreferenceFragment(), SharedPreferences.OnSharedPreferenceChangeListener {
        val donate: Preference by lazy { findPreference(PREF_DONATE) }
        val fingerprintAction by lazy { findPreference(PREF_QUICK_ACTION) as ListPreference }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.pref_settings)

            donate.setOnPreferenceClickListener {
                openUri("https://github.com/ztc1997/Fingerprint2Sleep/blob/master/DONATE.md")
                true
            }

            fingerprintAction.summary = fingerprintAction.entry
        }

        override fun onResume() {
            super.onResume()
            defaultSharedPreferences.registerOnSharedPreferenceChangeListener(this)
        }

        override fun onPause() {
            super.onPause()
            defaultSharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            when (key) {
                PREF_QUICK_ACTION -> fingerprintAction.summary = fingerprintAction.entry
            }
        }

        private fun openUri(uriString: String) {
            val uri = Uri.parse(uriString)
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
    }
}