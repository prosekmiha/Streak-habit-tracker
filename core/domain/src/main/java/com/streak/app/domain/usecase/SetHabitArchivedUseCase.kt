package com.streak.app.domain.usecase

import com.streak.app.domain.notify.ReminderScheduler
import com.streak.app.domain.repository.HabitRepository
import javax.inject.Inject

// Arhivirana navada nima opomnikov; ob vrnitvi iz arhiva se opomniki znova razporedijo
class SetHabitArchivedUseCase @Inject constructor(
    private val repository: HabitRepository,
    private val reminders: ReminderScheduler
) {
    suspend operator fun invoke(habitId: String, archived: Boolean) {
        if (archived) reminders.cancel(habitId)
        repository.setArchived(habitId, archived)
        if (!archived) repository.getHabit(habitId)?.let(reminders::schedule)
    }
}
