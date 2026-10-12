package com.streak.app.domain.streak

import com.streak.app.domain.model.StreakUnit
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

// Pomozne funkcije za tedenska/mesecna obdobja z nastavljivim prvim dnem tedna
object Periods {

    fun start(date: LocalDate, unit: StreakUnit, firstDayOfWeek: DayOfWeek): LocalDate = when (unit) {
        StreakUnit.DAY -> date
        StreakUnit.WEEK -> date.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
        StreakUnit.MONTH -> date.withDayOfMonth(1)
    }

    fun end(periodStart: LocalDate, unit: StreakUnit): LocalDate = when (unit) {
        StreakUnit.DAY -> periodStart
        StreakUnit.WEEK -> periodStart.plusDays(6)
        StreakUnit.MONTH -> periodStart.withDayOfMonth(periodStart.lengthOfMonth())
    }

    fun next(periodStart: LocalDate, unit: StreakUnit): LocalDate = when (unit) {
        StreakUnit.DAY -> periodStart.plusDays(1)
        StreakUnit.WEEK -> periodStart.plusWeeks(1)
        StreakUnit.MONTH -> periodStart.plusMonths(1)
    }
}
