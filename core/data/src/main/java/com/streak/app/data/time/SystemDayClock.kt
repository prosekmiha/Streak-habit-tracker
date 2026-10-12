package com.streak.app.data.time

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import com.streak.app.di.ApplicationScope
import com.streak.app.domain.repository.SettingsRepository
import com.streak.app.domain.time.DayClock
import com.streak.app.domain.time.LogicalDay
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

// Logicni dan po lokalni uri naprave z upostevanjem "dan se konca ob".
// Ob spremembi ure, casovnega pasu ali datuma (sistemski broadcast) se
// danasnji dan takoj ponovno izracuna.
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class SystemDayClock @Inject constructor(
    @ApplicationContext context: Context,
    private val settingsRepository: SettingsRepository,
    @ApplicationScope scope: CoroutineScope
) : DayClock {

    private val timeChanged = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    init {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_DATE_CHANGED)
        }
        ContextCompat.registerReceiver(
            context,
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    timeChanged.tryEmit(Unit)
                }
            },
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    private val dayEndsAt: Flow<LocalTime> =
        settingsRepository.settings.map { it.dayEndsAt }.distinctUntilChanged()

    override fun now(): Instant = Instant.now()

    override fun zone(): ZoneId = ZoneId.systemDefault()

    override suspend fun today(): LocalDate = dateOf(now())

    override suspend fun dateOf(instant: Instant): LocalDate =
        LogicalDay.of(instant, zone(), dayEndsAt.first())

    override val todayFlow: Flow<LocalDate> = dayEndsAt
        .flatMapLatest { endsAt ->
            channelFlow {
                while (true) {
                    val now = now()
                    send(LogicalDay.of(now, zone(), endsAt))
                    val untilBoundary = Duration.between(now, LogicalDay.nextBoundary(now, zone(), endsAt))
                    // Zgornja meja 1 h kot varovalka, ce sistemski broadcast izostane
                    val wait = untilBoundary.coerceAtMost(MAX_WAIT).toMillis().coerceAtLeast(1_000)
                    withTimeoutOrNull(wait) { timeChanged.first() }
                }
            }
        }
        .distinctUntilChanged()
        .shareIn(scope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    private companion object {
        val MAX_WAIT: Duration = Duration.ofHours(1)
    }
}
