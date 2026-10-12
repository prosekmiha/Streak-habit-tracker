package com.streak.app.tiles

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.streak.app.MainActivity
import com.streak.app.R
import com.streak.app.di.ApplicationScope
import com.streak.app.domain.repository.HabitRepository
import com.streak.app.domain.repository.SettingsRepository
import com.streak.app.domain.session.SessionController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

// Ploscica za sejo: med tekoco sejo jo ustavi/nadaljuje, sicer odpre sejo izbrane
// casovne navade (zacetek je v aplikaciji, kjer je izbira zvoka in opozorilo ob sporu).
@AndroidEntryPoint
class SessionTileService : TileService() {

    @Inject lateinit var sessionController: SessionController
    @Inject lateinit var habitRepository: HabitRepository
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject @ApplicationScope lateinit var scope: CoroutineScope

    override fun onStartListening() {
        super.onStartListening()
        scope.launch { render() }
    }

    override fun onClick() {
        super.onClick()
        scope.launch {
            sessionController.awaitLoaded()
            if (sessionController.active.value != null) {
                sessionController.togglePause()
                render()
                return@launch
            }
            val habitId = configuredHabitId()
            withContext(Dispatchers.Main) {
                val open = { openApp(if (habitId != null) mainIntent(MainActivity.ACTION_START_SESSION, habitId) else mainIntent()) }
                if (isLocked && isSecure) unlockAndRun(open) else open()
            }
        }
    }

    private suspend fun configuredHabitId(): String? {
        val id = settingsRepository.settings.first().tileSessionHabitId ?: return null
        return habitRepository.getHabit(id)?.takeIf { !it.isArchived && QuickTiles.supportsSessionTile(it) }?.id
    }

    private suspend fun render() {
        sessionController.awaitLoaded()
        val session = sessionController.active.value
        val hide = settingsRepository.settings.first().hideNamesInNotifications
        val configured = configuredHabitId()?.let { habitRepository.getHabit(it) }
        withContext(Dispatchers.Main) {
            val tile = qsTile ?: return@withContext
            when {
                session != null -> {
                    tile.label = if (hide) getString(R.string.tile_session_label) else session.title
                    tile.state = if (session.running) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
                    setSubtitle(tile, getString(if (session.running) R.string.tile_session_running else R.string.tile_session_paused))
                }
                configured != null -> {
                    tile.label = if (hide) getString(R.string.tile_session_label) else configured.title
                    tile.state = Tile.STATE_INACTIVE
                    setSubtitle(tile, getString(R.string.start_session))
                }
                else -> {
                    tile.label = getString(R.string.tile_session_label)
                    tile.state = Tile.STATE_INACTIVE
                    setSubtitle(tile, getString(R.string.tile_choose_habit))
                }
            }
            tile.updateTile()
        }
    }

    private fun setSubtitle(tile: Tile, text: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) tile.subtitle = text
    }
}
