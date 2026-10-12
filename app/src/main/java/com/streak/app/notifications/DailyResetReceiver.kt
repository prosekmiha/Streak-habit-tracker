package com.streak.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.streak.app.domain.notify.ReminderScheduler
import com.streak.app.work.DailyResetWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

// Sprozeno z natancnim alarmom ob meji logicnega dne: obnovi alarm za naslednji dan
// in sprozi enkraten WorkManager job, ki opravi dejanski preracun/briefing.
@AndroidEntryPoint
class DailyResetReceiver : BroadcastReceiver() {

    @Inject lateinit var alarmScheduler: ReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val request = OneTimeWorkRequestBuilder<DailyResetWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            DailyResetWorker.WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                alarmScheduler.scheduleDailyReset()
            } finally {
                pending.finish()
            }
        }
    }
}
