package com.streak.app.widget

import com.streak.app.domain.model.AppSettings
import com.streak.app.domain.model.ThemeMode
import com.streak.app.domain.repository.HabitRepository
import com.streak.app.domain.repository.SettingsRepository
import com.streak.app.domain.time.ClockSanity
import com.streak.app.domain.time.ClockStatus
import com.streak.app.domain.time.DayClock
import com.streak.app.domain.widget.TodaySummary
import com.streak.app.domain.widget.WidgetHabit
import com.streak.app.domain.widget.WidgetSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

// Videz in zasebnost widgetov iz nastavitev aplikacije
data class WidgetLook(
    // null = po sistemu, sicer tema, izbrana v aplikaciji
    val dark: Boolean?,
    val hideDetails: Boolean,
    // Ob skoku ure se vpis potrdi v aplikaciji, zato widget takrat le odpre aplikacijo
    val clockOk: Boolean
) {
    companion object {
        val Default = WidgetLook(dark = null, hideDetails = false, clockOk = true)
    }
}

data class TodayWidgetState(val summary: TodaySummary, val look: WidgetLook)

sealed interface HabitWidgetState {
    data object Loading : HabitWidgetState
    // Widget se ni nastavljen ali navada ni vec na voljo (izbrisana, arhivirana, drug tip cilja)
    data object NeedsHabit : HabitWidgetState
    data class Ready(val item: WidgetHabit, val look: WidgetLook) : HabitWidgetState
}

@Singleton
class WidgetRepository @Inject constructor(
    private val habitRepository: HabitRepository,
    private val settingsRepository: SettingsRepository,
    private val dayClock: DayClock
) {
    private val look: Flow<WidgetLook> =
        combine(settingsRepository.settings, dayClock.todayFlow) { s, today -> s.toLook(ClockSanity.check(s.lastKnownDate, today)) }
            .distinctUntilChanged()

    fun today(): Flow<TodayWidgetState> =
        combine(habitRepository.observeHabits(), look) { habits, look -> TodayWidgetState(WidgetSummary.today(habits), look) }
            .distinctUntilChanged()

    // Navada enega widgeta; `accept` doloci, ali tip navade ustreza widgetu
    fun habit(id: String?, accept: (WidgetHabit) -> Boolean = { true }): Flow<HabitWidgetState> =
        combine(habitRepository.observeHabits(), look) { habits, look ->
            val item = habits.firstOrNull { it.habit.id == id }?.let(WidgetSummary::habit)
            if (item == null || !accept(item)) HabitWidgetState.NeedsHabit else HabitWidgetState.Ready(item, look)
        }.distinctUntilChanged()

    private fun AppSettings.toLook(clock: ClockStatus) = WidgetLook(
        dark = when (themeMode) {
            ThemeMode.SYSTEM -> null
            ThemeMode.DARK -> true
            ThemeMode.LIGHT -> false
        },
        hideDetails = hideWidgetDetails,
        clockOk = clock == ClockStatus.Ok
    )
}
