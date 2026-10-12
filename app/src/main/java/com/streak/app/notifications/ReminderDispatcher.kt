package com.streak.app.notifications

import android.content.Context
import com.streak.app.R
import com.streak.app.domain.model.AppSettings
import com.streak.app.domain.model.CompletionStatus
import com.streak.app.domain.model.Habit
import com.streak.app.domain.notify.NotificationLog
import com.streak.app.domain.notify.NotificationRules
import com.streak.app.domain.notify.ReminderScheduler
import com.streak.app.domain.repository.HabitRepository
import com.streak.app.domain.repository.MoodRepository
import com.streak.app.domain.repository.SettingsRepository
import com.streak.app.domain.time.DayClock
import com.streak.app.domain.usecase.ToggleHabitUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

// Odloca, ali in kako se obvestilo pokaze: le za danes neopravljene navade, med tihimi
// urami brez zvoka (povzetek in vecerni opomnik se ne poslje), z dnevno omejitvijo.
@Singleton
class ReminderDispatcher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val habitRepository: HabitRepository,
    private val settingsRepository: SettingsRepository,
    private val dayClock: DayClock,
    private val logHabit: ToggleHabitUseCase,
    private val helper: NotificationHelper,
    private val scheduler: ReminderScheduler,
    private val log: NotificationLog,
    private val moodRepository: MoodRepository
) {
    suspend fun onReminder(habitId: String, snoozed: Boolean) {
        val habit = habitRepository.getHabit(habitId) ?: return
        // Alarmi so enkratni: razporedi naslednje pojavitve (odlog ne vpliva na urnik)
        if (!snoozed) scheduler.schedule(habit)
        if (habit.isArchived) return
        val item = habitRepository.observeHabit(habitId).first() ?: return
        if (!NotificationRules.needsReminder(item.todayState)) return
        val settings = settingsRepository.settings.first()
        if (!log.tryConsume(dayClock.today(), settings.dailyNotificationCap)) return
        helper.showReminder(item, settings.hideNamesInNotifications, silent = quiet(settings))
    }

    suspend fun onBriefing() {
        val settings = settingsRepository.settings.first()
        scheduler.scheduleDaily(settings)
        if (quiet(settings)) return
        val pending = NotificationRules.pendingToday(habitRepository.observeHabits().first())
        if (pending.isEmpty() || !log.tryConsume(dayClock.today(), settings.dailyNotificationCap)) return
        helper.showBriefing(context.resources.getQuantityString(R.plurals.notif_briefing_habits, pending.size, pending.size))
    }

    suspend fun onNudge() {
        val settings = settingsRepository.settings.first()
        scheduler.scheduleDaily(settings)
        if (quiet(settings)) return
        val habits = habitRepository.observeHabits().first()
        val pending = NotificationRules.pendingToday(habits)
        val today = dayClock.today()
        if (pending.isEmpty()) {
            // Vse opravljeno: povabi k vpisu razpolozenja, ce ga se ni
            val active = habits.any { !it.habit.isArchived }
            if (active && moodRepository.get(today) == null && log.tryConsume(today, settings.dailyNotificationCap)) helper.showMoodPrompt()
            return
        }
        if (!log.tryConsume(today, settings.dailyNotificationCap)) return
        helper.showNudge(nudgeText(pending, settings.hideNamesInNotifications))
    }

    private fun nudgeText(pending: List<Habit>, hideNames: Boolean): String = if (hideNames) {
        context.resources.getQuantityString(R.plurals.notif_nudge_count, pending.size, pending.size)
    } else {
        val (names, more) = NotificationRules.nudgeNames(pending)
        val list = names.joinToString(", ")
        if (more > 0) context.resources.getQuantityString(R.plurals.notif_nudge_names_more, more, list, more) else list
    }

    // "Opravljeno" iz obvestila
    suspend fun markDone(habitId: String) {
        val habit = habitRepository.getHabit(habitId) ?: return
        logHabit.setStatus(habit, dayClock.today(), CompletionStatus.DONE)
        helper.cancelReminder(habitId)
    }

    fun snooze(habitId: String) {
        scheduler.snooze(habitId)
        helper.cancelReminder(habitId)
    }

    // Ko je navada opravljena (kjerkoli), pospravi njen opomnik; prikazan vecerni opomnik
    // se tiho posodobi, ko je vse opravljeno, pa izgine
    suspend fun syncWithProgress() {
        habitRepository.observeHabits().collect { habits ->
            habits.filter { !NotificationRules.needsReminder(it.todayState) }.forEach { helper.cancelReminder(it.habit.id) }
            val pending = NotificationRules.pendingToday(habits)
            when {
                pending.isEmpty() -> helper.cancelNudge()
                helper.isNudgeShown() -> {
                    val hide = settingsRepository.settings.first().hideNamesInNotifications
                    helper.showNudge(nudgeText(pending, hide), silent = true)
                }
            }
        }
    }

    private fun quiet(settings: AppSettings): Boolean =
        NotificationRules.inQuietHours(settings.quietHoursStart, settings.quietHoursEnd, LocalTime.now(dayClock.zone()))
}
