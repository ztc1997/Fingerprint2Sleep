package com.ztc1997.fingerprint2sleep.service

import android.annotation.TargetApi
import android.app.*
import android.app.job.JobInfo
import android.content.*
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.drawable.Icon
import android.hardware.camera2.CameraManager
import android.hardware.display.DisplayManager
import android.media.ImageReader
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.*
import android.support.v4.app.NotificationCompat
import android.support.v4.content.FileProvider
import com.eightbitlab.rxbus.Bus
import com.ztc1997.fingerprint2sleep.BuildConfig
import com.ztc1997.fingerprint2sleep.R
import com.ztc1997.fingerprint2sleep.activity.RequireAccessibilityActivity
import com.ztc1997.fingerprint2sleep.activity.SettingsActivity
import com.ztc1997.fingerprint2sleep.activity.ShortenTimeOutActivity
import com.ztc1997.fingerprint2sleep.activity.StartFPQAActivity
import com.ztc1997.fingerprint2sleep.aidl.IFPQAService
import com.ztc1997.fingerprint2sleep.app
import com.ztc1997.fingerprint2sleep.defaultDPreference
import com.ztc1997.fingerprint2sleep.extension.jobScheduler
import com.ztc1997.fingerprint2sleep.extension.saveImage
import com.ztc1997.fingerprint2sleep.extension.setScreenTimeOut
import com.ztc1997.fingerprint2sleep.extra.*
import com.ztc1997.fingerprint2sleep.quickactions.IQuickActions
import com.ztc1997.fingerprint2sleep.quickactions.NonXposedQuickActions
import com.ztc1997.fingerprint2sleep.receiver.StartFPQAReceiver
import org.jetbrains.anko.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class FPQAService : Service() {
    companion object {
        val DIR_SCREENSHOTS = Environment.getExternalStorageDirectory().absolutePath +
                "${File.separator}Pictures${File.separator}Screenshots"

        private const val NOTIFICATION_ID_FPQA = 1
        private const val NOTIFICATION_ID_TAKE_SCREENSHOT = 2

        private const val NOTIFICATION_INTENT_FPQA_CONTENT = 0
        private const val NOTIFICATION_INTENT_TAKE_SCREENSHOT_DELETE = 1
        private const val NOTIFICATION_INTENT_TAKE_SCREENSHOT_OPEN = 2


        const val NOTIFICATION_CHANNEL_ID = "FPQA_BG_CHANNEL_ID"

        private val ACTION_TAKE_SCREENSHOT_NOTIFICATION_DELETE = FPQAService::class.java.name +
                ".intent.ACTION_TAKE_SCREENSHOT_NOTIFICATION_DELETE"

        val ACTION_RESTART_SCANNING = FPQAService::class.java.name + "ACTION_RESTART_SCANNING"

        val CLASS_BLACK_LIST by lazy {
            setOf(
                    ShortenTimeOutActivity::class.java.name,
                    /* AOSP */
                    "com.android.settings.fingerprint.FingerprintSettings",
                    "com.android.settings.fingerprint.FingerprintEnrollEnrolling",
                    "com.android.systemui.recents.RecentsActivity",
                    /* MIUI */
                    "com.android.settings.NewFingerprintInternalActivity",
                    "com.miui.applicationlock.ConfirmAccessControl",
                    /* AliPay */
                    "com.alipay.android.app.flybird.ui.window.FlyBirdWindowActivity"
            )
        }

        var isRunning = false
            private set
    }

    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String?, enabled: Boolean) {
            super.onTorchModeChanged(cameraId, enabled)
            quickActions.flashState = enabled
        }
    }

    // var delayIsScanning = false

    var isScanning = false
        set(value) {
            field = value
            if (value)
            // delayIsScanning = value
            else
                Bus.send(IsScanningChangedEvent(value))
        }
        get() = field and (!defaultDPreference.getPrefBoolean(SettingsActivity.PREF_AGGRESSIVE_RETRY,
                false) or (System.currentTimeMillis() - lastRestartTime < defaultDPreference
                .getPrefString(SettingsActivity.PREF_AGGRESSIVE_RETRY_INTERVAL, "20000").toInt()))

    var isError = false
        set(value) {
            field = value
            if (value)
                Bus.send(OnAuthenticationErrorEvent)
            else
                startForegroundIfSet()
        }

    var errString = ""

    var lastPkgName = ""

    var lastClassName = ""

    var errorPkgName = ""

    var lastRestartTime = 0L

    var isStarting = false

    var cancellationSignal = CancellationSignal()

    val quickActions = NonXposedQuickActions(ctx)

    val authenticationCallback by lazy { Callback(quickActions) }

    val handler = Handler()

    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return

            val restartScanning = {
                isScanning = false
                StartFPQAActivity.startActivity(ctx)

                Bus.send(RestartScanningDelayedEvent)

                val screenTimeout = defaultDPreference.getPrefInt(ShortenTimeOutActivity.PREF_ORIGINAL_SCREEN_OFF_TIMEOUT, -1)
                if (screenTimeout > 0)
                    setScreenTimeOut(screenTimeout)
                defaultDPreference.setPrefInt(ShortenTimeOutActivity.PREF_ORIGINAL_SCREEN_OFF_TIMEOUT, -1)
            }

            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    cancellationSignal.cancel()
                    isScanning = false

                    val screenTimeout = defaultDPreference.getPrefInt(ShortenTimeOutActivity.PREF_ORIGINAL_SCREEN_OFF_TIMEOUT, -1)
                    if (screenTimeout > 0)
                        setScreenTimeOut(screenTimeout)
                    defaultDPreference.setPrefInt(ShortenTimeOutActivity.PREF_ORIGINAL_SCREEN_OFF_TIMEOUT, -1)
                }

                Intent.ACTION_USER_PRESENT -> restartScanning()
                ACTION_RESTART_SCANNING -> restartScanning()
                ACTION_TAKE_SCREENSHOT_NOTIFICATION_DELETE -> try {
                    notificationManager.cancel(NOTIFICATION_ID_TAKE_SCREENSHOT)
                    val path = intent.getSerializableExtra("path") as File
                    path.delete()
                    MediaScannerConnection.scanFile(context, arrayOf(path.path), null, null)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    val binder = object : IFPQAService.Stub() {
        override fun onPrefChanged(key: String?) {
            when (key) {
                SettingsActivity.PREF_FOREGROUND_SERVICE -> startForegroundIfSet()

                SettingsActivity.PREF_ENABLE_FINGERPRINT_QUICK_ACTION ->
                    if (!defaultDPreference.getPrefBoolean(key, false))
                        stopFPQA()

                SettingsActivity.PREF_SCREEN_OFF_METHOD ->
                    if (defaultDPreference.getPrefString(key, SettingsActivity.VALUES_PREF_SCREEN_OFF_METHOD_SHORTEN_TIMEOUT) ==
                            SettingsActivity.VALUES_PREF_SCREEN_OFF_METHOD_POWER_BUTTON)
                        checkAndStartRoot()

                SettingsActivity.PREF_FORCE_NON_XPOSED_MODE ->
                    if (!defaultDPreference
                                    .getPrefBoolean(SettingsActivity.PREF_FORCE_NON_XPOSED_MODE, false))
                        stopFPQA()

                SettingsActivity.PREF_ENABLE_DOUBLE_TAP ->
                    authenticationCallback.doubleTapEnabled =
                            defaultDPreference.getPrefBoolean(SettingsActivity.PREF_ENABLE_DOUBLE_TAP, false)

                SettingsActivity.PREF_DOUBLE_TAP_INTERVAL ->
                    authenticationCallback.doubleTapInterval =
                            defaultDPreference.getPrefString(SettingsActivity.PREF_DOUBLE_TAP_INTERVAL, "500").toLong()
            }
        }

        override fun isRunning() = FPQAService.isRunning
    }

    override fun onCreate() {
        super.onCreate()

        cameraManager.registerTorchCallback(torchCallback, null)
    }

    override fun onDestroy() {
        super.onDestroy()

        stopFPQA(false)
        cameraManager.unregisterTorchCallback(torchCallback)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null && intent.getBooleanExtra("take screenshot", false)) {
            takeScreenshot(intent)
        } else {
            startFPQA()

            restartScanning()

            if (!FPQAAccessibilityService.isRunning
                    && System.currentTimeMillis() > StartFPQAReceiver.CHECK_ACCESSIBILITY_AFTER
                    && defaultDPreference.getPrefInt(RequireAccessibilityActivity
                            .PREF_DO_NOT_CHECK_ACCESSIBILITY_AGAIN, -1) < 29)
                RequireAccessibilityActivity.startActivity(this)

            if (defaultDPreference.getPrefString(SettingsActivity.PREF_SCREEN_OFF_METHOD,
                            SettingsActivity.VALUES_PREF_SCREEN_OFF_METHOD_SHORTEN_TIMEOUT) ==
                    SettingsActivity.VALUES_PREF_SCREEN_OFF_METHOD_POWER_BUTTON)
                checkAndStartRoot()
        }

        val newFlags = flags or START_STICKY
        return super.onStartCommand(intent, newFlags, startId)
    }

    fun restartScanning() {
        if (cancellationSignal.isCanceled) cancellationSignal = CancellationSignal()

        if (!isScanning) {
            isStarting = true
            fingerprintManager.authenticate(null, cancellationSignal, 0, authenticationCallback, null)
            handler.postDelayed({ isStarting = false }, 100)
            isScanning = true
            lastRestartTime = System.currentTimeMillis()
        }

        Bus.send(FinishStartFPQAActivityEvent)

        isError = false
    }

    fun startFPQA() {
        if (!isRunning) {
            isRunning = true

            val filter = IntentFilter()
            filter.addAction(Intent.ACTION_USER_PRESENT)
            filter.addAction(Intent.ACTION_SCREEN_OFF)
            filter.addAction(ACTION_RESTART_SCANNING)
            registerReceiver(receiver, filter)

            Bus.observe<ActivityChangedEvent>()
                    .doOnEach {
                        val event = (it.value as ActivityChangedEvent).event
                        lastPkgName = event.packageName.toString()
                        lastClassName = event.className.toString()
                    }
                    .filter { defaultDPreference.getPrefBoolean(SettingsActivity.PREF_AUTO_RETRY, true) }
                    .filter { lastPkgName != errorPkgName }
                    .filter {
                        val b = lastPkgName !in defaultDPreference.getPrefStringSet(SettingsActivity.PREF_BLACK_LIST, emptySet())
                        if (!b && !cancellationSignal.isCanceled) cancellationSignal.cancel()
                        b
                    }
                    .filter { lastPkgName !in defaultDPreference.getPrefStringSet(SettingsActivity.PREF_AUTO_RETRY_BLACK_LIST, emptySet()) }
                    .filter { lastClassName !in CLASS_BLACK_LIST }
                    // .filter { !delayIsScanning }
                    .throttleLast(200, TimeUnit.MILLISECONDS)
                    .filter { !isScanning && isRunning }
                    .subscribe { onActivityChanged() }

            // Bus.observe<IsScanningChangedEvent>()
            //         .throttleLast(THROTTLE_DELAY, TimeUnit.MILLISECONDS)
            //         .subscribe { delayIsScanning = it.value }

            Bus.observe<OnAuthenticationErrorEvent>()
                    .delay(200, TimeUnit.MILLISECONDS)
                    .filter { !isScanning }
                    .subscribe { startForegroundIfSet() }

            Bus.observe<RestartScanningDelayedEvent>()
                    .delay(200, TimeUnit.MILLISECONDS)
                    .filter { !isScanning && isRunning }
                    .subscribe { StartFPQAActivity.startActivity(ctx) }

            Bus.observe<RestartScanningEvent>().subscribe { restartScanning() }
        }

        val info = JobInfo.Builder(DaemonJobService.ID,
                ComponentName(BuildConfig.APPLICATION_ID, DaemonJobService::class.java.name))
                .setPersisted(true)
                .setPeriodic(10 * 1000)
                .build()
        jobScheduler.schedule(info)
    }

    fun stopFPQA(prefChanged: Boolean = true) {
        if (isRunning) {
            isRunning = false

            unregisterReceiver(receiver)

            Bus.unregister(this)
            // delayIsScanning = isScanning

            cancellationSignal.cancel()
            stopForeground(true)
            stopSelf()
        }
        if (prefChanged)
            jobScheduler.cancel(DaemonJobService.ID)
    }

    fun onActivityChanged() {
        StartFPQAActivity.startActivity(ctx)
    }

    fun startForegroundIfSet() {
        if (isRunning && defaultDPreference.getPrefBoolean(SettingsActivity.PREF_FOREGROUND_SERVICE, false)) {

            createNotificationChannel()

            val notification = if (isError)
                generateNotification(getString(R.string.notification_content_text_retry) + errString, true)
            else generateNotification(R.string.notification_content_text)

            startForeground(NOTIFICATION_ID_FPQA, notification)
        } else {
            stopForeground(true)
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    inline fun generateNotification(textRes: Int) = generateNotification(getString(textRes))

    fun generateNotification(text: CharSequence, restart: Boolean = false): Notification {
        val pendingIntent = if (restart)
            PendingIntent.getBroadcast(app,
                    NOTIFICATION_INTENT_FPQA_CONTENT, Intent(ACTION_RESTART_SCANNING),
                    PendingIntent.FLAG_UPDATE_CURRENT)
        else
            PendingIntent.getActivity(app,
                    NOTIFICATION_INTENT_FPQA_CONTENT, Intent(app, SettingsActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT)

        val notification = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_fingerprint_white_24dp)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(text)
                .setPriority(Notification.PRIORITY_MIN)
                .setContentIntent(pendingIntent)
                .build()

        return notification
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val channelName = getString(R.string.notification_name)
        val chan = NotificationChannel(NOTIFICATION_CHANNEL_ID,
                channelName, NotificationManager.IMPORTANCE_NONE)
        chan.lightColor = Color.DKGRAY
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        notificationManager.createNotificationChannel(chan)
    }

    fun checkAndStartRoot() {
        quickActions.anycall.startShell {
            if (!it) runOnUiThread { toast(R.string.toast_root_access_failed) }
        }
    }

    fun takeScreenshot(intent: Intent) {
        val resultCode = intent.getIntExtra("resultCode", Activity.RESULT_OK)
        val data = intent.getParcelableExtra<Intent>("data")

        val mp = mediaProjectionManager.getMediaProjection(resultCode, data)

        val point = Point()
        windowManager.defaultDisplay.getRealSize(point)
        val reader = ImageReader.newInstance(point.x, point.y, PixelFormat.RGBA_8888, 2)

        val vd = mp.createVirtualDisplay("screenshot", reader.width,
                reader.height, resources.displayMetrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC, reader.surface, null, null)

        reader.setOnImageAvailableListener({
            val image = it.acquireLatestImage()

            val planes = image.planes
            val buffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * it.width
            // create bitmap
            var bmp = Bitmap.createBitmap(it.width + rowPadding / pixelStride,
                    it.height, Bitmap.Config.ARGB_8888)
            bmp.copyPixelsFromBuffer(buffer)
            image.close()
            it.close()
            vd.release()
            mp.stop()

            if (bmp.width > it.width) {
                val raw = bmp
                bmp = Bitmap.createBitmap(raw, 0, 0, it.width, it.height, null, false)
                raw.recycle()
            }

            val formatter = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault())
            val curDate = Date(System.currentTimeMillis())
            val timeStr = formatter.format(curDate)
            val path = File(DIR_SCREENSHOTS, "Screenshot_$timeStr.jpg")

            bmp.saveImage(path)
            MediaScannerConnection.scanFile(this, arrayOf(path.path), null, null)
            showTakeScreenshotNotification(bmp, path)

            bmp.recycle()
        }, null)
    }

    private fun showTakeScreenshotNotification(pic: Bitmap, path: File) {
        val deleteIntent = PendingIntent.getBroadcast(this, NOTIFICATION_INTENT_TAKE_SCREENSHOT_DELETE,
                Intent(ACTION_TAKE_SCREENSHOT_NOTIFICATION_DELETE).putExtra("path", path),
                PendingIntent.FLAG_UPDATE_CURRENT)

        val openIntent = Intent(Intent.ACTION_VIEW)
        val shareIntent = Intent(Intent.ACTION_SEND)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            openIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            shareIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            val uri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID +
                    ".fileProvider", path)
            openIntent.setDataAndType(uri, "image/*")
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
            shareIntent.type = "image/*"
        } else {
            val uri = Uri.fromFile(path)
            openIntent.setDataAndType(uri, "image/*")
            openIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
            shareIntent.type = "image/*"
            shareIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        val openPendingIntent = PendingIntent.getActivity(this, NOTIFICATION_INTENT_TAKE_SCREENSHOT_OPEN,
                openIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val sharePendingIntent = PendingIntent.getActivity(this, NOTIFICATION_INTENT_TAKE_SCREENSHOT_OPEN,
                shareIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val notif = Notification.Builder(this)
                .setAutoCancel(true)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.screenshot_notification_content))
                .setSmallIcon(R.drawable.ic_photo_white_24dp)
                .setLargeIcon(pic)
                .setStyle(Notification.BigPictureStyle()
                        .bigPicture(pic))
                .setContentIntent(openPendingIntent)
                .addAction(Notification.Action.Builder(
                        Icon.createWithResource(this, R.drawable.ic_delete_grey_800_24dp),
                        getString(R.string.screenshot_notification_action_delete),
                        deleteIntent)
                        .build())
                .addAction(Notification.Action.Builder(
                        Icon.createWithResource(this, R.drawable.ic_share_grey_800_24dp),
                        getString(R.string.screenshot_notification_action_share),
                        sharePendingIntent)
                        .build())
                .build()

        notificationManager.notify(NOTIFICATION_ID_TAKE_SCREENSHOT, notif)

        val filter = IntentFilter()
        filter.addAction(ACTION_TAKE_SCREENSHOT_NOTIFICATION_DELETE)
        registerReceiver(receiver, filter)
    }

    override fun onBind(intent: Intent): IBinder? {
        return binder
    }

    object OnAuthenticationErrorEvent

    inner class Callback(quickActions: IQuickActions) : GestureAuthenticationCallback(quickActions) {

        override var doubleTapEnabled =
                defaultDPreference.getPrefBoolean(SettingsActivity.PREF_ENABLE_DOUBLE_TAP, false)

        override var doubleTapInterval =
                defaultDPreference.getPrefString(SettingsActivity.PREF_DOUBLE_TAP_INTERVAL, "500").toLong()

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
            super.onAuthenticationError(errorCode, errString)

            if (isStarting) return

            if (defaultDPreference.getPrefBoolean(SettingsActivity.PREF_NOTIFY_ON_ERROR, false))
                errString?.let { toast(getString(R.string.toast_notify_on_error, it)) }

            this@FPQAService.errString = errString?.toString().orEmpty()

            isError = true
            isScanning = false

            errorPkgName = lastPkgName
        }

        override fun restartScanning(action: String?) {
            isScanning = false

            if (action in SettingsActivity.DONT_RESTART_ACTIONS)
                return

            StartFPQAActivity.startActivity(ctx)
        }
    }
}
