package com.ztc1997.fingerprint2sleep.service

import android.app.Activity
import android.app.IntentService
import android.app.Notification
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.drawable.Icon
import android.hardware.display.DisplayManager
import android.media.ImageReader
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.support.v4.content.FileProvider
import com.ztc1997.fingerprint2sleep.BuildConfig
import com.ztc1997.fingerprint2sleep.R
import com.ztc1997.fingerprint2sleep.extension.saveImage
import org.jetbrains.anko.mediaProjectionManager
import org.jetbrains.anko.notificationManager
import org.jetbrains.anko.windowManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


class ScreenshotService : IntentService("ScreenshotService") {
    companion object {
        val DIR_SCREENSHOTS = Environment.getExternalStorageDirectory().absolutePath +
                "${File.separator}Pictures${File.separator}Screenshots"

        private const val NOTIFICATION_ID = 2

        const val NOTIFICATION_INTENT_CLEAR = 0
        const val NOTIFICATION_INTENT_DELETE = 1
        const val NOTIFICATION_INTENT_OPEN = 2

        val ACTION_NOTIFICATION_CLEAR = ScreenshotService::class.java.name +
                ".intent.ACTION_NOTIFICATION_CLEAR"
        val ACTION_NOTIFICATION_DELETE = ScreenshotService::class.java.name +
                ".intent.ACTION_NOTIFICATION_DELETE"
    }

    override fun onHandleIntent(intent: Intent?) {
        if (intent == null) return

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

            val formatter = SimpleDateFormat("yyyyMMdd-HHmmss")
            val curDate = Date(System.currentTimeMillis())
            val timeStr = formatter.format(curDate)
            val path = File(DIR_SCREENSHOTS, "Screenshot_$timeStr.jpg")

            bmp.saveImage(path)
            MediaScannerConnection.scanFile(this, arrayOf(path.path), null, null)
            showNotification(bmp, path)

            bmp.recycle()
        }, Handler())
    }

    private fun showNotification(pic: Bitmap, path: File) {
        val deleteIntent = PendingIntent.getBroadcast(this, NOTIFICATION_INTENT_DELETE,
                Intent(ACTION_NOTIFICATION_DELETE).putExtra("path", path),
                PendingIntent.FLAG_UPDATE_CURRENT)
        val clearIntent = PendingIntent.getBroadcast(this, NOTIFICATION_INTENT_CLEAR,
                Intent(ACTION_NOTIFICATION_CLEAR), PendingIntent.FLAG_UPDATE_CURRENT)

        val openIntent = Intent(Intent.ACTION_VIEW)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            openIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            val contentUri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID +
                    ".fileProvider", path)
            openIntent.setDataAndType(contentUri, "image/*")
        } else {
            openIntent.setDataAndType(Uri.fromFile(path), "image/*")
            openIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        val openPendingIntent = PendingIntent.getActivity(this, NOTIFICATION_INTENT_OPEN,
                openIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val notif = Notification.Builder(this)
                .setAutoCancel(true)
                .setContentTitle(getString(R.string.screenshot_notification_content))
                .setSmallIcon(R.drawable.ic_photo_white_24dp)
                .setLargeIcon(pic)
                .setStyle(Notification.BigPictureStyle()
                        .bigPicture(pic))
                .setContentIntent(openPendingIntent)
                .setDeleteIntent(clearIntent)
                .addAction(Notification.Action.Builder(
                        Icon.createWithResource(this, R.drawable.ic_delete_grey_800_24dp),
                        getString(R.string.screenshot_notification_action_delete),
                        deleteIntent)
                        .build())
                .build()

        notificationManager.notify(NOTIFICATION_ID, notif)

        val filter = IntentFilter()
        filter.addAction(ACTION_NOTIFICATION_DELETE)
        filter.addAction(ACTION_NOTIFICATION_CLEAR)
        registerReceiver(NotificationReceiver, filter)
    }

    object NotificationReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (context == null || intent == null) return
            context.notificationManager.cancel(NOTIFICATION_ID)
            context.unregisterReceiver(this)

            when (intent.action) {
                ACTION_NOTIFICATION_DELETE -> try {
                    val path = intent.getSerializableExtra("path") as File
                    path.delete()
                    MediaScannerConnection.scanFile(context, arrayOf(path.path), null, null)
                } catch(e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}