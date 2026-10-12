package com.streak.app.domain.session

import com.streak.app.domain.model.Habit
import kotlinx.coroutines.flow.StateFlow

sealed interface StartResult {
    data object Started : StartResult
    data object Resumed : StartResult
    data class Conflict(val other: ActiveSession) : StartResult
}

// Edini vir resnice za tekoco sejo (zaslon, obvestilo, bliznjica in ambientna storitev)
interface SessionController {
    val active: StateFlow<ActiveSession?>

    fun now(): TimePoint

    // Natancen alarm za cilj (Android 12+ zahteva dovoljenje uporabnika)
    fun exactAlarmsAllowed(): Boolean

    suspend fun awaitLoaded()
    suspend fun lastAmbient(): AmbientSound

    // Ob zagonu procesa ali naprave: obnovi obvestilo in alarm (zvoka ne zazene sam od sebe)
    suspend fun restore()

    suspend fun start(habit: Habit): StartResult
    suspend fun togglePause()
    suspend fun pause()
    suspend fun resume()
    suspend fun setAmbient(sound: AmbientSound)

    // Cilj je dosezen (alarm ali zaznava v aplikaciji): obvestilo se pokaze enkrat
    suspend fun onGoalReached()

    // Zakljuci sejo. Kratke seje se ne shranijo. Vrne shranjeno trajanje v sekundah.
    suspend fun finish(save: Boolean): Long?
}
