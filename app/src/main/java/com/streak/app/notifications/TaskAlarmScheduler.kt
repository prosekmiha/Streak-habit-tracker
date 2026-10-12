package com.streak.app.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.streak.app.domain.model.Task
import com.streak.app.domain.notify.TaskReminderRules
import com.streak.app.domain.notify.TaskReminderScheduler
import com.streak.app.domain.repository.TaskRepository
import com.streak.app.domain.time.DayClock
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

// Opomniki nalog: en enkraten alarm na nalogo za najblizji sprozilec; ob sprozitvi
// TaskReminderDispatcher razporedi naslednjega.
@Singleton
class TaskAlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dayClock: DayClock,
    private val taskRepository: TaskRepository,
    private val helper: NotificationHelper
) : TaskReminderScheduler {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    override fun schedule(task: Task) {
        if (task.isDone) helper.cancelTaskReminder(task.id)
        val next = TaskReminderRules.nextTrigger(task, ZonedDateTime.now(dayClock.zone()))
        if (next == null) {
            alarmManager.cancel(intent(task.id, null))
        } else {
            alarmManager.setReminder(next.at.toInstant().toEpochMilli(), intent(task.id, next.occurrence.toEpochDay()))
        }
    }

    override fun cancel(taskId: String) {
        alarmManager.cancel(intent(taskId, null))
        helper.cancelTaskReminder(taskId)
    }

    override suspend fun rescheduleAll() {
        taskRepository.getTasks().forEach { schedule(it) }
    }

    // Isti requestCode in komponenta: nov alarm nadomesti starega (dodatki se posodobijo)
    private fun intent(taskId: String, occurrenceEpochDay: Long?): PendingIntent {
        val intent = Intent(context, TaskReminderReceiver::class.java)
            .putExtra(TaskReminderReceiver.EXTRA_TASK_ID, taskId)
        occurrenceEpochDay?.let { intent.putExtra(TaskReminderReceiver.EXTRA_OCCURRENCE, it) }
        return PendingIntent.getBroadcast(
            context, taskId.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
