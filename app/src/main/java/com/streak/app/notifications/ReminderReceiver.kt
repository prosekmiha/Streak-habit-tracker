package com.streak.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.streak.app.domain.model.CompletionStatus
import com.streak.app.domain.repository.HabitRepository
import com.streak.app.domain.time.DayClock
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

// Ob sprozenem alarmu preveri, ali je navada za danes ze opravljena (ali preskocena);
// ce ni, pokaze obvestilo in razporedi naslednjo stopnjo escalation. Pri osnovnem
// nivoju znova razporedi alarm za naslednji dan (alarmi so enkratni).
@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {

    @Inject lateinit var notificationHelper: NotificationHelper
    @Inject lateinit var alarmScheduler: AlarmScheduler
    @Inject lateinit var habitRepository: HabitRepository
    @Inject lateinit var dayClock: DayClock

    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getStringExtra(EXTRA_HABIT_ID) ?: return
        val reminderIndex = intent.getIntExtra(EXTRA_REMINDER_INDEX, 0)
        val level = intent.getIntExtra(EXTRA_LEVEL, 0)

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val habit = habitRepository.getHabit(habitId) ?: return@launch
                if (habit.isArchived) return@launch

                // Pri osnovnem nivoju obnovi dnevne alarme za naslednji dan
                if (level == 0) alarmScheduler.schedule(habit)

                val today = dayClock.today()
                if (!habit.isActiveOn(today)) return@launch
                val status = habitRepository.getCompletion(habitId, today)?.status
                if (status == CompletionStatus.DONE || status == CompletionStatus.SKIPPED || status == CompletionStatus.FROZEN) {
                    return@launch
                }

                val notifId = habitId.hashCode() and 0xFFFFFF
                notificationHelper.showReminder(notifId, habit.title, level)

                // Escalation: po 30 min ponovi z visjim nivojem
                if (level < AlarmScheduler.MAX_LEVEL) {
                    alarmScheduler.scheduleEscalation(habitId, habit.title, reminderIndex, level + 1, 30)
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val EXTRA_HABIT_ID = "habit_id"
        const val EXTRA_HABIT_TITLE = "habit_title"
        const val EXTRA_REMINDER_INDEX = "reminder_index"
        const val EXTRA_LEVEL = "level"
    }
}
