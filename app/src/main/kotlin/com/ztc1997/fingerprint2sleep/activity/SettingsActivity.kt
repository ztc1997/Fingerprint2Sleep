package com.ztc1997.fingerprint2sleep.activity

import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.os.Bundle
import android.os.IBinder
import android.preference.*
import com.ceco.marshmallow.gravitybox.preference.AppPickerPreference
import com.ceco.marshmallow.gravitybox.preference.AppPickerPreference.ShortcutHandler
import com.google.android.gms.ads.AdRequest
import com.ztc1997.fingerprint2sleep.R
import com.ztc1997.fingerprint2sleep.aidl.IFPQAService
import com.ztc1997.fingerprint2sleep.defaultDPreference
import com.ztc1997.fingerprint2sleep.service.FPQAService
import com.ztc1997.fingerprint2sleep.util.XposedProbe
import com.ztc1997.fingerprint2sleep.xposed.hook.FingerprintServiceHooks
import kotlinx.android.synthetic.main.activity_settings.*
import org.jetbrains.anko.*
import java.text.Collator
import java.util.*


class SettingsActivity : Activity() {
    companion object {
        private const val REQ_OBTAIN_SHORTCUT = 0

        @Deprecated("No longer use", replaceWith = ReplaceWith("PREF_ENABLE_FINGERPRINT_QUICK_ACTION"))
        const val PREF_ENABLE_FINGERPRINT2SLEEP = "pref_enable_fingerprint2sleep"

        const val PREF_ENABLE_FINGERPRINT_QUICK_ACTION = "pref_enable_fingerprint_quick_action"
        const val PREF_RESPONSE_ENROLLED_FINGERPRINT_ONLY = "pref_response_enrolled_fingerprint_only"
        const val PREF_NOTIFY_ON_ERROR = "pref_notify_on_error"
        const val PREF_FOREGROUND_SERVICE = "pref_foreground_service"
        const val PREF_AUTO_RETRY = "pref_auto_retry"
        const val PREF_BLACK_LIST = "pref_black_list"
        // const val PREF_DONATE = "pref_donate"
        const val PREF_SCREEN_OFF_METHOD = "pref_screen_off_method"
        const val PREF_ACTION_SINGLE_TAP = "pref_quick_action"
        const val PREF_ACTION_FAST_SWIPE = "pref_action_fast_swipe"
        const val PREF_SCREEN_NON_XPOSED_MODE = "pref_screen_non_xposed_mode"
        const val PREF_ACTION_SINGLE_TAP_APP = "pref_action_single_tap_app"
        const val PREF_ACTION_FAST_SWIPE_APP = "pref_action_fast_swipe_app"

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
        const val VALUES_PREF_QUICK_ACTION_LAUNCH_APP = "launch_app"

        const val VALUES_PREF_SCREEN_OFF_METHOD_SHORTEN_TIMEOUT = "shorten_timeout"
        const val VALUES_PREF_SCREEN_OFF_METHOD_DEVICE_ADMIN = "device_admin"
        const val VALUES_PREF_SCREEN_OFF_METHOD_POWER_BUTTON = "power_button"

        val PREF_KEYS_BOOLEAN = setOf(PREF_ENABLE_FINGERPRINT_QUICK_ACTION,
                PREF_RESPONSE_ENROLLED_FINGERPRINT_ONLY, PREF_NOTIFY_ON_ERROR,
                PREF_FOREGROUND_SERVICE, PREF_AUTO_RETRY)
        val PREF_KEYS_STRING = setOf(PREF_ACTION_SINGLE_TAP, PREF_ACTION_FAST_SWIPE,
                PREF_SCREEN_OFF_METHOD, PREF_ACTION_SINGLE_TAP_APP,
                PREF_ACTION_FAST_SWIPE_APP)
        val PREF_KEYS_STRING_SET = setOf(PREF_BLACK_LIST)

        val DELAY_RESTART_ACTIONS = setOf(VALUES_PREF_QUICK_ACTION_BACK,
                VALUES_PREF_QUICK_ACTION_HOME, VALUES_PREF_QUICK_ACTION_POWER_DIALOG,
                VALUES_PREF_QUICK_ACTION_TOGGLE_SPLIT_SCREEN, VALUES_PREF_QUICK_ACTION_LAUNCH_APP)

        val DONT_RESTART_ACTIONS = setOf(VALUES_PREF_QUICK_ACTION_RECENTS,
                VALUES_PREF_QUICK_ACTION_SLEEP)
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
            alert(R.string.msg_dialog_device_does_not_support_fingerprint) {
                positiveButton(android.R.string.ok) { finish() }
                onCancel { finish() }
                show()
            }
        }

        if (XposedProbe.isModuleActivated() && !XposedProbe.isModuleVersionMatched())
            toast(R.string.toast_xposed_version_mismatched)

        val adRequest = AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .build()
        adView.loadAd(adRequest)
    }

    override fun onResume() {
        super.onResume()
        bindService(Intent(this, FPQAService::class.java), conn, BIND_AUTO_CREATE)

        // if (billingProcessor.isPurchased(IAP_SKU_DONATE))
        //     adView.visibility = View.GONE
    }

    override fun onPause() {
        super.onPause()
        unbindService(conn)
    }

    class SettingsFragment : PreferenceFragment(), SharedPreferences.OnSharedPreferenceChangeListener {
        val activity by lazy { act as SettingsActivity }

        // val donate: Preference by lazy { findPreference(PREF_DONATE) }
        val FPQASwitch by lazy { findPreference(PREF_ENABLE_FINGERPRINT_QUICK_ACTION) as CheckBoxPreference }
        val nonXposedScreen by lazy { findPreference(PREF_SCREEN_NON_XPOSED_MODE) as PreferenceScreen }
        val actionSingleTap by lazy { findPreference(PREF_ACTION_SINGLE_TAP) as ListPreference }
        val actionSingleTapApp by lazy { findPreference(PREF_ACTION_SINGLE_TAP_APP) as AppPickerPreference }
        val actionFastSwipe by lazy { findPreference(PREF_ACTION_FAST_SWIPE) as ListPreference }
        val actionFastSwipeApp by lazy { findPreference(PREF_ACTION_FAST_SWIPE_APP) as AppPickerPreference }
        val screenOffMethod by lazy { findPreference(PREF_SCREEN_OFF_METHOD) as ListPreference }
        val blacklist by lazy { findPreference(PREF_BLACK_LIST) as MultiSelectListPreference }

        private val loadAppsTask by lazy { LoadAppsTask() }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(com.ztc1997.fingerprint2sleep.R.xml.pref_settings)

            AppPickerPreference.settingsFragment = this

            val moduleActivated = XposedProbe.isModuleActivated()

            FPQASwitch.summary = getString(if (moduleActivated)
                R.string.summary_pref_enable_fingerprint_quick_action_xposed else
                R.string.summary_pref_enable_fingerprint_quick_action_non_xposed)

            if (moduleActivated) nonXposedScreen.isEnabled = false

            loadAppsTask.execute()
        }

        override fun onDestroy() {
            super.onDestroy()
            loadAppsTask.cancel(true)
        }

        override fun onResume() {
            super.onResume()
            defaultSharedPreferences.registerOnSharedPreferenceChangeListener(this)

            actionSingleTap.summary = actionSingleTap.entry
            actionSingleTapApp.isEnabled = defaultSharedPreferences.getString(PREF_ACTION_SINGLE_TAP,
                    VALUES_PREF_QUICK_ACTION_NONE) == VALUES_PREF_QUICK_ACTION_LAUNCH_APP
            actionFastSwipe.summary = actionFastSwipe.entry
            actionFastSwipeApp.isEnabled = defaultSharedPreferences.getString(PREF_ACTION_FAST_SWIPE,
                    VALUES_PREF_QUICK_ACTION_NONE) == VALUES_PREF_QUICK_ACTION_LAUNCH_APP
            screenOffMethod.summary = screenOffMethod.entry
        }

        override fun onPause() {
            super.onPause()
            defaultSharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
            when (key) {
                PREF_ACTION_SINGLE_TAP -> {
                    actionSingleTap.summary = actionSingleTap.entry
                    actionSingleTapApp.isEnabled = sharedPreferences.getString(key,
                            VALUES_PREF_QUICK_ACTION_NONE) == VALUES_PREF_QUICK_ACTION_LAUNCH_APP
                }
                PREF_ACTION_FAST_SWIPE -> {
                    actionFastSwipe.summary = actionFastSwipe.entry
                    actionFastSwipeApp.isEnabled = sharedPreferences.getString(key,
                            VALUES_PREF_QUICK_ACTION_NONE) == VALUES_PREF_QUICK_ACTION_LAUNCH_APP
                }
                PREF_SCREEN_OFF_METHOD -> screenOffMethod.summary = screenOffMethod.entry
            }

            when (key) {
                in PREF_KEYS_BOOLEAN ->
                    activity.defaultDPreference.setPrefBoolean(key, sharedPreferences.getBoolean(key, false))

                in PREF_KEYS_STRING ->
                    activity.defaultDPreference.setPrefString(key, sharedPreferences.getString(key, ""))

                in PREF_KEYS_STRING_SET ->
                    activity.defaultDPreference.setPrefStringSet(key, sharedPreferences.getStringSet(key, emptySet()))
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

        var shortcutHandler: ShortcutHandler? = null
        fun obtainShortcut(handler: ShortcutHandler?) {
            if (handler == null) return

            shortcutHandler = handler
            startActivityForResult(shortcutHandler!!.createShortcutIntent, REQ_OBTAIN_SHORTCUT)
        }

        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            super.onActivityResult(requestCode, resultCode, data)
            if (requestCode == REQ_OBTAIN_SHORTCUT && shortcutHandler != null) {
                if (resultCode == Activity.RESULT_OK) {
                    var localIconResName: String? = null
                    var b: Bitmap? = null
                    val siRes = data!!.getParcelableExtra<Intent.ShortcutIconResource>(Intent.EXTRA_SHORTCUT_ICON_RESOURCE)
                    val shortcutIntent = data.getParcelableExtra<Intent>(Intent.EXTRA_SHORTCUT_INTENT)
                    if (siRes != null) {
                        if (shortcutIntent != null &&
                                AppPickerPreference.ACTION_LAUNCH_ACTION == shortcutIntent.action) {
                            localIconResName = siRes.resourceName
                        } else {
                            try {
                                val extContext = activity.createPackageContext(
                                        siRes.packageName, Context.CONTEXT_IGNORE_SECURITY)
                                val extRes = extContext.resources
                                val drawableResId = extRes.getIdentifier(siRes.resourceName, "drawable", siRes.packageName)
                                b = BitmapFactory.decodeResource(extRes, drawableResId)
                            } catch (e: PackageManager.NameNotFoundException) {
                                //
                            }
                        }
                    }
                    if (localIconResName == null && b == null) {
                        b = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON)
                    }

                    shortcutHandler?.onHandleShortcut(shortcutIntent,
                            data.getStringExtra(Intent.EXTRA_SHORTCUT_NAME),
                            localIconResName, b)
                } else {
                    shortcutHandler?.onShortcutCancelled()
                }
            }
        }

        private inner class LoadAppsTask : AsyncTask<Unit, Unit, Unit>() {
            private val appNames = ArrayList<CharSequence>()
            private val packageNames = ArrayList<CharSequence>()

            override fun onPreExecute() {
                blacklist.isEnabled = false
            }

            override fun doInBackground(vararg args: Unit) {
                val packages = context.packageManager
                        .getInstalledApplications(PackageManager.GET_META_DATA)

                val sortedApps = packages.mapTo(ArrayList()) {
                    if (isCancelled) return

                    arrayOf(it.packageName, it.loadLabel(context.packageManager)
                            .toString())
                }

                val comparator = object : Comparator<Array<String>> {
                    val collator = Collator.getInstance()
                    override fun compare(o1: Array<String>, o2: Array<String>)
                            = collator.compare(o1[1], o2[1])
                }
                sortedApps.sortWith(comparator)

                for (i in sortedApps.indices) {
                    if (isCancelled) return

                    appNames.add(sortedApps[i][1] + "\n" + "(" + sortedApps[i][0] + ")")
                    packageNames.add(sortedApps[i][0])
                }
            }

            override fun onPostExecute(result: Unit) {
                val appNamesList = appNames
                        .toTypedArray()
                val packageNamesList = packageNames
                        .toTypedArray()

                blacklist.entries = appNamesList
                blacklist.entryValues = packageNamesList
                blacklist.isEnabled = true
            }
        }

    }
}