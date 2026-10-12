package com.streak.app.domain.usecase

import com.streak.app.domain.model.Completion
import com.streak.app.domain.model.CompletionStatus
import com.streak.app.domain.model.GoalType
import com.streak.app.domain.model.Habit
import com.streak.app.domain.repository.HabitRepository
import com.streak.app.domain.time.DayClock
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

// NEEDS_LONG_PRESS: dotik bi izbrisal nekaj, cesar naslednji dotik ne povrne (zapisek,
// cas sej) - odznaci se le z dolgim pritiskom oz. iz menija dejanj
enum class LogResult { OK, FUTURE_BLOCKED, NEEDS_LONG_PRESS }

// Vpisi in spremembe stanja navade za logicni dan.
// - Prihodnjih dni ni mogoce vpisati (iskren zapis).
// - Vpis za pretekli dan se oznaci kot rocno popravljen (backfill).
class ToggleHabitUseCase @Inject constructor(
    private val repository: HabitRepository,
    private val dayClock: DayClock
) {
    // Preklopi med "opravljeno" in "brez zapisa". Pri opuscanju ne naredi nic:
    // cist dan nima zapisa, zdrs se vpise le z addRelapse (brez nenamernega tapa).
    suspend operator fun invoke(habit: Habit, date: LocalDate): LogResult {
        if (habit.isQuit) return LogResult.OK
        val existing = repository.getCompletion(habit.id, date)
        return if (existing?.status == CompletionStatus.DONE) {
            setStatus(habit, date, null)
        } else {
            val full = if (habit.goalType == GoalType.YES_NO) 1.0 else habit.targetValue
            write(habit, date, CompletionStatus.DONE, full, existing)
        }
    }

    // Dotik kartice ali dneva: da/ne preklopi, kolicina +1 (opravljena -1), trajanje se
    // oznaci s ciljem. Dotik nikoli ne izbrise vec, kot ga naslednji dotik povrne.
    suspend fun tap(habit: Habit, date: LocalDate): LogResult {
        if (habit.isQuit) return LogResult.OK
        if (date.isAfter(dayClock.today())) return LogResult.FUTURE_BLOCKED
        val existing = repository.getCompletion(habit.id, date)
        if (existing?.status != CompletionStatus.DONE) {
            return if (habit.goalType == GoalType.QUANTITY) addProgress(habit, date, 1.0) else invoke(habit, date)
        }
        val hasNote = !existing.note.isNullOrBlank()
        return when (habit.goalType) {
            // Cas sej se ne izbrise z dotikom
            GoalType.DURATION -> LogResult.NEEDS_LONG_PRESS
            GoalType.QUANTITY ->
                if (hasNote && existing.value <= 1.0) LogResult.NEEDS_LONG_PRESS else addProgress(habit, date, -1.0)
            else -> if (hasNote) LogResult.NEEDS_LONG_PRESS else setStatus(habit, date, null)
        }
    }

    // Preklop v seznamu s potrditvenimi polji (koledar): oznaci s ciljem ali odznaci, a vpisa
    // z zapiskom, kolicino ali casom ne izbrise (to le z dolgim pritiskom, tj. invoke)
    suspend fun toggleProtected(habit: Habit, date: LocalDate): LogResult {
        if (habit.isQuit) return LogResult.OK
        if (date.isAfter(dayClock.today())) return LogResult.FUTURE_BLOCKED
        val existing = repository.getCompletion(habit.id, date)
        val valuable = existing?.status == CompletionStatus.DONE &&
            (!existing.note.isNullOrBlank() || habit.goalType == GoalType.QUANTITY || habit.goalType == GoalType.DURATION)
        return if (valuable) LogResult.NEEDS_LONG_PRESS else invoke(habit, date)
    }

    // Nastavi stanje (null = odstrani zapis)
    suspend fun setStatus(habit: Habit, date: LocalDate, status: CompletionStatus?): LogResult {
        if (date.isAfter(dayClock.today())) return LogResult.FUTURE_BLOCKED
        val existing = repository.getCompletion(habit.id, date)
        if (status == null) {
            repository.deleteCompletion(habit.id, date)
            return LogResult.OK
        }
        val value = when (status) {
            CompletionStatus.DONE -> if (habit.goalType == GoalType.YES_NO) 1.0 else habit.targetValue
            CompletionStatus.PARTIAL -> existing?.value ?: 0.0
            CompletionStatus.SKIPPED, CompletionStatus.FROZEN -> 0.0
            CompletionStatus.RELAPSE -> (existing?.value?.takeIf { existing.status == CompletionStatus.RELAPSE } ?: 0.0)
                .coerceAtLeast(1.0)
        }
        return write(habit, date, status, value, existing)
    }

    // Pristeje (ali odsteje) kolicino oz. trajanje; stanje se izpelje iz cilja
    suspend fun addProgress(habit: Habit, date: LocalDate, delta: Double): LogResult {
        val existing = repository.getCompletion(habit.id, date)
        val base = if (existing?.status == CompletionStatus.DONE || existing?.status == CompletionStatus.PARTIAL) existing.value else 0.0
        val value = (base + delta).coerceAtLeast(0.0)
        if (value <= 0.0) return setStatus(habit, date, null)
        val status = if (habit.meetsGoal(value)) CompletionStatus.DONE else CompletionStatus.PARTIAL
        return write(habit, date, status, value, existing)
    }

    // Pristeje (ali odsteje) zdrs pri navadi za opuscanje; 0 zdrsov = cist dan (brez zapisa)
    suspend fun addRelapse(habit: Habit, date: LocalDate, delta: Int): LogResult {
        if (!habit.isQuit) return LogResult.OK
        val existing = repository.getCompletion(habit.id, date)
        val base = if (existing?.status == CompletionStatus.RELAPSE) existing.value else 0.0
        val value = (base + delta).coerceAtLeast(0.0)
        if (value <= 0.0) return setStatus(habit, date, null)
        return write(habit, date, CompletionStatus.RELAPSE, value, existing)
    }

    private suspend fun write(
        habit: Habit,
        date: LocalDate,
        status: CompletionStatus,
        value: Double,
        existing: Completion?
    ): LogResult {
        val today = dayClock.today()
        if (date.isAfter(today)) return LogResult.FUTURE_BLOCKED
        val now = dayClock.now()
        repository.saveCompletion(
            Completion(
                id = existing?.id ?: UUID.randomUUID().toString(),
                habitId = habit.id,
                date = date,
                status = status,
                value = value,
                loggedAt = now,
                zoneOffset = dayClock.zone().rules.getOffset(now),
                note = existing?.note,
                manuallyEdited = (existing?.manuallyEdited ?: false) || date != today
            )
        )
        return LogResult.OK
    }
}
