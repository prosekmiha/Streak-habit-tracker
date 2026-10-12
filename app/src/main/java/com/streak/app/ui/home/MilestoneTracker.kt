package com.streak.app.ui.home

import com.streak.app.domain.model.HabitWithStreak
import com.streak.app.domain.model.StreakUnit

// Ce je vpis uporabnika podaljsal verigo do mejnika, vrne praznovanje (enkrat na mejnik).
// Navada se oznaci ob vpisu, preveri pa ob naslednjem izracunu streakov.
internal class MilestoneTracker {
    private val pending = mutableMapOf<String, Int>()
    private val celebrated = mutableSetOf<String>()

    fun expect(habitId: String, streakBefore: Int) {
        pending[habitId] = streakBefore
    }

    fun check(habits: List<HabitWithStreak>): Celebration? {
        var result: Celebration? = null
        val iterator = pending.iterator()
        while (iterator.hasNext()) {
            val (id, before) = iterator.next()
            val h = habits.firstOrNull { it.habit.id == id } ?: run { iterator.remove(); continue }
            val now = h.streak.currentStreak
            if (now == before) continue
            iterator.remove()
            val milestones = if (h.streak.unit == StreakUnit.DAY) DAY_MILESTONES else PERIOD_MILESTONES
            if (now > before && now in milestones && celebrated.add("$id:$now")) {
                result = Celebration(h.habit.title, now, h.streak.unit)
            }
        }
        return result
    }

    private companion object {
        val DAY_MILESTONES = setOf(7, 30, 100, 365)
        val PERIOD_MILESTONES = setOf(4, 12, 26, 52)
    }
}
