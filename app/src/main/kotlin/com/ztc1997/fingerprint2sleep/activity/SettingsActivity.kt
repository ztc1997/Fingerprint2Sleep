package com.ztc1997.fingerprint2sleep.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.preference.*
import android.view.View
import com.ceco.marshmallow.gravitybox.preference.AppPickerPreference
import com.ceco.marshmallow.gravitybox.preference.AppPickerPreference.ShortcutHandler
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.firebase.analytics.FirebaseAnalytics
import com.orhanobut.logger.Logger
import com.tbruyelle.rxpermissions.RxPermissions
import com.ztc1997.fingerprint2sleep.BuildConfig
import com.ztc1997.fingerprint2sleep.R
import com.ztc1997.fingerprint2sleep.aidl.IFPQAService
import com.ztc1997.fingerprint2sleep.app
import com.ztc1997.fingerprint2sleep.defaultDPreference
import com.ztc1997.fingerprint2sleep.service.FPQAService
import com.ztc1997.fingerprint2sleep.util.XposedProbe
import com.ztc1997.fingerprint2sleep.util.XposedUtils
import com.ztc1997.fingerprint2sleep.util.isAdmobHostBanned
import de.psdev.licensesdialog.LicensesDialog
import kotlinx.android.synthetic.main.activity_settings.*
import org.jetbrains.anko.*
import java.text.Collator
import java.util.*

class SettingsActivity : Activity() {
    companion object {
        val ACTION_PREF_CHANGED = SettingsActivity::class.java.name + ".ACTION_PREF_CHANGED"

        private const val REQ_OBTAIN_SHORTCUT = 0

        const val PREF_ENABLE_FINGERPRINT_QUICK_ACTION = "pref_enable_fingerprint_quick_action"
        const val PREF_BLACK_LIST = "pref_fpqa_black_list"
        const val PREF_RESPONSE_ENROLLED_FINGERPRINT_ONLY = "pref_response_enrolled_fingerprint_only"
        const val PREF_FORCE_NON_XPOSED_MODE = "pref_force_non_xposed_mode"
        const val PREF_NOTIFY_ON_ERROR = "pref_notify_on_error"
        const val PREF_FOREGROUND_SERVICE = "pref_foreground_service"
        const val PREF_AUTO_RETRY = "pref_auto_retry"
        const val PREF_AUTO_RETRY_BLACK_LIST = "pref_black_list"
        const val PREF_AGGRESSIVE_RETRY = "pref_aggressive_retry"
        const val PREF_AGGRESSIVE_RETRY_INTERVAL = "pref_aggressive_retry_interval"
        // const val PREF_DONATE = "pref_donate"
        const val PREF_SCREEN_OFF_METHOD = "pref_screen_off_method"
        const val PREF_CATEGORY_SINGLE_TAP = "pref_category_single_tap"
        const val PREF_ACTION_SINGLE_TAP = "pref_quick_action"
        const val PREF_CATEGORY_FAST_SWIPE = "pref_category_fast_swipe"
        const val PREF_ACTION_FAST_SWIPE = "pref_action_fast_swipe"
        const val PREF_SCREEN_NON_XPOSED_MODE = "pref_screen_non_xposed_mode"
        const val PREF_ACTION_SINGLE_TAP_APP = "pref_action_single_tap_app"
        const val PREF_ACTION_FAST_SWIPE_APP = "pref_action_fast_swipe_app"
        const val PREF_CATEGORY_DOUBLE_TAP = "pref_category_double_tap"
        const val PREF_ENABLE_DOUBLE_TAP = "pref_enable_double_tap"
        const val PREF_DOUBLE_TAP_INTERVAL = "pref_double_tap_interval"
        const val PREF_ACTION_DOUBLE_TAP = "pref_action_double_tap"
        const val PREF_ACTION_DOUBLE_TAP_APP = "pref_action_double_tap_app"

        const val PREF_CONTACT_DEVELOPER = "pref_contact_developer"
        const val PREF_MATTERS_NEED_ATTENTION = "pref_matters_need_attention"
        const val PREF_LICENSES = "pref_licenses"

        const val PREF_DO_NOT_DETECT_HARDWARE_AGAIN = "pref_do_not_detect_hardware_again"
        const val PREF_DO_NOT_CHECK_FINGERPRINTS_AGAIN = "pref_do_not_check_fingerprints_again"

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
        const val VALUES_PREF_QUICK_ACTION_TAKE_SCREENSHOT = "take_screenshot"
        const val VALUES_PREF_QUICK_ACTION_LAUNCH_APP = "launch_app"
        const val VALUES_PREF_QUICK_ACTION_FLASHLIGHT = "flashlight"

        const val VALUES_PREF_SCREEN_OFF_METHOD_SHORTEN_TIMEOUT = "shorten_timeout"
        const val VALUES_PREF_SCREEN_OFF_METHOD_DEVICE_ADMIN = "device_admin"
        const val VALUES_PREF_SCREEN_OFF_METHOD_POWER_BUTTON = "power_button"

        val PREF_KEYS_BOOLEAN = setOf(PREF_ENABLE_FINGERPRINT_QUICK_ACTION,
                PREF_RESPONSE_ENROLLED_FINGERPRINT_ONLY, PREF_NOTIFY_ON_ERROR,
                PREF_FOREGROUND_SERVICE, PREF_AUTO_RETRY, PREF_FORCE_NON_XPOSED_MODE,
                PREF_ENABLE_DOUBLE_TAP, PREF_AGGRESSIVE_RETRY)

        val PREF_KEYS_STRING = setOf(PREF_ACTION_SINGLE_TAP, PREF_ACTION_FAST_SWIPE,
                PREF_SCREEN_OFF_METHOD, PREF_ACTION_SINGLE_TAP_APP,
                PREF_ACTION_FAST_SWIPE_APP, PREF_ACTION_DOUBLE_TAP, PREF_ACTION_DOUBLE_TAP_APP,
                PREF_DOUBLE_TAP_INTERVAL, PREF_AGGRESSIVE_RETRY_INTERVAL)

        val PREF_KEYS_STRING_SET = setOf(PREF_BLACK_LIST, PREF_AUTO_RETRY_BLACK_LIST)

        val DELAY_RESTART_ACTIONS = setOf(VALUES_PREF_QUICK_ACTION_BACK,
                VALUES_PREF_QUICK_ACTION_HOME, VALUES_PREF_QUICK_ACTION_POWER_DIALOG,
                VALUES_PREF_QUICK_ACTION_TOGGLE_SPLIT_SCREEN, VALUES_PREF_QUICK_ACTION_LAUNCH_APP)

        val DONT_RESTART_ACTIONS = setOf(VALUES_PREF_QUICK_ACTION_SLEEP,
                VALUES_PREF_QUICK_ACTION_RECENTS)

        val XPOSED_MODULE_BLACKLIST = listOf("tw.fatminmin.xposed.minminguard",
                "com.aviraxp.adblocker.continued", "pl.cinek.adblocker")

        init {
            XposedUtils.disableXposedModules { name ->
                XPOSED_MODULE_BLACKLIST.any { name.startsWith(it) }
            }
        }
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

        doAsync {
            if (isAdmobHostBanned/* && !isFinishing*/) {
                uiThread { showAdBlockerDetected() }
                FirebaseAnalytics.getInstance(app).logEvent("AdmobHostBanned", null)
            }
        }

        if (defaultDPreference.getPrefInt(PREF_DO_NOT_DETECT_HARDWARE_AGAIN, -1) < 18 &&
                !fingerprintManager.isHardwareDetected) {
            alert(R.string.msg_dialog_device_does_not_support_fingerprint) {
                negativeButton(R.string.btn_do_not_detect_hardware_again) {
                    defaultDPreference.setPrefInt(PREF_DO_NOT_DETECT_HARDWARE_AGAIN, BuildConfig.VERSION_CODE)
                }
                positiveButton(android.R.string.ok) {}
                show()
            }
        }

        var hasEnrolledFingerprints = true
        try {
            hasEnrolledFingerprints = fingerprintManager.hasEnrolledFingerprints()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (defaultDPreference.getPrefInt(PREF_DO_NOT_CHECK_FINGERPRINTS_AGAIN, -1) < 18 &&
                !hasEnrolledFingerprints) {
            alert(R.string.msg_dialog_has_not_enrolled_fingerprints) {
                negativeButton(R.string.btn_do_not_check_fingerprints_again) {
                    defaultDPreference.setPrefInt(PREF_DO_NOT_CHECK_FINGERPRINTS_AGAIN, BuildConfig.VERSION_CODE)
                }
                positiveButton(android.R.string.ok) {}
                show()
            }
        }

        if (XposedProbe.isModuleActivated() && !XposedProbe.isModuleVersionMatched())
            longToast(R.string.toast_xposed_version_mismatched)

        loadAd()
    }

    override fun onResume() {
        super.onResume()
        bindService(Intent(this, FPQAService::class.java), conn, BIND_AUTO_CREATE)
        adView.resume()

        // if (billingProcessor.isPurchased(IAP_SKU_DONATE))
        //     adView.visibility = View.GONE
    }

    override fun onPause() {
        super.onPause()
        unbindService(conn)
        adView.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        adView.destroy()
    }

    fun loadAd() {
        with(adView) {
            if ((layoutParams.height < 50 && layoutParams.height >= 0) || visibility != View.VISIBLE)
                showAdBlockerDetected()

            val adRequest = AdRequest.Builder()
                    .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                    .build()

            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    backgroundColor = getColor(android.R.color.background_light)
                }

                override fun onAdFailedToLoad(p0: Int) {
                    Logger.d("onAdFailedToLoad($p0)")
                    tvAdZone.visibility = View.VISIBLE
                }
            }

            loadAd(adRequest)
        }
    }

    fun showAdBlockerDetected() {
        Logger.d("Ad blocker detected")
        tvAdZone.visibility = View.VISIBLE
        // tvAdZone.textSize = 25f
        // val tvlps = tvAdZone.layoutParams
        // tvlps.height = ViewGroup.LayoutParams.MATCH_PARENT
        // tvAdZone.layoutParams = tvlps
    }

    class SettingsFragment : PreferenceFragment(), SharedPreferences.OnSharedPreferenceChangeListener {
        val activity by lazy { act as SettingsActivity }

        // val donate: Preference by lazy { findPreference(PREF_DONATE) }
        val FPQASwitch by lazy { findPreference(PREF_ENABLE_FINGERPRINT_QUICK_ACTION) as CheckBoxPreference }
        val blacklist by lazy { findPreference(PREF_BLACK_LIST) as MultiSelectListPreference }
        val forceNonXposed by lazy { findPreference(PREF_FORCE_NON_XPOSED_MODE) as CheckBoxPreference }
        val nonXposedScreen by lazy { findPreference(PREF_SCREEN_NON_XPOSED_MODE) as PreferenceScreen }

        val categorySingleTap by lazy { findPreference(PREF_CATEGORY_SINGLE_TAP) as PreferenceCategory }
        val actionSingleTap by lazy { findPreference(PREF_ACTION_SINGLE_TAP) as ListPreference }
        val actionSingleTapApp by lazy { findPreference(PREF_ACTION_SINGLE_TAP_APP) as AppPickerPreference }

        val categoryFastSwipe by lazy { findPreference(PREF_CATEGORY_FAST_SWIPE) as PreferenceCategory }
        val actionFastSwipe by lazy { findPreference(PREF_ACTION_FAST_SWIPE) as ListPreference }
        val actionFastSwipeApp by lazy { findPreference(PREF_ACTION_FAST_SWIPE_APP) as AppPickerPreference }

        val categoryDoubleTap by lazy { findPreference(PREF_CATEGORY_DOUBLE_TAP) as PreferenceCategory }
        val doubleTapInterval by lazy { findPreference(PREF_DOUBLE_TAP_INTERVAL) as ListPreference }
        val aggressiveRetryInterval by lazy { findPreference(PREF_AGGRESSIVE_RETRY_INTERVAL) as ListPreference }
        val actionDoubleTap by lazy { findPreference(PREF_ACTION_DOUBLE_TAP) as ListPreference }
        val actionDoubleTapApp by lazy { findPreference(PREF_ACTION_DOUBLE_TAP_APP) as AppPickerPreference }

        val screenOffMethod by lazy { findPreference(PREF_SCREEN_OFF_METHOD) as ListPreference }
        val autoRetryBlacklist by lazy { findPreference(PREF_AUTO_RETRY_BLACK_LIST) as MultiSelectListPreference }

        val contact: Preference by lazy { findPreference(PREF_CONTACT_DEVELOPER) }
        val attention: Preference by lazy { findPreference(PREF_MATTERS_NEED_ATTENTION) }
        val licenses: Preference by lazy { findPreference(PREF_LICENSES) }

        val listPreferences by lazy {
            arrayOf(actionSingleTap, doubleTapInterval, aggressiveRetryInterval, actionDoubleTap, screenOffMethod)
        }

        private val loadAppsTask by lazy { LoadAppsTask() }

        @SuppressLint("WorldReadableFiles")
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N)
                preferenceManager.sharedPreferencesMode = Context.MODE_WORLD_READABLE

            addPreferencesFromResource(R.xml.pref_settings)

            AppPickerPreference.settingsFragment = this

            val moduleActivated = XposedProbe.isModuleActivated()

            licenses.setOnPreferenceClickListener {
                LicensesDialog.Builder(act)
                        .setNotices(R.raw.licenses)
                        .setIncludeOwnLicense(false)
                        .build()
                        .show()
                true
            }

            attention.setOnPreferenceClickListener {
                showAttention()
                true
            }
            if (defaultDPreference.getPrefInt(PREF_MATTERS_NEED_ATTENTION, -1) < 0)
                showAttention()

            forceNonXposed.isEnabled = moduleActivated
            forceNonXposed.summary = getString(if (moduleActivated)
                R.string.summary_pref_screen_xposed_mode_activated else
                R.string.summary_pref_screen_xposed_mode_inactivated)

            nonXposedScreen.isEnabled = !moduleActivated or
                    defaultSharedPreferences.getBoolean(PREF_FORCE_NON_XPOSED_MODE, false)

            loadAppsTask.execute()

            listPreferences.forEach {
                it.setOnPreferenceChangeListener { preference, any ->
                    if (preference is ListPreference && any is String) {
                        val index = preference.findIndexOfValue(any)

                        preference.summary = if (index >= 0)
                            preference.entries[index] else null
                    }
                    true
                }
            }
        }

        override fun onDestroy() {
            super.onDestroy()
            loadAppsTask.cancel(true)
        }

        override fun onResume() {
            super.onResume()
            defaultSharedPreferences.registerOnSharedPreferenceChangeListener(this)

            val singleTapAction = defaultSharedPreferences.getString(PREF_ACTION_SINGLE_TAP,
                    VALUES_PREF_QUICK_ACTION_NONE)
            actionSingleTapApp.isEnabled = singleTapAction == VALUES_PREF_QUICK_ACTION_LAUNCH_APP
            updateActionSingleTapAppVisibility(singleTapAction)

            val fastSwipeAction = defaultSharedPreferences.getString(PREF_ACTION_FAST_SWIPE,
                    VALUES_PREF_QUICK_ACTION_NONE)
            actionFastSwipeApp.isEnabled = fastSwipeAction == VALUES_PREF_QUICK_ACTION_LAUNCH_APP
            updateActionFastSwipeAppVisibility(fastSwipeAction)

            val doubleTapAction = defaultSharedPreferences.getString(PREF_ACTION_DOUBLE_TAP,
                    VALUES_PREF_QUICK_ACTION_NONE)
            actionDoubleTapApp.isEnabled = doubleTapAction == VALUES_PREF_QUICK_ACTION_LAUNCH_APP
            updateActionDoubleTapAppVisibility(doubleTapAction)

            listPreferences.forEach { it.summary = it.entry }
            actionFastSwipe.summary = getString(R.string.summary_pref_action_fast_swipe) +
                    "\n" + actionFastSwipe.entry
        }

        override fun onPause() {
            super.onPause()
            defaultSharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
            val intent = Intent(ACTION_PREF_CHANGED)
            intent.putExtra("key", key)

            when (key) {
                in PREF_KEYS_BOOLEAN -> {
                    val value = sharedPreferences.getBoolean(key, false)
                    defaultDPreference.setPrefBoolean(key, value)
                    intent.putExtra("value", value)
                }

                in PREF_KEYS_STRING -> {
                    val value = sharedPreferences.getString(key, "")
                    defaultDPreference.setPrefString(key, value)
                    intent.putExtra("value", value)
                }

                in PREF_KEYS_STRING_SET -> {
                    val value = sharedPreferences.getStringSet(key, emptySet())
                    defaultDPreference.setPrefStringSet(key, value)
                    intent.putExtra("value", value.toTypedArray())
                }
            }

            activity.sendBroadcast(intent)

            try {
                activity.bgService?.onPrefChanged(key)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            when (key) {
                PREF_ENABLE_FINGERPRINT_QUICK_ACTION -> {
                    if (XposedProbe.isModuleActivated() and
                            !sharedPreferences.getBoolean(PREF_FORCE_NON_XPOSED_MODE, false))
                    else if (sharedPreferences.getBoolean(PREF_ENABLE_FINGERPRINT_QUICK_ACTION, false))
                        StartFPQAActivity.startActivity(ctx)
                }

                PREF_ACTION_SINGLE_TAP -> {
                    val singleTapAction = defaultSharedPreferences.getString(PREF_ACTION_SINGLE_TAP,
                            VALUES_PREF_QUICK_ACTION_NONE)
                    actionSingleTapApp.isEnabled = singleTapAction == VALUES_PREF_QUICK_ACTION_LAUNCH_APP
                    updateActionSingleTapAppVisibility(singleTapAction)
                }

                PREF_ACTION_FAST_SWIPE -> {
                    val fastSwipeAction = sharedPreferences.getString(PREF_ACTION_FAST_SWIPE,
                            VALUES_PREF_QUICK_ACTION_NONE)
                    actionFastSwipeApp.isEnabled = fastSwipeAction == VALUES_PREF_QUICK_ACTION_LAUNCH_APP
                    updateActionFastSwipeAppVisibility(fastSwipeAction)
                    actionFastSwipe.summary = getString(R.string.summary_pref_action_fast_swipe) +
                            "\n" + actionFastSwipe.entry
                }

                PREF_ACTION_DOUBLE_TAP -> {
                    val doubleTapAction = defaultSharedPreferences.getString(PREF_ACTION_DOUBLE_TAP,
                            VALUES_PREF_QUICK_ACTION_NONE)
                    actionDoubleTapApp.isEnabled = doubleTapAction == VALUES_PREF_QUICK_ACTION_LAUNCH_APP
                    updateActionDoubleTapAppVisibility(doubleTapAction)
                }

                PREF_FORCE_NON_XPOSED_MODE -> {
                    val forceNonXposed = sharedPreferences.getBoolean(PREF_FORCE_NON_XPOSED_MODE, false)

                    nonXposedScreen.isEnabled = !XposedProbe.isModuleActivated() or
                            forceNonXposed

                    if (sharedPreferences.getBoolean(PREF_ENABLE_FINGERPRINT_QUICK_ACTION, false) and
                            forceNonXposed)
                        StartFPQAActivity.startActivity(ctx)
                }
            }
        }

        private fun showAttention() {
            alert(R.string.content_matters_need_attention, R.string.title_matters_need_attention) {
                positiveButton(R.string.btn_matters_need_attention) {
                    defaultDPreference.setPrefInt(PREF_MATTERS_NEED_ATTENTION, BuildConfig.VERSION_CODE)
                }
                show()
            }
        }

        private fun updateActionSingleTapAppVisibility(value: String) = updateActionVisibility(value == VALUES_PREF_QUICK_ACTION_LAUNCH_APP, actionSingleTapApp, categorySingleTap)

        private fun updateActionFastSwipeAppVisibility(value: String) = updateActionVisibility(value == VALUES_PREF_QUICK_ACTION_LAUNCH_APP, actionFastSwipeApp, categoryFastSwipe)

        private fun updateActionDoubleTapAppVisibility(value: String) = updateActionVisibility(value == VALUES_PREF_QUICK_ACTION_LAUNCH_APP, actionDoubleTapApp, categoryDoubleTap)


        private fun updateActionVisibility(visibility: Boolean, preference: Preference, parent: PreferenceGroup) {
            if (visibility)
                parent.addPreference(preference)
            else
                parent.removePreference(preference)
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
                    if (data == null) return

                    val shortcutIntent = data.getParcelableExtra<Intent>(Intent.EXTRA_SHORTCUT_INTENT)
                    val handleShortcut = {
                        var localIconResName: String? = null
                        var b: Bitmap? = null
                        val siRes = data.getParcelableExtra<Intent.ShortcutIconResource>(Intent.EXTRA_SHORTCUT_ICON_RESOURCE)
                        if (siRes != null) {
                            if (AppPickerPreference.ACTION_LAUNCH_ACTION == shortcutIntent?.action) {
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
                    }

                    if (shortcutIntent?.action == Intent.ACTION_CALL)
                        RxPermissions(activity)
                                .request(Manifest.permission.CALL_PHONE)
                                .subscribe {
                                    if (it) handleShortcut()
                                    else
                                        shortcutHandler?.onShortcutCancelled()
                                }
                    else
                        handleShortcut()
                } else {
                    shortcutHandler?.onShortcutCancelled()
                }
            }
        }

        private inner class LoadAppsTask : AsyncTask<Unit, Unit, Pair<Array<CharSequence>, Array<CharSequence>>?>() {
            override fun onPreExecute() {
                autoRetryBlacklist.isEnabled = false
                blacklist.isEnabled = false
            }

            override fun doInBackground(vararg args: Unit): Pair<Array<CharSequence>, Array<CharSequence>>? {
                val packages = context.packageManager
                        .getInstalledApplications(PackageManager.GET_META_DATA)

                val sortedApps = packages.mapTo(ArrayList()) {
                    if (isCancelled) return null

                    arrayOf(it.packageName, it.loadLabel(context.packageManager)
                            .toString())
                }

                val comparator = object : Comparator<Array<String>> {
                    val collator = Collator.getInstance()
                    override fun compare(o1: Array<String>, o2: Array<String>) = collator.compare(o1[1], o2[1])
                }
                sortedApps.sortWith(comparator)

                val appNamesList = mutableListOf<CharSequence>()
                val packageNamesList = mutableListOf<CharSequence>()

                for (i in sortedApps.indices) {
                    if (isCancelled) return null

                    appNamesList.add(sortedApps[i][1] + "\n" + "(" + sortedApps[i][0] + ")")
                    packageNamesList.add(sortedApps[i][0])
                }

                val appNames = appNamesList.toTypedArray()
                val packageNames = packageNamesList.toTypedArray()

                return appNames to packageNames
            }

            override fun onPostExecute(result: Pair<Array<CharSequence>, Array<CharSequence>>?) {
                if (result == null) return

                autoRetryBlacklist.entries = result.first
                autoRetryBlacklist.entryValues = result.second
                autoRetryBlacklist.isEnabled = true

                blacklist.entries = result.first
                blacklist.entryValues = result.second
                blacklist.isEnabled = true
            }
        }

    }
}