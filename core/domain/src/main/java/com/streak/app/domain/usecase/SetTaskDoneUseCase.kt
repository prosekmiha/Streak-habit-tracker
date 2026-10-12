package com.streak.app.domain.usecase

import com.streak.app.domain.model.Task
import com.streak.app.domain.notify.TaskReminderScheduler
import com.streak.app.domain.repository.TaskRepository
import com.streak.app.domain.time.DayClock
import javax.inject.Inject

// Odkljuka ali vrne nalogo; opravljena nima vec opomnikov.
// Ponavljajoca naloga se ob odkljukanju ne zakljuci, ampak premakne na naslednji termin.
class SetTaskDoneUseCase @Inject constructor(
    private val repository: TaskRepository,
    private val reminders: TaskReminderScheduler,
    private val dayClock: DayClock
) {
    suspend operator fun invoke(task: Task, done: Boolean) {
        if (done && task.isRecurring && task.dueDate != null) {
            val due = task.dueDate
            // Opravljen je tekoci termin (zamujen ali danasnji): naslednji je po danes oz. po roku
            val next = task.nextOccurrence(maxOf(due, dayClock.today()).plusDays(1)) ?: due
            val advanced = task.copy(dueDate = next, isDone = false)
            repository.upsertTask(advanced)
            // Pospravi obvestilo opravljenega termina in razporedi opomnik naslednjega
            reminders.cancel(task.id)
            reminders.schedule(advanced)
            return
        }
        repository.setDone(task.id, done)
        reminders.schedule(task.copy(isDone = done))
    }
}
