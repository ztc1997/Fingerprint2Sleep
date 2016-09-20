package com.ztc1997.fingerprint2sleep

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.preference.Preference
import android.preference.PreferenceFragment
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.google.android.gms.ads.AdRequest
import kotlinx.android.synthetic.main.activity_settings.*
import org.jetbrains.anko.defaultSharedPreferences
import org.jetbrains.anko.devicePolicyManager
import org.jetbrains.anko.fingerprintManager
import org.jetbrains.anko.startService

class SettingsActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {
    companion object {
        const val PREF_ENABLE_FINGERPRINT2SLEEP = "pref_enable_fingerprint2sleep"
        const val PREF_RESPONSE_ENROLLED_FINGERPRINT_ONLY = "pref_response_enrolled_fingerprint_only"
        const val PREF_NOTIFY_ON_ERROR = "pref_notify_on_error"
        const val PREF_DISABLE_ADS = "pref_disable_ads"
        const val PREF_FOREGROUND_SERVICE = "pref_foreground_service"
        const val PREF_DONATE = "pref_donate"
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

        checkDeviceAdmin()

        if (!FP2SService.isRunning && defaultSharedPreferences.getBoolean(PREF_ENABLE_FINGERPRINT2SLEEP, false))
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_DEVICE_ADMIN && resultCode != Activity.RESULT_OK) {
            alert(R.string.msg_dialog_device_admin_failed) {
                positiveButton(android.R.string.ok) { finish() }
                onCancel { finish() }
                show()
            }
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
            PREF_ENABLE_FINGERPRINT2SLEEP -> if (sharedPreferences.getBoolean(key, false))
                startService<FP2SService>()

            PREF_DISABLE_ADS -> if (defaultSharedPreferences.getBoolean(PREF_DISABLE_ADS, false)) {
                adView.visibility = View.GONE
            } else {
                adView.visibility = View.VISIBLE
                val adRequest = AdRequest.Builder().build()
                adView.loadAd(adRequest)
            }
        }
    }

    class SettingsFragment : PreferenceFragment() {
        val donate: Preference by lazy { findPreference(PREF_DONATE) }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.pref_settings)

            donate.setOnPreferenceClickListener {
                openUri("https://github.com/ztc1997/Fingerprint2Sleep/blob/master/DONATE.md")
                true
            }
        }

        private fun openUri(uriString: String) {
            val uri = Uri.parse(uriString)
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
    }
}