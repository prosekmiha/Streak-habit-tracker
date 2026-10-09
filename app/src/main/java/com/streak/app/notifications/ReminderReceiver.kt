package com.streak.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.streak.app.data.local.dao.HabitDao
import com.streak.app.data.local.dao.HabitLogDao
import com.streak.app.data.mapper.toDomain
import com.streak.app.domain.model.LogStatus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

// Ob sprozenem alarmu preveri, ali je navada za danes ze DONE; ce ni, pokaze obvestilo
// in razporedi naslednjo stopnjo escalation. Pri osnovnem nivoju znova razporedi alarm
// za naslednji dan (alarmi so enkratni in jih je treba obnoviti).
@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {

    @Inject lateinit var notificationHelper: NotificationHelper
    @Inject lateinit var alarmScheduler: AlarmScheduler
    @Inject lateinit var habitDao: HabitDao
    @Inject lateinit var logDao: HabitLogDao

    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getStringExtra(EXTRA_HABIT_ID) ?: return
        val habitTitle = intent.getStringExtra(EXTRA_HABIT_TITLE) ?: ""
        val level = intent.getIntExtra(EXTRA_LEVEL, 0)

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Pri osnovnem nivoju obnovi dnevni alarm za naslednji dan
                if (level == 0) {
                    habitDao.getHabit(habitId)?.toDomain()?.let { habit ->
                        if (habit.reminderEnabled) alarmScheduler.schedule(habit)
                    }
                }

                val today = LocalDate.now().toEpochDay()
                val alreadyDone = logDao.getForDay(habitId, today)?.status == LogStatus.DONE
                if (alreadyDone) return@launch

                val notifId = habitId.hashCode() and 0xFFFFFF
                notificationHelper.showReminder(notifId, habitTitle, level)

                // Escalation: po 30 min ponovi z visjim nivojem (najvec do 2)
                if (level < 2) {
                    alarmScheduler.scheduleEscalation(habitId, habitTitle, level + 1, 30)
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val EXTRA_HABIT_ID = "habit_id"
        const val EXTRA_HABIT_TITLE = "habit_title"
        const val EXTRA_LEVEL = "level"
    }
}
