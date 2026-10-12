package com.streak.app.domain.review

import com.streak.app.domain.repository.GameRepository
import com.streak.app.domain.repository.HabitRepository
import com.streak.app.domain.repository.MoodRepository
import com.streak.app.domain.time.DayClock
import kotlinx.coroutines.flow.first
import java.time.Instant
import javax.inject.Inject

// Zbere podatke za obdobje (tudi arhivirane navade) in izracuna pregled
class LoadReviewUseCase @Inject constructor(
    private val habitRepository: HabitRepository,
    private val moodRepository: MoodRepository,
    private val gameRepository: GameRepository,
    private val dayClock: DayClock
) {
    suspend operator fun invoke(range: ReviewRange): Review {
        val today = dayClock.today()
        val habits = habitRepository.getHabits(includeArchived = true)
        val input = ReviewInput(
            habits = habits,
            completions = habitRepository.getCompletionsBetween(range.start, range.end),
            aggregates = habitRepository.observeAggregates(range.start, range.end).first(),
            activity = gameRepository.activityBetween(range.start, range.end),
            sessions = gameRepository.sessionsBetween(range.start, range.end),
            moods = moodRepository.observeRange(range.start, range.end).first(),
            archivedOn = habits.mapNotNull { h -> h.archivedAt?.let { h.id to dayClock.dateOf(Instant.ofEpochMilli(it)) } }.toMap(),
            today = today
        )
        return ReviewCalculator.compute(range, input)
    }
}
