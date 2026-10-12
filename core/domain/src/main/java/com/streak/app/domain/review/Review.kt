package com.streak.app.domain.review

import com.streak.app.domain.gamification.DayActivity
import com.streak.app.domain.gamification.DaySessions
import com.streak.app.domain.model.Completion
import com.streak.app.domain.model.CompletionStatus
import com.streak.app.domain.model.DailyAggregate
import com.streak.app.domain.model.Habit
import com.streak.app.domain.model.MoodEntry
import com.streak.app.domain.model.DayState
import com.streak.app.domain.streak.DayEvaluator
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth

enum class ReviewPeriod { MONTH, YEAR }

// Obdobje pregleda: koledarski mesec ali leto
data class ReviewRange(val period: ReviewPeriod, val start: LocalDate, val end: LocalDate) {
    fun previous(): ReviewRange = when (period) {
        ReviewPeriod.MONTH -> month(YearMonth.from(start).minusMonths(1))
        ReviewPeriod.YEAR -> year(Year.from(start).minusYears(1))
    }

    fun next(): ReviewRange = when (period) {
        ReviewPeriod.MONTH -> month(YearMonth.from(start).plusMonths(1))
        ReviewPeriod.YEAR -> year(Year.from(start).plusYears(1))
    }

    fun contains(date: LocalDate): Boolean = !date.isBefore(start) && !date.isAfter(end)

    companion object {
        fun month(m: YearMonth) = ReviewRange(ReviewPeriod.MONTH, m.atDay(1), m.atEndOfMonth())
        fun year(y: Year) = ReviewRange(ReviewPeriod.YEAR, y.atDay(1), y.atMonth(12).atEndOfMonth())

        fun of(period: ReviewPeriod, date: LocalDate) = when (period) {
            ReviewPeriod.MONTH -> month(YearMonth.from(date))
            ReviewPeriod.YEAR -> year(Year.from(date))
        }
    }
}

// Kdaj v dnevu uporabnik najpogosteje opravi navade (iz vpisov na isti dan)
enum class Chronotype { EARLY_BIRD, DAYTIME, NIGHT_OWL }

data class Review(
    val range: ReviewRange,
    // Opravljene navade in cisti dnevi opuscanja
    val doneCount: Int,
    val cleanDays: Int,
    val activeDays: Int,
    val perfectDays: Int,
    val longestPerfectRun: Int,
    // Navada z najvec opravljenimi dnevi v obdobju
    val topHabit: Habit?,
    val topHabitDays: Int,
    val bestWeekday: DayOfWeek?,
    val chronotype: Chronotype?,
    val sessionCount: Int,
    val sessionSeconds: Long,
    val moodAverage: Double?,
    val moodEntries: Int
) {
    val isEmpty: Boolean get() = doneCount == 0 && sessionCount == 0 && moodEntries == 0
}

// Vsi podatki za izracun (zbere jih LoadReviewUseCase)
data class ReviewInput(
    val habits: List<Habit>,
    val completions: List<Completion>,
    val aggregates: List<DailyAggregate>,
    val activity: List<DayActivity>,
    val sessions: List<DaySessions>,
    val moods: List<MoodEntry>,
    // Logicni dan arhiviranja (od njega naprej se navada v pregledu ne steje, kot v povzetkih)
    val archivedOn: Map<String, LocalDate>,
    val today: LocalDate
)

object ReviewCalculator {
    // Dnevi brez vpisov se pri kronotipu ne stejejo; potrebnih je vsaj nekaj vpisov
    const val MIN_CHRONOTYPE_LOGS = 5

    fun compute(range: ReviewRange, input: ReviewInput): Review {
        val last = minOf(range.end, input.today)
        val inRange = { d: LocalDate -> !d.isBefore(range.start) && !d.isAfter(last) }
        val completions = input.completions.filter { inRange(it.date) }
        val aggregates = input.aggregates.filter { inRange(it.date) }.sortedBy { it.date }
        val activity = input.activity.filter { inRange(it.date) }
        val sessions = input.sessions.filter { inRange(it.date) }
        val moods = input.moods.filter { inRange(it.date) }

        val quitIds = input.habits.filter { it.isQuit }.map { it.id }.toSet()
        val done = completions.filter { it.status == CompletionStatus.DONE && it.habitId !in quitIds }
        val cleanDays = input.habits.filter { it.isQuit }.sumOf { h ->
            val end = input.archivedOn[h.id]?.minusDays(1)?.let { minOf(last, it) } ?: last
            cleanDays(h, completions, range.start, end, input.today)
        }

        val top = done.groupBy { it.habitId }
            .mapValues { (_, list) -> list.map { it.date }.distinct().size }
            .maxWithOrNull(compareBy<Map.Entry<String, Int>> { it.value }.thenByDescending { it.key })
        val topHabit = top?.let { e -> input.habits.firstOrNull { it.id == e.key } }

        return Review(
            range = range,
            doneCount = done.size + cleanDays,
            cleanDays = cleanDays,
            activeDays = aggregates.count { it.done > 0 },
            perfectDays = aggregates.count(::isPerfect),
            longestPerfectRun = longestRun(aggregates),
            topHabit = topHabit,
            topHabitDays = if (topHabit != null) top.value else 0,
            bestWeekday = bestWeekday(aggregates),
            chronotype = chronotype(activity),
            sessionCount = sessions.sumOf { it.count },
            sessionSeconds = sessions.sumOf { it.seconds },
            moodAverage = moods.takeIf { it.isNotEmpty() }?.map { it.mood }?.average(),
            moodEntries = moods.size
        )
    }

    private fun isPerfect(a: DailyAggregate) = a.scheduled > 0 && a.done >= a.scheduled

    // Najdaljse zaporedje popolnih dni (dan brez nacrtovanih navad zaporedje prekine)
    private fun longestRun(sorted: List<DailyAggregate>): Int {
        var best = 0
        var run = 0
        var prev: LocalDate? = null
        for (a in sorted) {
            run = if (isPerfect(a)) (if (prev != null && a.date == prev.plusDays(1)) run + 1 else 1) else 0
            if (run > 0) prev = a.date else prev = null
            if (run > best) best = run
        }
        return best
    }

    // Dan v tednu z najvisjim delezem opravljenih navad
    private fun bestWeekday(aggregates: List<DailyAggregate>): DayOfWeek? =
        aggregates.filter { it.scheduled > 0 }
            .groupBy { it.date.dayOfWeek }
            .mapValues { (_, list) -> list.sumOf { it.done }.toDouble() / list.sumOf { it.scheduled } }
            .filterValues { it > 0.0 }
            .maxWithOrNull(compareBy<Map.Entry<DayOfWeek, Double>> { it.value }.thenByDescending { it.key.value })
            ?.key

    private fun chronotype(activity: List<DayActivity>): Chronotype? {
        val total = activity.sumOf { it.done }
        if (total < MIN_CHRONOTYPE_LOGS) return null
        val before9 = activity.sumOf { it.doneBefore9 }.toDouble() / total
        val before18 = activity.sumOf { it.doneBefore18 }.toDouble() / total
        return when {
            before9 >= 0.5 -> Chronotype.EARLY_BIRD
            before18 < 0.5 -> Chronotype.NIGHT_OWL
            else -> Chronotype.DAYTIME
        }
    }

    // Cisti dnevi navade za opuscanje v obdobju: enako kot pri streaku (zdrs prekine,
    // preskok in zamrznitev sta nevtralna)
    private fun cleanDays(habit: Habit, completions: List<Completion>, start: LocalDate, end: LocalDate, today: LocalDate): Int {
        val byDate = completions.filter { it.habitId == habit.id }.associateBy { it.date }
        var count = 0
        var day = maxOf(start, habit.startDate)
        while (!day.isAfter(end)) {
            if (DayEvaluator.stateOn(habit, day, byDate[day], emptyList(), today) == DayState.DONE) count++
            day = day.plusDays(1)
        }
        return count
    }
}
