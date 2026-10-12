package com.streak.app.domain.usecase

import com.streak.app.domain.model.Habit
import com.streak.app.domain.notify.ReminderScheduler
import com.streak.app.domain.repository.HabitRepository
import javax.inject.Inject

// Shrani navado (novo ali spremenjeno) in na novo razporedi njene opomnike.
// Stari alarmi se preklicejo, ker so se casi opomnikov morda spremenili.
class SaveHabitUseCase @Inject constructor(
    private val repository: HabitRepository,
    private val reminders: ReminderScheduler
) {
    suspend operator fun invoke(habit: Habit) {
        reminders.cancel(habit.id)
        repository.upsertHabit(habit)
        reminders.schedule(habit)
    }
}
