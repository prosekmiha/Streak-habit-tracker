package com.streak.app.notifications

import com.streak.app.domain.model.AppSettings
import com.streak.app.domain.notify.NotificationRules
import com.streak.app.domain.notify.TaskReminderScheduler
import com.streak.app.domain.repository.SettingsRepository
import com.streak.app.domain.repository.TaskRepository
import com.streak.app.domain.time.DayClock
import com.streak.app.domain.usecase.SetTaskDoneUseCase
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

// Opomnik naloge: le za neopravljeno nalogo, med tihimi urami brez zvoka. Dnevna omejitev
// obvestil zanj ne velja, ker ga je uporabnik izrecno nastavil.
@Singleton
class TaskReminderDispatcher @Inject constructor(
    private val taskRepository: TaskRepository,
    private val settingsRepository: SettingsRepository,
    private val dayClock: DayClock,
    private val scheduler: TaskReminderScheduler,
    private val setTaskDone: SetTaskDoneUseCase,
    private val helper: NotificationHelper
) {
    suspend fun onReminder(taskId: String, occurrence: LocalDate?) {
        val task = taskRepository.getTask(taskId) ?: return
        // Alarm je enkraten: razporedi naslednji sprozilec
        scheduler.schedule(task)
        if (task.isDone) return
        val settings = settingsRepository.settings.first()
        helper.showTaskReminder(task, occurrence ?: dayClock.today(), dayClock.today(), settings.hideNamesInNotifications, quiet(settings))
    }

    // "Opravljeno" iz obvestila
    suspend fun markDone(taskId: String) {
        val task = taskRepository.getTask(taskId) ?: return
        setTaskDone(task, done = true)
    }

    private fun quiet(settings: AppSettings): Boolean =
        NotificationRules.inQuietHours(settings.quietHoursStart, settings.quietHoursEnd, LocalTime.now(dayClock.zone()))
}
