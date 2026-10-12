package com.streak.app.domain.usecase

import com.streak.app.domain.model.CompletionStatus
import com.streak.app.testing.completion
import com.streak.app.testing.testHabit
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import kotlin.random.Random

// Primerja stari in novi izracun dnevnega streaka na nakljucnih zgodovinah.
// Pri polni zgodovini (vsak pretekli dan ima zapis DONE/SKIPPED/MISSED) se morata
// ujemati; staro posebnost (dnevi brez zapisa ne prekinejo rekorda) tako izkljucimo.
// MISSED v novem modelu ni shranjen - ustreza mu odsotnost zapisa.
class StreakEquivalenceTest {

    private val legacy = LegacyComputeStreak()
    private val current = ComputeStreakUseCase()
    private val today = LocalDate.of(2026, 10, 7)

    @Test fun `new daily streak matches legacy on complete histories`() {
        val random = Random(42)
        repeat(500) { run ->
            val days = random.nextInt(1, 120)
            val start = today.minusDays(days.toLong() - 1)
            val legacyLogs = ArrayList<LegacyLog>()
            val newCompletions = HashMap<LocalDate, com.streak.app.domain.model.Completion>()
            for (i in 0 until days) {
                val date = start.plusDays(i.toLong())
                val roll = random.nextInt(100)
                val status = when {
                    date == today && roll < 40 -> null // danes se neoznaceno
                    roll < 70 -> LegacyStatus.DONE
                    roll < 80 -> LegacyStatus.SKIPPED
                    else -> LegacyStatus.MISSED
                }
                status ?: continue
                legacyLogs += LegacyLog(date, status)
                when (status) {
                    LegacyStatus.DONE -> newCompletions[date] = completion(date)
                    LegacyStatus.SKIPPED -> newCompletions[date] = completion(date, CompletionStatus.SKIPPED)
                    LegacyStatus.MISSED -> Unit
                }
            }
            val old = legacy(LegacyHabit(LegacyFrequency.DAILY, 7), legacyLogs, today)
            val new = current(testHabit(start = start), newCompletions, emptyList(), today)
            assertEquals("current streak, run $run", old.currentStreak, new.currentStreak)
            assertEquals("best streak, run $run", old.bestStreak, new.bestStreak)
        }
    }
}
