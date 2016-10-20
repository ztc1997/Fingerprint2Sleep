package com.ztc1997.fingerprint2sleep.activity

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.ztc1997.fingerprint2sleep.R
import com.ztc1997.fingerprint2sleep.extension.finishWithoutAnim
import com.ztc1997.fingerprint2sleep.receiver.AdminReceiver
import com.ztc1997.fingerprint2sleep.service.FPQAService
import org.jetbrains.anko.devicePolicyManager
import org.jetbrains.anko.startService
import org.jetbrains.anko.toast

class RequireAdminActivity : AppCompatActivity() {
    companion object {
        const val REQUEST_CODE_DEVICE_ADMIN = 0

        fun startActivity(context: Context) {
            val startIntent = Intent(context, RequireAdminActivity::class.java)
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startIntent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            context.startActivity(startIntent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val componentName = ComponentName(this, AdminReceiver::class.java)
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, getString(R.string.explanation_device_admin))

        startActivityForResult(intent, REQUEST_CODE_DEVICE_ADMIN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_DEVICE_ADMIN)
            if (resultCode == Activity.RESULT_OK) {
                devicePolicyManager.lockNow()
            } else {
                toast(R.string.toast_device_admin_failed)
                startService<FPQAService>()
            }

        finishWithoutAnim()
    }
}