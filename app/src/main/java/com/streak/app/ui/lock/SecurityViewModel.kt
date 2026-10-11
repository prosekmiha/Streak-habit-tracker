package com.streak.app.ui.lock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streak.app.domain.lock.PinRules
import com.streak.app.lock.AppLockManager
import com.streak.app.lock.LockConfig
import com.streak.app.lock.PinResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class VerifyPurpose { DISABLE, CHANGE_PIN, NEW_RECOVERY }
enum class NewPinPurpose { ENABLE, CHANGE }
enum class PinError { WRONG, LOCKED_OUT, WEAK, MISMATCH }

// Korak v listu za kodo
sealed interface SecurityFlow {
    data object None : SecurityFlow
    data class Verify(val purpose: VerifyPurpose, val pin: String = "", val error: PinError? = null, val errorKey: Int = 0) : SecurityFlow
    data class NewPin(
        val purpose: NewPinPurpose,
        val length: Int = 4,
        val first: String? = null,
        val pin: String = "",
        val error: PinError? = null,
        val errorKey: Int = 0
    ) : SecurityFlow
    data class ShowRecovery(val code: String) : SecurityFlow
}

@HiltViewModel
class SecurityViewModel @Inject constructor(private val manager: AppLockManager) : ViewModel() {

    val config: StateFlow<LockConfig> = manager.config
    private val _flow = MutableStateFlow<SecurityFlow>(SecurityFlow.None)
    val flow: StateFlow<SecurityFlow> = _flow.asStateFlow()

    fun startEnable() { _flow.value = SecurityFlow.NewPin(NewPinPurpose.ENABLE) }
    fun startVerify(purpose: VerifyPurpose) { _flow.value = SecurityFlow.Verify(purpose) }
    fun close() { _flow.value = SecurityFlow.None }

    fun setPin(pin: String) = _flow.update {
        when (it) {
            is SecurityFlow.Verify -> it.copy(pin = pin)
            is SecurityFlow.NewPin -> it.copy(pin = pin)
            else -> it
        }
    }

    fun setLength(length: Int) = _flow.update { if (it is SecurityFlow.NewPin && it.first == null) it.copy(length = length, pin = "", error = null) else it }

    fun verify(pin: String) {
        val step = _flow.value as? SecurityFlow.Verify ?: return
        viewModelScope.launch {
            when (val r = manager.checkPin(pin)) {
                PinResult.Success -> when (step.purpose) {
                    VerifyPurpose.DISABLE -> { manager.disable(); close() }
                    VerifyPurpose.CHANGE_PIN -> _flow.value = SecurityFlow.NewPin(NewPinPurpose.CHANGE, length = config.value.pinLength)
                    VerifyPurpose.NEW_RECOVERY -> _flow.value = SecurityFlow.ShowRecovery(manager.newRecoveryCode())
                }
                is PinResult.Wrong -> _flow.value = step.copy(pin = "", error = PinError.WRONG, errorKey = step.errorKey + 1)
                is PinResult.LockedOut -> _flow.value = step.copy(pin = "", error = PinError.LOCKED_OUT, errorKey = step.errorKey + 1)
            }
        }
    }

    // Nova koda: vnos, potrditev, nato vklop (z obnovitveno kodo) ali sprememba
    fun newPin(pin: String) {
        val step = _flow.value as? SecurityFlow.NewPin ?: return
        when {
            step.first == null && PinRules.isWeak(pin) ->
                _flow.value = step.copy(pin = "", error = PinError.WEAK, errorKey = step.errorKey + 1)
            step.first == null -> _flow.value = step.copy(first = pin, pin = "", error = null)
            pin != step.first -> _flow.value = step.copy(first = null, pin = "", error = PinError.MISMATCH, errorKey = step.errorKey + 1)
            else -> viewModelScope.launch {
                when (step.purpose) {
                    NewPinPurpose.ENABLE -> _flow.value = SecurityFlow.ShowRecovery(manager.enable(pin))
                    NewPinPurpose.CHANGE -> { manager.changePin(pin); close() }
                }
            }
        }
    }

    fun setBiometric(enabled: Boolean) = viewModelScope.launch { manager.setBiometric(enabled) }
    fun setTimeout(ms: Long) = viewModelScope.launch { manager.setTimeout(ms) }
    fun setPrivacyScreen(enabled: Boolean) = viewModelScope.launch { manager.setPrivacyScreen(enabled) }
}
