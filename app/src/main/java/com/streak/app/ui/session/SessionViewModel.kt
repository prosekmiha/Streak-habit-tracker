package com.streak.app.ui.session

import android.os.SystemClock
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streak.app.domain.model.Habit
import com.streak.app.domain.model.HabitSession
import com.streak.app.domain.repository.HabitRepository
import com.streak.app.domain.time.DayClock
import com.streak.app.domain.usecase.ToggleHabitUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Duration
import java.util.UUID
import javax.inject.Inject

data class SessionState(
    val habit: Habit? = null,
    val elapsedSeconds: Long = 0,
    val running: Boolean = true,
    val finished: Boolean = false
) {
    val targetSeconds: Long get() = habit?.targetValue?.toLong() ?: 0
    // Dihalni vodic 4 s vdih / 4 s izdih
    val breathingIn: Boolean get() = (elapsedSeconds / 4) % 2 == 0L
    val breathingCount: Int get() = (4 - (elapsedSeconds % 4)).toInt()
}

// Casovna seja. Cas temelji na elapsedRealtime (neodvisno od spremembe ure) in je
// shranjen v SavedStateHandle; trajna seja z obvestilom pride v M3.
@HiltViewModel
class SessionViewModel @Inject constructor(
    private val habitRepository: HabitRepository,
    private val logHabit: ToggleHabitUseCase,
    private val dayClock: DayClock,
    private val handle: SavedStateHandle
) : ViewModel() {

    private val habitId: String = checkNotNull(handle["habitId"])
    private var runStartedAt: Long? = handle.get<Long>(KEY_RUN) ?: SystemClock.elapsedRealtime().also { handle[KEY_RUN] = it }
    private var accumulated: Long = handle.get<Long>(KEY_ACC) ?: 0L

    private val _state = MutableStateFlow(SessionState(running = handle.get<Boolean>(KEY_RUNNING) ?: true))
    val state: StateFlow<SessionState> = _state.asStateFlow()

    init {
        if (!_state.value.running) runStartedAt = null
        viewModelScope.launch { _state.update { it.copy(habit = habitRepository.getHabit(habitId)) } }
        viewModelScope.launch {
            while (isActive) {
                _state.update { it.copy(elapsedSeconds = elapsedMillis() / 1000) }
                delay(250)
            }
        }
    }

    private fun elapsedMillis(): Long = accumulated + (runStartedAt?.let { SystemClock.elapsedRealtime() - it } ?: 0L)

    fun togglePause() {
        if (_state.value.running) {
            accumulated = elapsedMillis()
            runStartedAt = null
        } else {
            runStartedAt = SystemClock.elapsedRealtime()
        }
        handle[KEY_ACC] = accumulated
        handle[KEY_RUN] = runStartedAt
        _state.update { it.copy(running = !it.running).also { s -> handle[KEY_RUNNING] = s.running } }
    }

    // Zakljuci sejo: shrani zgodovino in pristej trajanje k danasnjemu napredku
    fun finish(save: Boolean, onDone: () -> Unit) {
        val habit = _state.value.habit
        val seconds = elapsedMillis() / 1000
        if (!save || habit == null || seconds < MIN_SECONDS) {
            onDone()
            return
        }
        viewModelScope.launch {
            val today = dayClock.today()
            val now = dayClock.now()
            habitRepository.saveSession(
                HabitSession(
                    id = UUID.randomUUID().toString(),
                    habitId = habit.id,
                    date = today,
                    startedAt = now.minus(Duration.ofSeconds(seconds)),
                    endedAt = now,
                    durationSeconds = seconds
                )
            )
            logHabit.addProgress(habit, today, seconds.toDouble())
            _state.update { it.copy(finished = true) }
            onDone()
        }
    }

    private companion object {
        const val KEY_RUN = "session_run_started"
        const val KEY_ACC = "session_accumulated"
        const val KEY_RUNNING = "session_running"
        const val MIN_SECONDS = 10L
    }
}
