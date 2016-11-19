package com.ztc1997.fingerprint2sleep.apppicker

import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.LruCache
import android.util.TypedValue
import com.ztc1997.fingerprint2sleep.BuildConfig
import java.util.*


object AppPickerDialog {
    private val appIconCache: LruCache<String, BitmapDrawable>

    var appIconSizePx = 0

    init {
        val cacheSize = Math.min(Runtime.getRuntime().maxMemory().toInt() / 6, 4194304)
        appIconCache = object : LruCache<String, BitmapDrawable>(cacheSize) {
            override fun sizeOf(key: String?, value: BitmapDrawable) = value.bitmap.byteCount
        }
    }

    fun show(ctx: Context, selected: String = "", callback: (String) -> Boolean) {
        if (appIconSizePx == 0)
            appIconSizePx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40f,
                    ctx.resources.displayMetrics).toInt()

        var alert: AlertDialog? = null

        val adapter = AppPickerAdapter(ctx, loadDate(ctx, false), selected) {
            if (callback(selected))
                alert?.dismiss()
        }

        alert = AlertDialog.Builder(ctx)
                .setAdapter(adapter, null)
                .show()
    }

    fun loadDate(ctx: Context, isShortcut: Boolean): List<AppInfo> {
        val appList = ArrayList<ResolveInfo>()

        val packages = ctx.packageManager.getInstalledPackages(0)

        val mainIntent = Intent()
        if (isShortcut) {
            mainIntent.action = Intent.ACTION_CREATE_SHORTCUT
        } else {
            mainIntent.action = Intent.ACTION_MAIN
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        }

        packages.forEach {
            if (isShortcut && it.packageName == BuildConfig.APPLICATION_ID)
                return@forEach
            mainIntent.`package` = it.packageName
            val activityList = ctx.packageManager.queryIntentActivities(mainIntent, 0)
            appList += activityList
        }

        Collections.sort(appList, ResolveInfo.DisplayNameComparator(ctx.packageManager))

        val ais = appList.map {
            val appName = it.loadLabel(ctx.packageManager).toString()
            AppInfo(appName, it, ctx)
        }

        return ais
    }

    class AppInfo(val appName: String, val resolveInfo: ResolveInfo, private val ctx: Context) {
        val intent: Intent by lazy {
            val i = Intent(Intent.ACTION_MAIN)
            i.addCategory(Intent.CATEGORY_LAUNCHER)

            val cn = ComponentName(resolveInfo.activityInfo.packageName,
                    resolveInfo.activityInfo.name)
            i.component = cn
            i.putExtra("isShortcut", false)
        }

        val value: String by lazy { intent.toUri(0) }

        val appIcon: BitmapDrawable by lazy {
            val key = value
            var icon = appIconCache.get(key)
            if (icon == null) {
                var bitmap = drawableToBitmap(resolveInfo.loadIcon(ctx.packageManager))
                bitmap = Bitmap.createScaledBitmap(bitmap, appIconSizePx, appIconSizePx, false)
                icon = BitmapDrawable(ctx.resources, bitmap)
                appIconCache.put(key, icon)
            }
            icon
        }

        fun drawableToBitmap(drawable: Drawable?): Bitmap? {
            if (drawable == null) return null

            if (drawable is BitmapDrawable) {
                return drawable.bitmap
            }

            var width = drawable.intrinsicWidth
            width = if (width > 0) width else 1
            var height = drawable.intrinsicHeight
            height = if (height > 0) height else 1

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)

            return bitmap
        }
    }
}