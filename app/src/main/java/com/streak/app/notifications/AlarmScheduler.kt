package com.streak.app.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.streak.app.domain.model.AppSettings
import com.streak.app.domain.model.Habit
import com.streak.app.domain.notify.NotificationRules
import com.streak.app.domain.repository.HabitRepository
import com.streak.app.domain.repository.SettingsRepository
import com.streak.app.domain.time.DayClock
import com.streak.app.domain.time.LogicalDay
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.LocalTime
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

// Razporeja vse alarme: opomnike navad (vec na navado), odlog, jutranji povzetek,
// vecerni opomnik in menjavo logicnega dne. Alarmi so enkratni; ob sprozitvi se
// razporedi naslednji. Natancni so, ce uporabnik to dovoli (Android 12+).
@Singleton
class AlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dayClock: DayClock,
    private val settingsRepository: SettingsRepository,
    private val habitRepository: HabitRepository
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    // Po zagonu naprave, posodobitvi, spremembi ure/pasu ali dovoljenja
    suspend fun rescheduleAll() {
        val settings = settingsRepository.settings.first()
        scheduleDailyReset(settings)
        scheduleDaily(settings)
        habitRepository.getHabits().forEach { schedule(it) }
    }

    // Naslednja pojavitev vsakega vklopljenega opomnika navade
    fun schedule(habit: Habit) {
        cancel(habit.id, includeSnooze = false)
        if (habit.isArchived) return
        habit.reminders.take(MAX_REMINDERS).forEachIndexed { index, reminder ->
            if (reminder.enabled) setAlarm(next(reminder.time), reminderIntent(habit.id, index))
        }
    }

    fun cancel(habit: Habit) = cancel(habit.id)

    fun cancel(habitId: String, includeSnooze: Boolean = true) {
        for (index in 0 until MAX_REMINDERS) alarmManager.cancel(reminderIntent(habitId, index))
        if (includeSnooze) alarmManager.cancel(reminderIntent(habitId, SNOOZE_SLOT))
    }

    // "Cez 15 min" iz obvestila
    fun snooze(habitId: String, minutes: Long = SNOOZE_MINUTES) {
        setAlarm(System.currentTimeMillis() + minutes * 60_000, reminderIntent(habitId, SNOOZE_SLOT))
    }

    // Natancen alarm ob meji logicnega dne ("dan se konca ob")
    suspend fun scheduleDailyReset(settings: AppSettings? = null) {
        val endsAt = (settings ?: settingsRepository.settings.first()).dayEndsAt
        val next = LogicalDay.nextBoundary(dayClock.now(), dayClock.zone(), endsAt)
        setAlarm(next.toEpochMilli(), dailyResetIntent())
    }

    // Jutranji povzetek in vecerni opomnik ob nastavljenih urah
    suspend fun scheduleDaily(settings: AppSettings? = null) {
        val s = settings ?: settingsRepository.settings.first()
        val briefing = dailyIntent(DailyNotificationReceiver.ACTION_BRIEFING)
        val nudge = dailyIntent(DailyNotificationReceiver.ACTION_NUDGE)
        if (s.briefingEnabled) setAlarm(next(s.briefingTime), briefing) else alarmManager.cancel(briefing)
        if (s.nudgeEnabled) setAlarm(next(s.nudgeTime), nudge) else alarmManager.cancel(nudge)
    }

    private fun next(time: LocalTime): Long =
        NotificationRules.nextOccurrence(ZonedDateTime.now(dayClock.zone()), time).toInstant().toEpochMilli()

    private fun setAlarm(triggerAt: Long, pi: PendingIntent) {
        if (canScheduleExact()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } else {
            // Brez dovoljenja: sistem lahko alarm zamakne (v Doze do nekaj minut)
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    private fun dailyResetIntent(): PendingIntent = PendingIntent.getBroadcast(
        context, REQUEST_DAILY_RESET, Intent(context, DailyResetReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun dailyIntent(action: String): PendingIntent = PendingIntent.getBroadcast(
        context, if (action == DailyNotificationReceiver.ACTION_BRIEFING) REQUEST_BRIEFING else REQUEST_NUDGE,
        Intent(context, DailyNotificationReceiver::class.java).setAction(action),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun reminderIntent(habitId: String, slot: Int): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
            .putExtra(ReminderReceiver.EXTRA_HABIT_ID, habitId)
            .putExtra(ReminderReceiver.EXTRA_SNOOZE, slot == SNOOZE_SLOT)
        val requestCode = (habitId.hashCode() and 0xFFFFF) * SLOTS + slot
        return PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val MAX_REMINDERS = 8
        const val SNOOZE_MINUTES = 15L
        private const val SNOOZE_SLOT = MAX_REMINDERS
        private const val SLOTS = 16 // > MAX_REMINDERS + odlog
        private const val REQUEST_DAILY_RESET = 0x5EED
        private const val REQUEST_BRIEFING = 0x5EEE
        private const val REQUEST_NUDGE = 0x5EEF
    }
}
