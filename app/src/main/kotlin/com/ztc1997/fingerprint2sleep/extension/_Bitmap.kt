package com.ztc1997.fingerprint2sleep.extension

import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

fun Bitmap.saveImage(path: File) {
    val parentFile = path.parentFile
    if (!parentFile.exists()) parentFile.mkdirs()
    if (path.exists()) path.delete()

    var out: OutputStream? = null
    try {
        out = FileOutputStream(path)
        compress(Bitmap.CompressFormat.JPEG, 90, out)
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        try {
            out?.close()
        } catch(e: Exception) {
        }
    }
}