package com.streak.app.session

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.streak.app.data.session.SessionStore
import com.streak.app.domain.model.GoalType
import com.streak.app.domain.model.Habit
import com.streak.app.domain.model.HabitSession
import com.streak.app.domain.repository.HabitRepository
import com.streak.app.domain.session.ActiveSession
import com.streak.app.domain.session.AmbientSound
import com.streak.app.domain.session.TimePoint
import com.streak.app.domain.time.DayClock
import com.streak.app.domain.usecase.ToggleHabitUseCase
import com.streak.app.shortcuts.ShortcutPublisher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Duration
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

sealed interface StartResult {
    data object Started : StartResult
    data object Resumed : StartResult
    data class Conflict(val other: ActiveSession) : StartResult
}

// Edini vir resnice za tekoco sejo. Stanje se shrani ob vsaki spremembi; obvestilo,
// alarm za cilj in ambientni zvok se uskladijo s stanjem (zaslon, obvestilo in
// bliznjica uporabljajo isti kontroler).
@Singleton
class SessionController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val store: SessionStore,
    private val habitRepository: HabitRepository,
    private val logHabit: ToggleHabitUseCase,
    private val dayClock: DayClock,
    private val notifier: SessionNotifier,
    private val shortcuts: ShortcutPublisher
) {
    private val mutex = Mutex()
    private val loaded = CompletableDeferred<Unit>()
    private val _active = MutableStateFlow<ActiveSession?>(null)
    val active: StateFlow<ActiveSession?> = _active.asStateFlow()

    private val alarmManager get() = context.getSystemService(AlarmManager::class.java)

    suspend fun awaitLoaded() {
        if (!loaded.isCompleted) mutex.withLock {
            if (!loaded.isCompleted) {
                _active.value = store.load()
                loaded.complete(Unit)
            }
        }
    }

    fun now(): TimePoint = TimePoint(
        SystemClock.elapsedRealtime(),
        Instant.now(),
        Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, 0)
    )

    suspend fun lastAmbient(): AmbientSound = store.lastAmbient()

    // Ob zagonu procesa ali naprave: obnovi obvestilo in alarm (zvoka ne zazene sam od sebe)
    suspend fun restore() {
        awaitLoaded()
        _active.value?.let { sync(it, startAudio = false) }
    }

    suspend fun start(habit: Habit): StartResult {
        val result = update { current ->
            when {
                current == null -> {
                    val session = ActiveSession.start(habit.id, habit.title, habit.targetSeconds(), now(), store.lastAmbient())
                    session to StartResult.Started
                }
                current.habitId == habit.id -> current to StartResult.Resumed
                else -> current to StartResult.Conflict(current)
            }
        }
        // Uporabnik je odprl tekoco sejo (npr. po smrti procesa): zvok naj spet igra
        if (result == StartResult.Resumed) _active.value?.let(::startAudioIfNeeded)
        if (result == StartResult.Started) shortcuts.reportSessionStarted(habit.id)
        return result
    }

    suspend fun togglePause() = update { s -> s?.let { if (it.running) it.pause(now()) else it.resume(now()) } to Unit }
    suspend fun pause() = update { s -> s?.pause(now()) to Unit }
    suspend fun resume() = update { s -> s?.resume(now()) to Unit }
    suspend fun setAmbient(sound: AmbientSound) = update { s -> s?.copy(ambient = sound) to Unit }

    // Natancen alarm za cilj (Android 12+ zahteva dovoljenje uporabnika)
    fun exactAlarmsAllowed(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    // Alarm za cilj je sprozen (ali pa je cilj zaznan v aplikaciji): pokazi obvestilo enkrat
    suspend fun onGoalReached() {
        val s = update { s ->
            if (s == null || s.goalNotified || s.targetSeconds <= 0 || s.remainingToGoalMs(now()) != null) s to null
            else s.copy(goalNotified = true) to s
        }
        s?.let { notifier.showGoalReached(it) }
    }

    // Zakljuci sejo. Kratke seje (< 10 s) se ne shranijo. Vrne shranjeno trajanje v sekundah.
    suspend fun finish(save: Boolean): Long? {
        var saved: Long? = null
        update { s ->
            if (s != null && save) {
                val seconds = s.elapsedMs(now()) / 1000
                if (seconds >= MIN_SECONDS) {
                    persistHistory(s, seconds)
                    saved = seconds
                }
            }
            null to Unit
        }
        return saved
    }

    private suspend fun persistHistory(s: ActiveSession, seconds: Long) {
        val today = dayClock.today()
        val end = dayClock.now()
        habitRepository.saveSession(
            HabitSession(
                id = UUID.randomUUID().toString(),
                habitId = s.habitId,
                date = today,
                startedAt = end.minus(Duration.ofSeconds(seconds)),
                endedAt = end,
                durationSeconds = seconds
            )
        )
        // Trajanje se pristeje k napredku le pri casovnih navadah
        habitRepository.getHabit(s.habitId)
            ?.takeIf { it.goalType == GoalType.DURATION }
            ?.let { logHabit.addProgress(it, today, seconds.toDouble()) }
    }

    private suspend fun <R> update(block: suspend (ActiveSession?) -> Pair<ActiveSession?, R>): R {
        awaitLoaded()
        return mutex.withLock {
            val before = _active.value
            val (after, result) = block(before)
            if (after != before) {
                store.save(after)
                _active.value = after
                sync(after, startAudio = true)
            }
            result
        }
    }

    // Uskladi obvestilo, alarm za cilj in zvok s stanjem
    private fun sync(session: ActiveSession?, startAudio: Boolean) {
        if (session == null) {
            notifier.cancel()
            cancelGoalAlarm()
            return
        }
        notifier.show(session, now())
        val remaining = session.remainingToGoalMs(now())
        if (session.running && !session.goalNotified && remaining != null) scheduleGoalAlarm(remaining) else cancelGoalAlarm()
        if (startAudio) startAudioIfNeeded(session)
    }

    // Storitev se ustavi sama, ko zvok ni vec potreben; ponoven zagon je neskodljiv
    private fun startAudioIfNeeded(session: ActiveSession) {
        if (session.running && session.ambient != AmbientSound.NONE) {
            runCatching { ContextCompat.startForegroundService(context, Intent(context, AmbientService::class.java)) }
        }
    }

    private fun goalIntent(): PendingIntent = PendingIntent.getBroadcast(
        context, GOAL_REQUEST,
        Intent(context, SessionReceiver::class.java).setAction(SessionReceiver.ACTION_GOAL),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun scheduleGoalAlarm(inMs: Long) {
        val at = SystemClock.elapsedRealtime() + inMs
        val am = alarmManager
        if (exactAlarmsAllowed()) {
            am.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, at, goalIntent())
        } else {
            am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, at, goalIntent())
        }
    }

    private fun cancelGoalAlarm() = alarmManager.cancel(goalIntent())

    private companion object {
        const val MIN_SECONDS = 10L
        const val GOAL_REQUEST = 7301
    }
}

// Ciljno trajanje casovne navade v sekundah
fun Habit.targetSeconds(): Long = if (goalType == GoalType.DURATION) targetValue.toLong().coerceAtLeast(0) else 0
