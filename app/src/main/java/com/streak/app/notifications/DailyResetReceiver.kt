package com.streak.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.streak.app.work.DailyResetWorker
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

// Sprozeno z natancnim alarmom ob polnoci: obnovi alarm za naslednji dan in
// sprozi enkraten (ne periodicen) WorkManager job, ki opravi dejanski reset/briefing.
@AndroidEntryPoint
class DailyResetReceiver : BroadcastReceiver() {

    @Inject lateinit var alarmScheduler: AlarmScheduler

    override fun onReceive(context: Context, intent: Intent) {
        alarmScheduler.scheduleDailyReset()

        val request = OneTimeWorkRequestBuilder<DailyResetWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            DailyResetWorker.WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}
