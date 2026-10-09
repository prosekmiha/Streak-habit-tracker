package com.streak.app.domain.usecase

import com.streak.app.domain.model.LogStatus
import com.streak.app.domain.repository.HabitRepository
import java.time.LocalDate
import javax.inject.Inject

// Preklopi stanje navade za podan dan med DONE in brez zapisa
class ToggleHabitUseCase @Inject constructor(
    private val repository: HabitRepository
) {
    suspend operator fun invoke(habitId: String, date: LocalDate, currentlyDone: Boolean) {
        if (currentlyDone) {
            repository.setLogStatus(habitId, date, null) // odstrani zapis
        } else {
            repository.setLogStatus(habitId, date, LogStatus.DONE)
        }
    }

    suspend fun setStatus(habitId: String, date: LocalDate, status: LogStatus?) {
        repository.setLogStatus(habitId, date, status)
    }
}
