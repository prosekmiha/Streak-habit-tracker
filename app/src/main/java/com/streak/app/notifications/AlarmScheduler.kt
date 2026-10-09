package com.streak.app.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.streak.app.domain.model.Habit
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

// Razporeja dnevne alarme za opomnike navad
@Singleton
class AlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun schedule(habit: Habit) {
        val minute = habit.reminderMinuteOfDay ?: return
        if (!habit.reminderEnabled) return

        val time = LocalTime.of(minute / 60, minute % 60)
        var next = LocalDateTime.of(LocalDate.now(), time)
        if (next.isBefore(LocalDateTime.now())) next = next.plusDays(1)
        val triggerAt = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val pi = reminderPendingIntent(habit.id, habit.title, escalationLevel = 0)

        // Dnevno ponavljanje
        if (canScheduleExact()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    // Escalation: ponovni alarm cez delayMinutes z visjim nivojem
    fun scheduleEscalation(habitId: String, habitTitle: String, escalationLevel: Int, delayMinutes: Long) {
        val triggerAt = System.currentTimeMillis() + delayMinutes * 60_000
        val pi = reminderPendingIntent(habitId, habitTitle, escalationLevel)
        if (canScheduleExact()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    fun cancel(habitId: String) {
        for (level in 0..2) {
            alarmManager.cancel(reminderPendingIntent(habitId, "", level))
        }
    }

    // Razporedi natancen alarm za dnevni reset/briefing ob polnoci.
    // WorkManager periodic work ni zanesljiv za tocen cas (Doze/App standby ga lahko
    // zamakne za ure), zato uporabimo isti exact-alarm mehanizem kot za opomnike.
    fun scheduleDailyReset() {
        val now = LocalDateTime.now()
        var next = LocalDateTime.of(now.toLocalDate().plusDays(1), LocalTime.MIDNIGHT)
        if (next.isBefore(now)) next = next.plusDays(1)
        val triggerAt = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val pi = dailyResetPendingIntent()
        if (canScheduleExact()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    private fun dailyResetPendingIntent(): PendingIntent {
        val intent = Intent(context, DailyResetReceiver::class.java)
        return PendingIntent.getBroadcast(
            context, REQUEST_CODE_DAILY_RESET, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun reminderPendingIntent(habitId: String, habitTitle: String, escalationLevel: Int): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_HABIT_ID, habitId)
            putExtra(ReminderReceiver.EXTRA_HABIT_TITLE, habitTitle)
            putExtra(ReminderReceiver.EXTRA_LEVEL, escalationLevel)
        }
        val requestCode = (habitId.hashCode() and 0xFFFFFF) + escalationLevel
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
        private const val REQUEST_CODE_DAILY_RESET = 0x5EED
    }
}
