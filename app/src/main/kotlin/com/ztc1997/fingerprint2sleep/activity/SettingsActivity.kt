package com.ztc1997.fingerprint2sleep.activity

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.preference.ListPreference
import android.preference.Preference
import android.preference.PreferenceFragment
import android.support.v7.app.AppCompatActivity
import com.google.android.gms.ads.AdRequest
import com.ztc1997.fingerprint2sleep.R
import com.ztc1997.fingerprint2sleep.SOURCE_ENC
import com.ztc1997.fingerprint2sleep.aidl.IFPQAService
import com.ztc1997.fingerprint2sleep.defaultDPreference
import com.ztc1997.fingerprint2sleep.extension.alert
import com.ztc1997.fingerprint2sleep.service.FPQAService
import com.ztc1997.fingerprint2sleep.util.RC4
import kotlinx.android.synthetic.main.activity_settings.*
import org.jetbrains.anko.ctx
import org.jetbrains.anko.defaultSharedPreferences
import org.jetbrains.anko.fingerprintManager

class SettingsActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {
    companion object {
        const val PREF_ENABLE_FINGERPRINT2SLEEP = "pref_enable_fingerprint2sleep"

        const val PREF_ENABLE_FINGERPRINT_QUICK_ACTION = "pref_enable_fingerprint_quick_action"
        const val PREF_RESPONSE_ENROLLED_FINGERPRINT_ONLY = "pref_response_enrolled_fingerprint_only"
        const val PREF_NOTIFY_ON_ERROR = "pref_notify_on_error"
        const val PREF_FOREGROUND_SERVICE = "pref_foreground_service"
        const val PREF_DONATE = "pref_donate"
        const val PREF_LOCK_SCREEN_WITH_POWER_BUTTON_AS_ROOT = "pref_lock_screen_with_power_button_as_root"
        const val PREF_ACTION_SINGLE_TAP = "pref_quick_action"
        const val PREF_ACTION_FAILED_TO_ACQUIRE = "pref_action_failed_to_acquire"

        const val VALUES_PREF_QUICK_ACTION_NONE = "none"
        const val VALUES_PREF_QUICK_ACTION_SLEEP = "sleep"
        const val VALUES_PREF_QUICK_ACTION_HOME = "home"
        const val VALUES_PREF_QUICK_ACTION_EXPEND_NOTIFICATIONS_PANEL = "expend_notifications_panel"
        const val VALUES_PREF_QUICK_ACTION_TOGGLE_NOTIFICATIONS_PANEL = "toggle_notifications_panel"

        val PREF_KEYS_BOOLEAN = listOf(PREF_ENABLE_FINGERPRINT_QUICK_ACTION,
                PREF_RESPONSE_ENROLLED_FINGERPRINT_ONLY, PREF_NOTIFY_ON_ERROR,
                PREF_FOREGROUND_SERVICE, PREF_LOCK_SCREEN_WITH_POWER_BUTTON_AS_ROOT)
        val PREF_KEYS_STRING = listOf(PREF_ACTION_SINGLE_TAP, PREF_ACTION_FAILED_TO_ACQUIRE)

        val SOURCE by lazy { RC4.decry_RC4(SOURCE_ENC, PREF_KEYS_BOOLEAN[4]) }
    }

    private var bgService: IFPQAService? = null

    val conn = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val iService = IFPQAService.Stub.asInterface(service)
            bgService = iService

            if (!iService.isRunning && defaultSharedPreferences.getBoolean(PREF_ENABLE_FINGERPRINT_QUICK_ACTION, false))
                StartFPQAActivity.startActivity(ctx)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        if (!fingerprintManager.isHardwareDetected) {
            alert(com.ztc1997.fingerprint2sleep.R.string.msg_dialog_device_does_not_support_fingerprint) {
                positiveButton(android.R.string.ok) { finish() }
                onCancel { finish() }
                show()
            }
            return
        }

        val adRequest = AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .build()
        adView.loadAd(adRequest)
    }

    override fun onResume() {
        super.onResume()
        bindService(Intent(this, FPQAService::class.java), conn, BIND_AUTO_CREATE)
        defaultSharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        unbindService(conn)
        defaultSharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        when (key) {
            in PREF_KEYS_BOOLEAN ->
                defaultDPreference.setPrefBoolean(key, sharedPreferences.getBoolean(key, false))

            in PREF_KEYS_STRING ->
                defaultDPreference.setPrefString(key, sharedPreferences.getString(key, ""))
        }

        try {
            bgService?.onPrefChanged(key)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        when (key) {
            PREF_ENABLE_FINGERPRINT_QUICK_ACTION -> if (sharedPreferences.getBoolean(key, false))
                StartFPQAActivity.startActivity(ctx)
        }
    }

    class SettingsFragment : PreferenceFragment(), SharedPreferences.OnSharedPreferenceChangeListener {
        val donate: Preference by lazy { findPreference(PREF_DONATE) }
        val actionSingleTap by lazy { findPreference(PREF_ACTION_SINGLE_TAP) as ListPreference }
        val actionFailedToAcquire by lazy { findPreference(PREF_ACTION_FAILED_TO_ACQUIRE) as ListPreference }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(com.ztc1997.fingerprint2sleep.R.xml.pref_settings)

            donate.setOnPreferenceClickListener {
                openUri("https://github.com/ztc1997/Fingerprint2Sleep/blob/master/DONATE.md")
                true
            }

            actionSingleTap.summary = actionSingleTap.entry
            refreshSummaryActionFailedToAcquire()
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
                PREF_ACTION_SINGLE_TAP -> actionSingleTap.summary = actionSingleTap.entry
                PREF_ACTION_FAILED_TO_ACQUIRE -> refreshSummaryActionFailedToAcquire()
            }
        }

        fun refreshSummaryActionFailedToAcquire() {
            actionFailedToAcquire.summary = "${getString(R.string.summary_pref_action_failed_to_acquire)}\n${actionFailedToAcquire.entry}"
        }

        private fun openUri(uriString: String) {
            val uri = Uri.parse(uriString)
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
    }
}