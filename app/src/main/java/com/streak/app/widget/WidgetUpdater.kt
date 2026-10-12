package com.streak.app.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.streak.app.domain.repository.HabitRepository
import com.streak.app.domain.repository.SettingsRepository
import com.streak.app.tiles.TileUpdater
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// Osvezi widgete ob vsaki spremembi podatkov ali nastavitev. Ob zagonu procesa (tudi ob
// alarmu za menjavo dneva) se osvezijo takoj, zato pokazejo pravi "danes".
@Singleton
class WidgetUpdater @Inject constructor(
    @ApplicationContext private val context: Context,
    private val habitRepository: HabitRepository,
    private val settingsRepository: SettingsRepository,
    private val tileUpdater: TileUpdater
) {
    suspend fun updateAll() {
        runCatching {
            ProgressWidget().updateAll(context)
            HabitsWidget().updateAll(context)
            StreakWidget().updateAll(context)
            QuantityWidget().updateAll(context)
        }
        // Ploscici v hitrih nastavitvah prikazujeta iste podatke
        tileUpdater.requestAll()
    }

    @OptIn(FlowPreview::class)
    suspend fun run() {
        combine(
            habitRepository.observeHabits(),
            settingsRepository.settings.map {
                listOf(it.themeMode, it.hideWidgetDetails, it.lastKnownDate, it.hideNamesInNotifications, it.tileHabitId, it.tileSessionHabitId)
            }.distinctUntilChanged()
        ) { _, _ -> Unit }
            .debounce(DEBOUNCE_MS)
            .collect { updateAll() }
    }

    private companion object {
        const val DEBOUNCE_MS = 300L
    }
}
