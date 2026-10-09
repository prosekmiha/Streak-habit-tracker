package com.streak.app.domain.usecase

import com.streak.app.domain.model.Frequency
import com.streak.app.domain.model.Habit
import com.streak.app.domain.model.HabitLog
import com.streak.app.domain.model.LogStatus
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

// Karakterizacijski testi OBSTOJECEGA izracuna streaka (pred refaktorjem).
// Zabelezijo trenutno vedenje, vkljucno z znanimi posebnostmi, da nov izracun
// lahko primerjamo s starim. Posebnosti so oznacene s "QUIRK".
class ComputeStreakUseCaseCharacterizationTest {

    private val useCase = ComputeStreakUseCase()
    private val today = LocalDate.of(2026, 10, 7) // sreda
    private lateinit var previousLocale: Locale

    @Before fun setUp() {
        previousLocale = Locale.getDefault()
        Locale.setDefault(Locale.UK) // teden se zacne v ponedeljek
    }

    @After fun tearDown() = Locale.setDefault(previousLocale)

    private fun habit(frequency: Frequency = Frequency.DAILY, targetPerWeek: Int = 7) = Habit(
        id = "h", title = "t", icon = "Star", colorHex = "#000000", frequency = frequency,
        targetPerWeek = targetPerWeek, reminderMinuteOfDay = null, reminderEnabled = false,
        isActive = true, createdAt = 0L, sortOrder = 0
    )

    private fun log(daysAgo: Long, status: LogStatus = LogStatus.DONE) = HabitLog(
        id = "l$daysAgo", habitId = "h", date = today.minusDays(daysAgo),
        status = status, completedAt = null, note = null
    )

    private fun daily(vararg logs: HabitLog) = useCase(habit(), logs.toList(), today)

    @Test fun `daily - no logs gives zero`() {
        val s = daily()
        assertEquals(0, s.currentStreak)
        assertEquals(0, s.bestStreak)
        assertEquals(0f, s.completionRate)
    }

    @Test fun `daily - done today and two previous days`() {
        val s = daily(log(0), log(1), log(2))
        assertEquals(3, s.currentStreak)
        assertEquals(3, s.bestStreak)
        assertEquals(3f / 30f, s.completionRate)
    }

    @Test fun `daily - today not yet done does not break the chain`() {
        val s = daily(log(1), log(2))
        assertEquals(2, s.currentStreak)
    }

    @Test fun `daily - skipped day is bridged and not counted`() {
        val s = daily(log(0), log(1, LogStatus.SKIPPED), log(2))
        assertEquals(2, s.currentStreak)
        assertEquals(2, s.bestStreak)
    }

    @Test fun `daily - missing record breaks current streak`() {
        val s = daily(log(0), log(2), log(3))
        assertEquals(1, s.currentStreak)
    }

    @Test fun `daily - missed record breaks current streak`() {
        val s = daily(log(0), log(1, LogStatus.MISSED), log(2), log(3))
        assertEquals(1, s.currentStreak)
        assertEquals(2, s.bestStreak)
    }

    @Test fun `daily QUIRK - best streak bridges gaps without records`() {
        // Med dnevi brez zapisa (ni MISSED) stari izracun steje verigo kot neprekinjeno.
        val s = daily(log(10), log(5))
        assertEquals(0, s.currentStreak)
        assertEquals(2, s.bestStreak)
    }

    @Test fun `daily - completion rate counts only last 30 days`() {
        val s = daily(log(0), log(29), log(30), log(31))
        assertEquals(2f / 30f, s.completionRate)
    }

    @Test fun `weekly - current week counted once target reached`() {
        // Pon 5.10., tor 6.10., sre 7.10. -> 3 v tem tednu
        val logs = listOf(log(0), log(1), log(2), /* prejsnji teden */ log(7), log(8), log(9))
        val s = useCase(habit(Frequency.WEEKLY, targetPerWeek = 3), logs, today)
        assertEquals(2, s.currentStreak)
        assertEquals(2, s.bestStreak)
    }

    @Test fun `weekly - unfinished current week does not break the chain`() {
        val logs = listOf(log(0), /* prejsnji teden */ log(7), log(8), log(9))
        val s = useCase(habit(Frequency.WEEKLY, targetPerWeek = 3), logs, today)
        assertEquals(1, s.currentStreak)
    }

    @Test fun `weekly - completion rate over last 4 weeks capped at target`() {
        val logs = listOf(log(0), log(1), log(2), log(3)) // 3 ta teden (+1 v nedeljo prejsnjega)
        val s = useCase(habit(Frequency.WEEKLY, targetPerWeek = 2), logs, today)
        // ta teden min(3,2)=2, prejsnji teden 1 -> 3 / (2*4)
        assertEquals(3f / 8f, s.completionRate)
    }
}
