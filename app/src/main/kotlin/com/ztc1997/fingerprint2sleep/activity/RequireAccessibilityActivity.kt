package com.ztc1997.fingerprint2sleep.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.ztc1997.fingerprint2sleep.R
import com.ztc1997.fingerprint2sleep.extension.finishWithoutAnim
import com.ztc1997.fingerprint2sleep.service.FPQAAccessibilityService
import com.ztc1997.fingerprint2sleep.service.FPQAService
import com.ztc1997.fingerprint2sleep.util.AccessibilityUtil
import org.jetbrains.anko.alert
import org.jetbrains.anko.startService
import org.jetbrains.anko.toast

class RequireAccessibilityActivity : Activity() {
    companion object {
        const val REQUEST_CODE_REQUIRE_ACCESSIBILITY = 0

        fun startActivity(context: Context) {
            val startIntent = Intent(context, RequireAccessibilityActivity::class.java)
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startIntent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            context.startActivity(startIntent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        alert(R.string.description_accessibility) {
            positiveButton(android.R.string.ok) {
                val intent = Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
                startActivityForResult(intent, REQUEST_CODE_REQUIRE_ACCESSIBILITY)
            }

            onCancel { finishWithoutAnim() }
            show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RequireAdminActivity.REQUEST_CODE_DEVICE_ADMIN)
            if (!AccessibilityUtil.isAccessibilityEnabled(this, FPQAAccessibilityService.ID))
                toast(R.string.toast_accessibility_failed)

        startService<FPQAService>()
        finishWithoutAnim()
    }
}