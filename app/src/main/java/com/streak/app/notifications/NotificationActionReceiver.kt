package com.streak.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.streak.app.di.ApplicationScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

// Dejanja opomnikov: "Opravljeno" in "Cez 15 min" za navade, "Opravljeno" za naloge
@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    @Inject lateinit var dispatcher: ReminderDispatcher
    @Inject lateinit var taskDispatcher: TaskReminderDispatcher
    @Inject @ApplicationScope lateinit var scope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(if (intent.action == ACTION_TASK_DONE) EXTRA_TASK_ID else EXTRA_HABIT_ID) ?: return
        val pending = goAsync()
        scope.launch {
            try {
                when (intent.action) {
                    ACTION_DONE -> dispatcher.markDone(id, intent.getLongExtra(EXTRA_DAY, NO_DAY).takeIf { it != NO_DAY }?.let(LocalDate::ofEpochDay))
                    ACTION_SNOOZE -> dispatcher.snooze(id)
                    ACTION_TASK_DONE -> taskDispatcher.markDone(id)
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_DONE = "com.streak.app.reminder.DONE"
        const val ACTION_SNOOZE = "com.streak.app.reminder.SNOOZE"
        const val ACTION_TASK_DONE = "com.streak.app.reminder.TASK_DONE"
        const val EXTRA_HABIT_ID = "habit_id"
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_DAY = "epoch_day"
        private const val NO_DAY = Long.MIN_VALUE
    }
}
