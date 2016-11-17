package com.ztc1997.fingerprint2sleep.activity

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.os.Bundle
import android.os.IBinder
import android.preference.CheckBoxPreference
import android.preference.ListPreference
import android.preference.PreferenceFragment
import android.preference.PreferenceScreen
import android.view.View
import com.anjlab.android.iab.v3.BillingProcessor
import com.anjlab.android.iab.v3.TransactionDetails
import com.google.android.gms.ads.AdRequest
import com.ztc1997.fingerprint2sleep.App.Companion.IAP_SKU_DONATE
import com.ztc1997.fingerprint2sleep.App.Companion.LICENSE_KEY
import com.ztc1997.fingerprint2sleep.R
import com.ztc1997.fingerprint2sleep.SOURCE_ENC
import com.ztc1997.fingerprint2sleep.aidl.IFPQAService
import com.ztc1997.fingerprint2sleep.defaultDPreference
import com.ztc1997.fingerprint2sleep.service.FPQAService
import com.ztc1997.fingerprint2sleep.util.RC4
import com.ztc1997.fingerprint2sleep.util.XposedProbe
import com.ztc1997.fingerprint2sleep.xposed.hook.FingerprintServiceHooks
import kotlinx.android.synthetic.main.activity_settings.*
import org.jetbrains.anko.*

class SettingsActivity : Activity(), BillingProcessor.IBillingHandler {
    companion object {
        @Deprecated("No longer use", replaceWith = ReplaceWith("PREF_ENABLE_FINGERPRINT_QUICK_ACTION"))
        const val PREF_ENABLE_FINGERPRINT2SLEEP = "pref_enable_fingerprint2sleep"

        const val PREF_ENABLE_FINGERPRINT_QUICK_ACTION = "pref_enable_fingerprint_quick_action"
        const val PREF_RESPONSE_ENROLLED_FINGERPRINT_ONLY = "pref_response_enrolled_fingerprint_only"
        const val PREF_NOTIFY_ON_ERROR = "pref_notify_on_error"
        const val PREF_FOREGROUND_SERVICE = "pref_foreground_service"
        // const val PREF_DONATE = "pref_donate"
        const val PREF_SCREEN_OFF_METHOD = "pref_screen_off_method"
        const val PREF_ACTION_SINGLE_TAP = "pref_quick_action"
        const val PREF_ACTION_FAST_SWIPE = "pref_action_fast_swipe"
        const val PREF_SCREEN_NON_XPOSED_MODE = "pref_screen_non_xposed_mode"

        const val VALUES_PREF_QUICK_ACTION_NONE = "none"
        const val VALUES_PREF_QUICK_ACTION_SLEEP = "sleep"
        const val VALUES_PREF_QUICK_ACTION_BACK = "back"
        const val VALUES_PREF_QUICK_ACTION_HOME = "home"
        const val VALUES_PREF_QUICK_ACTION_RECENTS = "recents"
        const val VALUES_PREF_QUICK_ACTION_POWER_DIALOG = "power_dialog"
        const val VALUES_PREF_QUICK_ACTION_TOGGLE_SPLIT_SCREEN = "toggle_split_screen"
        const val VALUES_PREF_QUICK_ACTION_EXPEND_NOTIFICATIONS_PANEL = "expend_notifications_panel"
        const val VALUES_PREF_QUICK_ACTION_TOGGLE_NOTIFICATIONS_PANEL = "toggle_notifications_panel"
        const val VALUES_PREF_QUICK_ACTION_EXPAND_QUICK_SETTINGS = "expand_quick_settings"

        const val VALUES_PREF_SCREEN_OFF_METHOD_SHORTEN_TIMEOUT = "shorten_timeout"
        const val VALUES_PREF_SCREEN_OFF_METHOD_DEVICE_ADMIN = "device_admin"
        const val VALUES_PREF_SCREEN_OFF_METHOD_POWER_BUTTON = "power_button"

        val PREF_KEYS_BOOLEAN = listOf(PREF_ENABLE_FINGERPRINT_QUICK_ACTION,
                PREF_RESPONSE_ENROLLED_FINGERPRINT_ONLY, PREF_NOTIFY_ON_ERROR,
                PREF_FOREGROUND_SERVICE, "pref_lock_screen_with_power_button_as_root")
        val PREF_KEYS_STRING = listOf(PREF_ACTION_SINGLE_TAP, PREF_ACTION_FAST_SWIPE, PREF_SCREEN_OFF_METHOD)

        val DELAY_RESTART_ACTIONS = setOf(VALUES_PREF_QUICK_ACTION_BACK,
                VALUES_PREF_QUICK_ACTION_HOME, VALUES_PREF_QUICK_ACTION_POWER_DIALOG,
                VALUES_PREF_QUICK_ACTION_TOGGLE_SPLIT_SCREEN)

        val DONT_RESTART_ACTIONS = setOf(VALUES_PREF_QUICK_ACTION_RECENTS,
                VALUES_PREF_QUICK_ACTION_SLEEP)

        val SOURCE by lazy { RC4.decry_RC4(SOURCE_ENC, PREF_KEYS_BOOLEAN[4]) }
    }

    private var bgService: IFPQAService? = null

    private lateinit var billingProcessor: BillingProcessor

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

        billingProcessor = BillingProcessor(this, LICENSE_KEY, this)

        if (XposedProbe.isModuleActivated() && !XposedProbe.isModuleVersionMatched())
            toast(R.string.toast_xposed_version_mismatched)
    }

    override fun onResume() {
        super.onResume()
        bindService(Intent(this, FPQAService::class.java), conn, BIND_AUTO_CREATE)

        if (billingProcessor.isPurchased(IAP_SKU_DONATE))
            adView.visibility = View.GONE
    }

    override fun onPause() {
        super.onPause()
        unbindService(conn)
    }

    override fun onBillingInitialized() {
        if (!billingProcessor.isPurchased(IAP_SKU_DONATE)) {
            val adRequest = AdRequest.Builder()
                    .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                    .build()
            adView.loadAd(adRequest)
        }
    }

    override fun onProductPurchased(productId: String?, details: TransactionDetails?) {
    }

    override fun onBillingError(errorCode: Int, error: Throwable?) {
        toast(error?.message.orEmpty())
    }

    override fun onPurchaseHistoryRestored() {
    }

    class SettingsFragment : PreferenceFragment(), SharedPreferences.OnSharedPreferenceChangeListener {
        val activity by lazy { act as SettingsActivity }

        // val donate: Preference by lazy { findPreference(PREF_DONATE) }
        val FPQASwitch by lazy { findPreference(PREF_ENABLE_FINGERPRINT_QUICK_ACTION) as CheckBoxPreference }
        val nonXposedScreen by lazy { findPreference(PREF_SCREEN_NON_XPOSED_MODE) as PreferenceScreen }
        val actionSingleTap by lazy { findPreference(PREF_ACTION_SINGLE_TAP) as ListPreference }
        val actionFastSwipe by lazy { findPreference(PREF_ACTION_FAST_SWIPE) as ListPreference }
        val screenOffMethod by lazy { findPreference(PREF_SCREEN_OFF_METHOD) as ListPreference }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(com.ztc1997.fingerprint2sleep.R.xml.pref_settings)

            // donate.setOnPreferenceClickListener {
            //     if (!activity.billingProcessor.isPurchased(IAP_SKU_DONATE))
            //         activity.billingProcessor.purchase(activity, IAP_SKU_DONATE)
            //     true
            // }

            val moduleActivated = XposedProbe.isModuleActivated()

            FPQASwitch.summary = getString(if (moduleActivated)
                R.string.summary_pref_enable_fingerprint_quick_action_xposed else
                R.string.summary_pref_enable_fingerprint_quick_action_non_xposed)

            if (moduleActivated) nonXposedScreen.isEnabled = false
        }

        override fun onResume() {
            super.onResume()
            defaultSharedPreferences.registerOnSharedPreferenceChangeListener(this)

            actionSingleTap.summary = actionSingleTap.entry
            actionFastSwipe.summary = actionFastSwipe.entry
            screenOffMethod.summary = screenOffMethod.entry

            // if (activity.billingProcessor.isPurchased(IAP_SKU_DONATE)) {
            //     donate.title = getString(R.string.title_pref_donate_purchased)
            //     donate.summary = getString(R.string.summary_pref_donate_purchased)
            // }
        }

        override fun onPause() {
            super.onPause()
            defaultSharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
            when (key) {
                PREF_ACTION_SINGLE_TAP -> actionSingleTap.summary = actionSingleTap.entry
                PREF_ACTION_FAST_SWIPE -> actionFastSwipe.summary = actionFastSwipe.entry
                PREF_SCREEN_OFF_METHOD -> screenOffMethod.summary = screenOffMethod.entry
            }

            when (key) {
                in PREF_KEYS_BOOLEAN ->
                    activity.defaultDPreference.setPrefBoolean(key, sharedPreferences.getBoolean(key, false))

                in PREF_KEYS_STRING ->
                    activity.defaultDPreference.setPrefString(key, sharedPreferences.getString(key, ""))
            }

            try {
                activity.bgService?.onPrefChanged(key)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            when (key) {
                PREF_ENABLE_FINGERPRINT_QUICK_ACTION -> {
                    if (XposedProbe.isModuleActivated())
                        activity.sendBroadcast(Intent(FingerprintServiceHooks.ACTION_ENABLED_STATE_CHANGED))
                    else if (sharedPreferences.getBoolean(key, false))
                        StartFPQAActivity.startActivity(ctx)
                }
            }
        }
    }
}