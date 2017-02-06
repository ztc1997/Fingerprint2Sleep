package com.ztc1997.fingerprint2sleep.service

import android.app.job.JobParameters
import android.app.job.JobService
import com.ztc1997.fingerprint2sleep.activity.SettingsActivity
import com.ztc1997.fingerprint2sleep.activity.StartFPQAActivity
import com.ztc1997.fingerprint2sleep.defaultDPreference
import org.jetbrains.anko.ctx

class DaemonJobService : JobService() {
    companion object {
        const val ID = 1
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        return false
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        if (!FPQAService.isRunning &&
                defaultDPreference.getPrefBoolean(SettingsActivity.PREF_ENABLE_FINGERPRINT_QUICK_ACTION, false))
            StartFPQAActivity.startActivity(ctx)

        return false
    }
}