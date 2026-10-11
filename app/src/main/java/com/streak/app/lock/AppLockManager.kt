package com.streak.app.lock

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import com.streak.app.di.ApplicationScope
import com.streak.app.domain.lock.AutoLock
import com.streak.app.domain.lock.LockoutPolicy
import com.streak.app.domain.lock.PinHasher
import com.streak.app.domain.lock.RecoveryCode
import com.streak.app.domain.session.TimePoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

enum class LockState { LOADING, DISABLED, LOCKED, UNLOCKED }

sealed interface PinResult {
    data object Success : PinResult
    // Se toliko poskusov brez zamika (0 = naslednji napacni sprozi zamik)
    data class Wrong(val freeAttemptsLeft: Int) : PinResult
    data class LockedOut(val remainingMs: Long) : PinResult
}

// Zaklep aplikacije: koda (4 ali 6 stevk), neobvezno biometrija, samodejni zaklep po
// casu v ozadju, stopnjevan zamik po napacnih poskusih in obnovitvena koda
@Singleton
class AppLockManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val store: AppLockStore,
    @ApplicationScope scope: CoroutineScope
) {
    private val hasher = PinHasher(store::pepper)
    private val mutex = Mutex()
    private val _state = MutableStateFlow(LockState.LOADING)
    val state: StateFlow<LockState> = _state.asStateFlow()

    val config: StateFlow<LockConfig> = store.config.stateIn(scope, SharingStarted.Eagerly, LockConfig())

    // Kdaj je aplikacija sla v ozadje (v pomnilniku; hladen zagon je vedno zaklenjen)
    private var backgroundSince: TimePoint? = null

    init {
        scope.launch {
            _state.value = if (store.current().enabled) LockState.LOCKED else LockState.DISABLED
        }
    }

    fun now(): TimePoint = TimePoint(
        SystemClock.elapsedRealtime(),
        Instant.now(),
        Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, 0)
    )

    fun onAppBackground() {
        if (_state.value == LockState.UNLOCKED) backgroundSince = now()
    }

    fun onAppForeground() {
        val since = backgroundSince ?: return
        backgroundSince = null
        if (_state.value == LockState.UNLOCKED && config.value.enabled && AutoLock.shouldLock(since, config.value.timeoutMs, now())) {
            _state.value = LockState.LOCKED
        }
    }

    // Preostali zamik zaradi napacnih poskusov
    fun lockoutRemainingMs(c: LockConfig = config.value): Long =
        LockoutPolicy.remainingMs(c.lockoutSince, LockoutPolicy.delayMs(c.failedAttempts), now())

    // Napacen poskus: povecaj stevec in po potrebi zacni zamik
    private suspend fun recordFailure(c: LockConfig): Long {
        val failed = c.failedAttempts + 1
        val delay = LockoutPolicy.delayMs(failed)
        store.update { it.copy(failedAttempts = failed, lockoutSince = if (delay > 0) now() else null) }
        return delay
    }

    // Preveri kodo (odklep, pa tudi potrditev pred spremembo nastavitev)
    suspend fun checkPin(pin: String): PinResult = mutex.withLock {
        val c = store.current()
        val remaining = lockoutRemainingMs(c)
        if (remaining > 0) return PinResult.LockedOut(remaining)
        val secrets = store.secrets() ?: return PinResult.Success
        val ok = withContext(Dispatchers.Default) { hasher.verify(pin, secrets.pinSalt, secrets.pinHash) }
        if (ok) {
            store.update { it.copy(failedAttempts = 0, lockoutSince = null) }
            if (_state.value == LockState.LOCKED) _state.value = LockState.UNLOCKED
            return PinResult.Success
        }
        val delay = recordFailure(c)
        return if (delay > 0) PinResult.LockedOut(delay) else PinResult.Wrong(LockoutPolicy.FREE_ATTEMPTS - c.failedAttempts - 1)
    }

    fun biometricSucceeded() {
        if (_state.value == LockState.LOCKED) _state.value = LockState.UNLOCKED
    }

    // Obnovitvena koda izklopi zaklep (uporabnik ga lahko znova nastavi z novo kodo)
    suspend fun useRecoveryCode(input: String): Boolean = mutex.withLock {
        val c = store.current()
        if (lockoutRemainingMs(c) > 0) return false
        val secrets = store.secrets() ?: return false
        val hash = secrets.recoveryHash ?: return false
        val salt = secrets.recoverySalt ?: return false
        val ok = withContext(Dispatchers.Default) { hasher.verify(RecoveryCode.normalize(input), salt, hash) }
        if (!ok) {
            recordFailure(c)
            return false
        }
        store.clear()
        _state.value = LockState.DISABLED
        true
    }

    // Vklop: vrne obnovitveno kodo, ki se pokaze samo enkrat
    suspend fun enable(pin: String): String = mutex.withLock {
        val recovery = RecoveryCode.generate()
        withContext(Dispatchers.Default) {
            val salt = hasher.newSalt()
            store.savePin(hasher.hash(pin, salt), salt, pin.length)
            val rSalt = hasher.newSalt()
            store.saveRecovery(hasher.hash(recovery, rSalt), rSalt)
        }
        store.update { it.copy(enabled = true, privacyScreen = true, failedAttempts = 0, lockoutSince = null) }
        _state.value = LockState.UNLOCKED
        recovery
    }

    suspend fun changePin(pin: String) = mutex.withLock {
        withContext(Dispatchers.Default) {
            val salt = hasher.newSalt()
            store.savePin(hasher.hash(pin, salt), salt, pin.length)
        }
    }

    suspend fun newRecoveryCode(): String = mutex.withLock {
        val recovery = RecoveryCode.generate()
        withContext(Dispatchers.Default) {
            val salt = hasher.newSalt()
            store.saveRecovery(hasher.hash(recovery, salt), salt)
        }
        recovery
    }

    suspend fun disable() = mutex.withLock {
        store.clear()
        _state.value = LockState.DISABLED
    }

    suspend fun setBiometric(enabled: Boolean) = store.update { it.copy(biometric = enabled) }
    suspend fun setTimeout(ms: Long) = store.update { it.copy(timeoutMs = ms) }
    suspend fun setPrivacyScreen(enabled: Boolean) = store.update { it.copy(privacyScreen = enabled) }

    companion object {
        val TIMEOUTS = listOf(0L, 60_000L, 5 * 60_000L, 30 * 60_000L)
    }
}
