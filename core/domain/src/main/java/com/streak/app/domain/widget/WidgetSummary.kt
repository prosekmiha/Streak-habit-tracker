package com.streak.app.domain.widget

import com.streak.app.domain.model.DayState
import com.streak.app.domain.model.GoalType
import com.streak.app.domain.model.Habit
import com.streak.app.domain.model.HabitWithStreak
import com.streak.app.domain.model.StreakUnit
import com.streak.app.domain.model.TimeOfDay

// Kaj naredi dotik navade na widgetu (enako kot dotik kartice na domacem zaslonu).
// Dotik nikoli ne izbrise vec, kot ga naslednji dotik povrne (popoln izbris je v aplikaciji).
enum class WidgetTap {
    TOGGLE,      // da/ne: opravi ali odznaci
    ADD_ONE,     // kolicina: +1
    REMOVE_ONE,  // opravljena kolicina: -1 (ne izbrise celega vpisa)
    OPEN_SESSION, // trajanje: seja tece v aplikaciji
    OPEN_APP      // opuscanje: zdrs se vpise le v aplikaciji (ne po nesreci z widgeta)
}

// Razpolozenje maskote na widgetu napredka
enum class WidgetMood { NO_HABITS, REST_DAY, NOT_STARTED, IN_PROGRESS, ALL_DONE }

data class WidgetHabit(
    val habit: Habit,
    val state: DayState,
    val value: Double,
    val currentStreak: Int,
    val bestStreak: Int,
    val streakUnit: StreakUnit
) {
    val isDone: Boolean get() = state == DayState.DONE

    // Preskoceno, zamrznjeno ali dopust: za danes ni vec kaj storiti
    val isSettled: Boolean get() = state == DayState.SKIPPED || state == DayState.FROZEN || state == DayState.VACATION

    val progress: Float
        get() = if (habit.targetValue <= 0.0) 0f else (value / habit.targetValue).toFloat().coerceIn(0f, 1f)

    val tap: WidgetTap
        get() = when {
            habit.isQuit -> WidgetTap.OPEN_APP
            habit.goalType == GoalType.QUANTITY -> if (isDone) WidgetTap.REMOVE_ONE else WidgetTap.ADD_ONE
            // Opravljeno trajanje se ne odznaci (sicer bi izgubili cas sej): odpre se seja
            habit.goalType == GoalType.DURATION -> WidgetTap.OPEN_SESSION
            else -> WidgetTap.TOGGLE
        }
}

data class TodaySummary(
    val due: Int,
    val done: Int,
    // Najprej neopravljene (po delu dneva), nato opravljene in preskocene
    val items: List<WidgetHabit>,
    val hasHabits: Boolean
) {
    val remaining: Int get() = (due - done).coerceAtLeast(0)
    val progress: Float get() = if (due == 0) 0f else done.toFloat() / due

    val mood: WidgetMood
        get() = when {
            !hasHabits -> WidgetMood.NO_HABITS
            due == 0 -> WidgetMood.REST_DAY
            done >= due -> WidgetMood.ALL_DONE
            done == 0 -> WidgetMood.NOT_STARTED
            else -> WidgetMood.IN_PROGRESS
        }

    companion object {
        val Empty = TodaySummary(0, 0, emptyList(), hasHabits = false)
    }
}

object WidgetSummary {
    private val dayOrder = listOf(TimeOfDay.MORNING, TimeOfDay.AFTERNOON, TimeOfDay.EVENING, TimeOfDay.ANYTIME)

    fun today(habits: List<HabitWithStreak>): TodaySummary {
        val items = habits.mapNotNull { it.toWidgetHabit() }
        val due = items.count { it.state.isDue() }
        val done = items.count { it.isDone }
        val sorted = items.sortedWith(compareBy<WidgetHabit> { group(it) }.thenBy { dayOrder.indexOf(it.habit.timeOfDay) })
        return TodaySummary(due, done, sorted, hasHabits = habits.isNotEmpty())
    }

    fun habit(item: HabitWithStreak): WidgetHabit = WidgetHabit(
        habit = item.habit,
        state = item.todayState ?: DayState.NOT_SCHEDULED,
        value = item.completionOn(item.today)?.value ?: 0.0,
        currentStreak = item.streak.currentStreak,
        bestStreak = item.streak.bestStreak,
        streakUnit = item.streak.unit
    )

    // Ista pravila kot kartice na domacem zaslonu: navade izven urnika le, ce imajo zapis
    private fun HabitWithStreak.toWidgetHabit(): WidgetHabit? {
        val state = todayState ?: return null
        val hasRecord = completionOn(today) != null
        if (state == DayState.INACTIVE && !hasRecord) return null
        if (state == DayState.NOT_SCHEDULED && habit.schedule.streakUnit == StreakUnit.DAY && !hasRecord) return null
        return habit(this)
    }

    private fun group(item: WidgetHabit): Int = when {
        item.state == DayState.PENDING || item.state == DayState.PARTIAL -> 0
        item.state == DayState.NOT_SCHEDULED -> 1 // tedenske/mesecne navade, ki danes niso nujne
        item.isDone -> 2
        else -> 3
    }

    private fun DayState.isDue() = this == DayState.DONE || this == DayState.PARTIAL ||
        this == DayState.PENDING || this == DayState.MISSED
}
