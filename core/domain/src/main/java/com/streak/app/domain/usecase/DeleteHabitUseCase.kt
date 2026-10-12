package com.streak.app.domain.usecase

import com.streak.app.domain.notify.ReminderScheduler
import com.streak.app.domain.repository.HabitRepository
import com.streak.app.domain.session.SessionController
import javax.inject.Inject

// Izbrise navado z vsemi zapisi in preklice njene alarme. Tekoca seja te navade se
// zavrze (brez navade je ni mogoce shraniti in bi ostala za vedno).
class DeleteHabitUseCase @Inject constructor(
    private val repository: HabitRepository,
    private val reminders: ReminderScheduler,
    private val sessions: SessionController
) {
    suspend operator fun invoke(habitId: String) {
        sessions.awaitLoaded()
        if (sessions.active.value?.habitId == habitId) sessions.finish(save = false)
        reminders.cancel(habitId)
        repository.deleteHabit(habitId)
    }
}
