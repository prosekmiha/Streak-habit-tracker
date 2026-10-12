package com.streak.app.domain.usecase

import com.streak.app.domain.notify.ReminderScheduler
import com.streak.app.domain.repository.HabitRepository
import javax.inject.Inject

// Izbrise navado z vsemi zapisi in preklice njene alarme
class DeleteHabitUseCase @Inject constructor(
    private val repository: HabitRepository,
    private val reminders: ReminderScheduler
) {
    suspend operator fun invoke(habitId: String) {
        reminders.cancel(habitId)
        repository.deleteHabit(habitId)
    }
}
