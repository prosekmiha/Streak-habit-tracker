package com.streak.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.streak.app.di.ApplicationScope
import com.streak.app.domain.notify.ReminderScheduler
import com.streak.app.domain.session.SessionController
import com.streak.app.domain.usecase.TrackKnownDateUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

// Ponovno razporedi vse alarme, ko jih sistem pobrise ali ko se spremeni cas:
// zagon naprave, posodobitev aplikacije, sprememba ure ali casovnega pasu in
// sprememba dovoljenja za natancne alarme
@AndroidEntryPoint
class RescheduleReceiver : BroadcastReceiver() {

    @Inject lateinit var alarmScheduler: ReminderScheduler
    @Inject lateinit var sessionController: SessionController
    @Inject lateinit var trackKnownDate: TrackKnownDateUseCase
    @Inject @ApplicationScope lateinit var scope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in HANDLED) return
        val pending = goAsync()
        scope.launch {
            try {
                // Med izklopom naprave ure ni mogoce rocno premakniti: sprejmi nov dan
                if (intent.action == Intent.ACTION_BOOT_COMPLETED) trackKnownDate(afterReboot = true)
                alarmScheduler.rescheduleAll()
                // Tekoca seja: obnovi obvestilo in alarm za cilj
                sessionController.restore()
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        val HANDLED = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            // AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED (API 31+)
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED"
        )
    }
}
