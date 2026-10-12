package com.streak.app.ui.home

import com.streak.app.domain.model.DayState
import com.streak.app.domain.model.HabitWithStreak
import com.streak.app.domain.model.StreakUnit
import com.streak.app.domain.model.TimeOfDay
import com.streak.app.ui.components.HabitCardUi
import com.streak.app.ui.components.WeekDayStatus
import com.streak.app.ui.components.WeekDayUi
import java.time.LocalDate
import java.time.LocalTime

// Pretvorba domenskega stanja v model domacega zaslona (ciste funkcije, brez Androida)
internal object HomeUiMapping {
    // Okno tedenskega traku okoli danasnjega dne
    const val PAST_DAYS = 13L
    const val FUTURE_DAYS = 6L

    private val DayOrder = listOf(TimeOfDay.MORNING, TimeOfDay.AFTERNOON, TimeOfDay.EVENING, TimeOfDay.ANYTIME)

    // Izbran dan, omejen na okno traku (sicer danes)
    fun clampDate(selected: LocalDate?, today: LocalDate): LocalDate =
        selected?.takeIf { !it.isAfter(today.plusDays(FUTURE_DAYS)) && !it.isBefore(today.minusDays(PAST_DAYS)) } ?: today

    fun card(habit: HabitWithStreak, date: LocalDate): HabitCardUi? {
        val dayState = habit.stateOn(date) ?: return null
        val completion = habit.completionOn(date)
        // Navade izven urnika (npr. ob vikendih) pokazemo le, ce imajo zapis
        if (dayState == DayState.INACTIVE && completion == null) return null
        if (dayState == DayState.NOT_SCHEDULED && habit.habit.schedule.streakUnit == StreakUnit.DAY && completion == null) return null
        return HabitCardUi(
            habit = habit.habit,
            state = dayState,
            value = completion?.value ?: 0.0,
            currentStreak = habit.streak.currentStreak,
            bestStreak = habit.streak.bestStreak,
            unit = habit.streak.unit,
            note = completion?.note
        )
    }

    fun DayState.isDue() = this == DayState.DONE || this == DayState.PARTIAL ||
        this == DayState.PENDING || this == DayState.MISSED

    // Ena mreza kot v dizajnu, razvrscena po delu dneva (jutro, popoldne, vecer, kadarkoli).
    // Naslove razdelkov pokazemo le, ce ima vsaka skupina vsaj dve navadi - sicer bi mreza razpadla.
    fun sections(cards: List<HabitCardUi>): List<HabitSection> {
        if (cards.isEmpty()) return emptyList()
        val groups = DayOrder.mapNotNull { tod -> cards.filter { it.habit.timeOfDay == tod }.takeIf { it.isNotEmpty() }?.let { tod to it } }
        return if (groups.size > 1 && groups.all { it.second.size >= 2 }) groups.map { (tod, items) -> HabitSection(tod, items) }
        else listOf(HabitSection(null, groups.flatMap { it.second }))
    }

    fun weekDays(habits: List<HabitWithStreak>, today: LocalDate): List<WeekDayUi> =
        (-PAST_DAYS..FUTURE_DAYS).map { offset ->
            val date = today.plusDays(offset)
            val states = habits.mapNotNull { it.stateOn(date) }
            val due = states.count { it.isDue() }
            val done = states.count { it == DayState.DONE }
            val status = when {
                date.isAfter(today) -> WeekDayStatus.FUTURE
                due > 0 && done >= due -> WeekDayStatus.ALL_DONE
                done > 0 -> WeekDayStatus.SOME_DONE
                else -> WeekDayStatus.NONE
            }
            WeekDayUi(date, status, date == today)
        }

    // Najdaljsa trenutna dnevna veriga (za cip ob traku napredka)
    fun topDailyStreak(habits: List<HabitWithStreak>): Int =
        habits.filter { it.habit.schedule.streakUnit == StreakUnit.DAY }.maxOfOrNull { it.streak.currentStreak } ?: 0

    fun greetingFor(time: LocalTime): Greeting = when (time.hour) {
        in 5..11 -> Greeting.MORNING
        in 12..17 -> Greeting.DAY
        else -> Greeting.EVENING
    }
}
