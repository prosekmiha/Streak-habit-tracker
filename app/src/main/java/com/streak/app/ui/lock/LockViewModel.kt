package com.streak.app.ui.lock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streak.app.lock.AppLockManager
import com.streak.app.lock.LockConfig
import com.streak.app.lock.PinResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LockUiState(
    val pin: String = "",
    val busy: Boolean = false,
    // Se poskusov brez premora po napacni kodi (null = ni bilo napake)
    val freeAttemptsLeft: Int? = null,
    val lockoutSeconds: Long = 0,
    val errorKey: Int = 0,
    val recoveryError: Boolean = false
)

@HiltViewModel
class LockViewModel @Inject constructor(private val manager: AppLockManager) : ViewModel() {

    private val _state = MutableStateFlow(LockUiState())
    val state: StateFlow<LockUiState> = _state.asStateFlow()
    val config: StateFlow<LockConfig> = manager.config
    private var countdown: Job? = null

    init {
        startCountdown(manager.lockoutRemainingMs())
    }

    fun setPin(pin: String) = _state.update { it.copy(pin = pin) }

    fun submit(pin: String) {
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            when (val result = manager.checkPin(pin)) {
                PinResult.Success -> _state.value = LockUiState()
                is PinResult.Wrong -> _state.update {
                    it.copy(pin = "", busy = false, freeAttemptsLeft = result.freeAttemptsLeft, errorKey = it.errorKey + 1)
                }
                is PinResult.LockedOut -> {
                    _state.update { it.copy(pin = "", busy = false, freeAttemptsLeft = null, errorKey = it.errorKey + 1) }
                    startCountdown(result.remainingMs)
                }
            }
        }
    }

    fun biometricSucceeded() = manager.biometricSucceeded()

    fun useRecovery(code: String, onDone: () -> Unit) {
        viewModelScope.launch {
            if (manager.useRecoveryCode(code)) {
                _state.value = LockUiState()
                onDone()
            } else {
                _state.update { it.copy(recoveryError = true) }
                startCountdown(manager.lockoutRemainingMs())
            }
        }
    }

    private fun startCountdown(ms: Long) {
        countdown?.cancel()
        if (ms <= 0) {
            _state.update { it.copy(lockoutSeconds = 0) }
            return
        }
        countdown = viewModelScope.launch {
            while (isActive) {
                val left = manager.lockoutRemainingMs()
                _state.update { it.copy(lockoutSeconds = (left + 999) / 1000) }
                if (left <= 0) break
                delay(250)
            }
        }
    }
}
