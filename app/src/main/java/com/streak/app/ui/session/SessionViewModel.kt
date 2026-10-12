package com.streak.app.ui.session

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streak.app.domain.model.CompletionStatus
import com.streak.app.domain.model.Habit
import com.streak.app.domain.repository.HabitRepository
import com.streak.app.domain.session.ActiveSession
import com.streak.app.domain.session.AmbientSound
import com.streak.app.domain.session.SessionController
import com.streak.app.domain.session.StartResult
import com.streak.app.domain.time.DayClock
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SessionState(
    val habit: Habit? = null,
    val elapsedSeconds: Long = 0,
    val running: Boolean = true,
    val ambient: AmbientSound = AmbientSound.NONE,
    // Druga seja ze tece (le ena hkrati)
    val conflict: ActiveSession? = null,
    // Seja je bila koncana (tudi iz obvestila): zaslon se zapre
    val ended: Boolean = false,
    // Brez dovoljenja za natancne alarme je opozorilo ob cilju lahko zamaknjeno
    val exactAlarmMissing: Boolean = false,
    // Navada je danes ze opravljena: seja caka na "Zacni" (dotik po nesreci ne zazene casa)
    val awaitingStart: Boolean = false
) {
    val targetSeconds: Long get() = habit?.targetSeconds() ?: 0
    val goalReached: Boolean get() = targetSeconds > 0 && elapsedSeconds >= targetSeconds
    // Dihalni vodic 4 s vdih / 4 s izdih
    val breathingIn: Boolean get() = (elapsedSeconds / 4) % 2 == 0L
    val breathingCount: Int get() = (4 - (elapsedSeconds % 4)).toInt()
}

// Zaslon je le pogled na SessionController: seja tece naprej tudi, ko zaslon zapustis
// (obvestilo, zaklenjen zaslon, smrt procesa).
@HiltViewModel
class SessionViewModel @Inject constructor(
    private val habitRepository: HabitRepository,
    private val controller: SessionController,
    private val dayClock: DayClock,
    handle: SavedStateHandle
) : ViewModel() {

    private val habitId: String = checkNotNull(handle["habitId"])
    private val _state = MutableStateFlow(SessionState())
    val state: StateFlow<SessionState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val habit = habitRepository.getHabit(habitId)
            if (habit == null) {
                _state.update { it.copy(ended = true) }
                return@launch
            }
            _state.update { it.copy(habit = habit) }
            refreshAlarmPermission()
            controller.awaitLoaded()
            val ownRunning = controller.active.value?.habitId == habitId
            val doneToday = habitRepository.getCompletion(habitId, dayClock.today())?.status == CompletionStatus.DONE
            if (!ownRunning && doneToday) {
                _state.update { it.copy(running = false, awaitingStart = true) }
                return@launch
            }
            begin(habit)
        }
    }

    private suspend fun begin(habit: Habit) {
        val result = controller.start(habit)
        if (result is StartResult.Conflict) {
            _state.update { it.copy(conflict = result.other) }
            return
        }
        observe()
    }

    private suspend fun observe() {
        var seen = false
        while (viewModelScope.isActive) {
            val session = controller.active.value
            if (session?.habitId == habitId) {
                seen = true
                val now = controller.now()
                _state.update {
                    it.copy(
                        elapsedSeconds = session.elapsedMs(now) / 1000,
                        running = session.running,
                        ambient = session.ambient
                    )
                }
                // Zaslon je odprt: cilj zaznamo takoj (zvok obvestila tudi z zaprtimi ocmi)
                if (session.running && !session.goalNotified && session.targetSeconds > 0 && session.remainingToGoalMs(now) == null) {
                    controller.onGoalReached()
                }
            } else if (seen) {
                // Seja je bila koncana drugje (npr. "Koncaj" v obvestilu)
                _state.update { it.copy(ended = true) }
                return
            }
            delay(TICK_MS)
        }
    }

    // Po vrnitvi iz sistemskih nastavitev: ce je dovoljenje novo, prerazporedi alarm
    fun refreshAlarmPermission() {
        val missing = _state.value.targetSeconds > 0 && !controller.exactAlarmsAllowed()
        val wasMissing = _state.value.exactAlarmMissing
        _state.update { it.copy(exactAlarmMissing = missing) }
        if (wasMissing && !missing) viewModelScope.launch { controller.restore() }
    }

    fun togglePause() = viewModelScope.launch {
        val s = _state.value
        if (s.awaitingStart) {
            val habit = s.habit ?: return@launch
            _state.update { it.copy(awaitingStart = false, running = true) }
            begin(habit)
        } else {
            controller.togglePause()
        }
    }

    fun setAmbient(sound: AmbientSound) = viewModelScope.launch { controller.setAmbient(sound) }

    fun finish(save: Boolean) = viewModelScope.launch {
        // Seja se ni zacela: le zapri (ne koncaj morebitne druge tekoce seje)
        if (!_state.value.awaitingStart) controller.finish(save)
        _state.update { it.copy(ended = true) }
    }

    // Zakljuci drugo tekoco sejo (shrani jo) in zacni to
    fun replaceConflict() = viewModelScope.launch {
        val habit = _state.value.habit ?: return@launch
        controller.finish(save = true)
        _state.update { it.copy(conflict = null) }
        controller.start(habit)
        observe()
    }

    private companion object {
        const val TICK_MS = 250L
    }
}
