package com.streak.app.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.streak.app.domain.model.Habit
import com.streak.app.domain.repository.SettingsRepository
import com.streak.app.domain.time.DayClock
import com.streak.app.domain.time.LogicalDay
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.LocalTime
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

// Razporeja alarme za opomnike navad in za menjavo logicnega dne
@Singleton
class AlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dayClock: DayClock,
    private val settingsRepository: SettingsRepository
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    // Razporedi naslednjo pojavitev vsakega vklopljenega opomnika navade
    fun schedule(habit: Habit) {
        if (habit.isArchived) return
        habit.reminders.take(MAX_REMINDERS).forEachIndexed { index, reminder ->
            if (!reminder.enabled) return@forEachIndexed
            val pi = reminderPendingIntent(habit.id, habit.title, index, escalationLevel = 0)
            setAlarm(nextOccurrence(reminder.time), pi)
        }
    }

    // Escalation: ponovni alarm cez delayMinutes z visjim nivojem
    fun scheduleEscalation(habitId: String, habitTitle: String, reminderIndex: Int, escalationLevel: Int, delayMinutes: Long) {
        val triggerAt = System.currentTimeMillis() + delayMinutes * 60_000
        setAlarm(triggerAt, reminderPendingIntent(habitId, habitTitle, reminderIndex, escalationLevel))
    }

    // Preklice vse mozne opomnike navade (tudi ce se je stevilo opomnikov zmanjsalo)
    fun cancel(habit: Habit) = cancel(habit.id)

    fun cancel(habitId: String) {
        for (index in 0 until MAX_REMINDERS) {
            for (level in 0..MAX_LEVEL) {
                alarmManager.cancel(reminderPendingIntent(habitId, "", index, level))
            }
        }
    }

    // Natancen alarm ob meji logicnega dne ("dan se konca ob"). WorkManager periodic
    // work ni zanesljiv za tocen cas (Doze/App standby ga lahko zamakne za ure).
    suspend fun scheduleDailyReset() {
        val endsAt = settingsRepository.settings.first().dayEndsAt
        val next = LogicalDay.nextBoundary(dayClock.now(), dayClock.zone(), endsAt)
        setAlarm(next.toEpochMilli(), dailyResetPendingIntent())
    }

    private fun nextOccurrence(time: LocalTime): Long {
        val now = ZonedDateTime.now(dayClock.zone())
        var next = now.with(time).withSecond(0).withNano(0)
        if (!next.isAfter(now)) next = next.plusDays(1).with(time)
        return next.toInstant().toEpochMilli()
    }

    private fun setAlarm(triggerAt: Long, pi: PendingIntent) {
        if (canScheduleExact()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    private fun dailyResetPendingIntent(): PendingIntent {
        val intent = Intent(context, DailyResetReceiver::class.java)
        return PendingIntent.getBroadcast(
            context, REQUEST_CODE_DAILY_RESET, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun reminderPendingIntent(habitId: String, habitTitle: String, reminderIndex: Int, escalationLevel: Int): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_HABIT_ID, habitId)
            putExtra(ReminderReceiver.EXTRA_HABIT_TITLE, habitTitle)
            putExtra(ReminderReceiver.EXTRA_REMINDER_INDEX, reminderIndex)
            putExtra(ReminderReceiver.EXTRA_LEVEL, escalationLevel)
        }
        val slot = reminderIndex * (MAX_LEVEL + 1) + escalationLevel
        val requestCode = (habitId.hashCode() and 0xFFFFF) * SLOTS + slot
        return PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun canScheduleExact(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else true
    }

    companion object {
        const val MAX_REMINDERS = 8
        const val MAX_LEVEL = 2
        private const val SLOTS = 32 // >= MAX_REMINDERS * (MAX_LEVEL + 1)
        private const val REQUEST_CODE_DAILY_RESET = 0x5EED
    }
}
