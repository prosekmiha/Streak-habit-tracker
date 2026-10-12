package com.streak.app.tiles

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.streak.app.R
import com.streak.app.di.ApplicationScope
import com.streak.app.domain.model.DayState
import com.streak.app.domain.model.GoalType
import com.streak.app.domain.repository.HabitRepository
import com.streak.app.domain.repository.SettingsRepository
import com.streak.app.domain.widget.WidgetHabit
import com.streak.app.domain.widget.WidgetSummary
import com.streak.app.ui.components.formatAmount
import com.streak.app.widget.WidgetActions
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

// Ploscica za izbrano navado: da/ne preklopi, kolicina +1. Na zaklenjenem zaslonu
// najprej zahteva odklep; z nastavitvijo "Skrij imena" je ime navade skrito.
@AndroidEntryPoint
class HabitTileService : TileService() {

    @Inject lateinit var habitRepository: HabitRepository
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var widgetActions: WidgetActions
    @Inject @ApplicationScope lateinit var scope: CoroutineScope

    override fun onStartListening() {
        super.onStartListening()
        scope.launch { render() }
    }

    override fun onClick() {
        super.onClick()
        if (isLocked && isSecure) unlockAndRun { handleClick() } else handleClick()
    }

    private fun handleClick() {
        scope.launch {
            val item = current()
            when (QuickTiles.habitClick(item)) {
                TileClick.LOG -> {
                    widgetActions.tap(item!!.habit.id)
                    render()
                }
                TileClick.CONFIGURE -> withContext(Dispatchers.Main) { openApp(mainIntent()) }
            }
        }
    }

    private suspend fun current(): WidgetHabit? {
        val id = settingsRepository.settings.first().tileHabitId ?: return null
        return habitRepository.observeHabits().first().firstOrNull { it.habit.id == id }?.let(WidgetSummary::habit)
    }

    private suspend fun render() {
        val item = current()
        val hide = settingsRepository.settings.first().hideNamesInNotifications
        withContext(Dispatchers.Main) {
            val tile = qsTile ?: return@withContext
            if (item == null || !QuickTiles.supportsHabitTile(item.habit)) {
                tile.label = getString(R.string.tile_habit_label)
                tile.state = Tile.STATE_INACTIVE
                setSubtitle(tile, getString(R.string.tile_choose_habit))
            } else {
                tile.label = if (hide) getString(R.string.tile_habit_label) else item.habit.title
                tile.state = if (item.isDone) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
                setSubtitle(tile, subtitle(item))
            }
            tile.updateTile()
        }
    }

    private fun subtitle(item: WidgetHabit): String = when {
        item.state == DayState.SKIPPED || item.state == DayState.FROZEN || item.state == DayState.VACATION -> getString(R.string.day_skipped)
        item.habit.goalType == GoalType.QUANTITY ->
            getString(R.string.habit_progress_quantity, formatAmount(item.value), formatAmount(item.habit.targetValue), item.habit.unit.orEmpty()).trim()
        item.isDone -> getString(R.string.state_done)
        else -> getString(R.string.state_not_done)
    }

    private fun setSubtitle(tile: Tile, text: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) tile.subtitle = text
    }
}
