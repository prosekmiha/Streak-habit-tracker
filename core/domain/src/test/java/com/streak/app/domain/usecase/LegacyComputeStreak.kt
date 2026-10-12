package com.streak.app.domain.usecase

import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale

// Kopija izracuna streaka PRED refaktorjem (M0), z lastnimi tipi.
// Sluzi kot referenca za karakterizacijske teste in primerjavo z novim izracunom.

enum class LegacyFrequency { DAILY, WEEKLY, CUSTOM }
enum class LegacyStatus { DONE, SKIPPED, MISSED }
data class LegacyHabit(val frequency: LegacyFrequency, val targetPerWeek: Int)
data class LegacyLog(val date: LocalDate, val status: LegacyStatus)
data class LegacyStreak(val currentStreak: Int, val bestStreak: Int, val completionRate: Float)

class LegacyComputeStreak {

    operator fun invoke(habit: LegacyHabit, logs: List<LegacyLog>, today: LocalDate = LocalDate.now()): LegacyStreak {
        return when (habit.frequency) {
            LegacyFrequency.DAILY -> dailyStreak(logs, today, windowDays = 30)
            LegacyFrequency.WEEKLY, LegacyFrequency.CUSTOM -> weeklyStreak(habit, logs, today)
        }
    }

    private fun dailyStreak(logs: List<LegacyLog>, today: LocalDate, windowDays: Int): LegacyStreak {
        val byDate = logs.associateBy { it.date }

        // Trenutni streak: stejemo nazaj od danes
        var current = 0
        var day = today
        // Ce danes se ni oznaceno, zacnemo od vceraj (danes se ne prekine verige)
        if (byDate[today]?.status != LegacyStatus.DONE) day = today.minusDays(1)
        while (true) {
            when (byDate[day]?.status) {
                LegacyStatus.DONE -> { current++; day = day.minusDays(1) }
                LegacyStatus.SKIPPED -> { day = day.minusDays(1) } // preskoci, ne prekine
                else -> break // MISSED ali brez zapisa
            }
        }

        // Najboljsi streak cez vse zapise
        var best = 0
        var run = 0
        val sorted = logs.sortedBy { it.date }
        var prev: LocalDate? = null
        for (log in sorted) {
            when (log.status) {
                LegacyStatus.DONE -> {
                    run = if (prev != null && contiguousIgnoringSkips(prev!!, log.date, byDate)) run + 1 else 1
                    if (run > best) best = run
                    prev = log.date
                }
                LegacyStatus.SKIPPED -> { prev = log.date }
                LegacyStatus.MISSED -> { run = 0; prev = log.date }
            }
        }
        if (current > best) best = current

        // Completion rate za zadnjih windowDays
        val windowStart = today.minusDays((windowDays - 1).toLong())
        val relevant = (0 until windowDays).map { windowStart.plusDays(it.toLong()) }
        val done = relevant.count { byDate[it]?.status == LegacyStatus.DONE }
        val rate = done.toFloat() / windowDays

        return LegacyStreak(current, best, rate)
    }

    // Ali sta dneva sosednja, ce vmesni dnevi niso MISSED (so SKIPPED ali DONE)
    private fun contiguousIgnoringSkips(prev: LocalDate, next: LocalDate, byDate: Map<LocalDate, LegacyLog>): Boolean {
        var d = prev.plusDays(1)
        while (d.isBefore(next)) {
            if (byDate[d]?.status == LegacyStatus.MISSED) return false
            d = d.plusDays(1)
        }
        return true
    }

    private fun weeklyStreak(habit: LegacyHabit, logs: List<LegacyLog>, today: LocalDate): LegacyStreak {
        val wf = WeekFields.of(Locale.getDefault())
        // Skupina DONE po tednu (leto-teden)
        fun weekKey(d: LocalDate): Pair<Int, Int> = d.get(wf.weekBasedYear()) to d.get(wf.weekOfWeekBasedYear())
        val doneByWeek = logs.filter { it.status == LegacyStatus.DONE }
            .groupingBy { weekKey(it.date) }
            .eachCount()

        val target = habit.targetPerWeek.coerceAtLeast(1)

        // Trenutni streak v tednih
        var current = 0
        var weekRef = today
        // Ce trenutni teden se ni dosegel cilja, ne prekini takoj
        if ((doneByWeek[weekKey(weekRef)] ?: 0) < target) weekRef = weekRef.minusWeeks(1)
        while ((doneByWeek[weekKey(weekRef)] ?: 0) >= target) {
            current++
            weekRef = weekRef.minusWeeks(1)
        }

        // Najboljsi streak v tednih
        val weeks = doneByWeek.keys.sortedWith(compareBy({ it.first }, { it.second }))
        var best = 0; var run = 0; var prevWeek: Pair<Int, Int>? = null
        for (w in weeks) {
            if ((doneByWeek[w] ?: 0) >= target) {
                run = if (prevWeek != null && isNextWeek(prevWeek!!, w)) run + 1 else 1
                if (run > best) best = run
            } else run = 0
            prevWeek = w
        }
        if (current > best) best = current

        // Completion za zadnjih ~4 tedne glede na cilj
        val last4 = (0 until 4).map { today.minusWeeks(it.toLong()) }.map { weekKey(it) }
        val achieved = last4.sumOf { (doneByWeek[it] ?: 0).coerceAtMost(target) }
        val rate = achieved.toFloat() / (target * 4)

        return LegacyStreak(current, best, rate.coerceIn(0f, 1f))
    }

    private fun isNextWeek(prev: Pair<Int, Int>, next: Pair<Int, Int>): Boolean {
        // Groba ocena: isto leto + zaporedni teden, ali prehod cez leto
        return (prev.first == next.first && next.second - prev.second == 1) ||
            (next.first - prev.first == 1 && prev.second >= 52 && next.second == 1)
    }
}
