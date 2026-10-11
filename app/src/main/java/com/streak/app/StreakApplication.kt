package com.streak.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.streak.app.data.local.DatabaseGuard
import com.streak.app.data.local.StreakDatabase
import com.streak.app.data.repository.StreakRecalculator
import com.streak.app.di.ApplicationScope
import com.streak.app.domain.time.DayClock
import com.streak.app.notifications.AlarmScheduler
import com.streak.app.notifications.NotificationHelper
import com.streak.app.notifications.ReminderDispatcher
import com.streak.app.domain.repository.SettingsRepository
import com.streak.app.domain.repository.HabitRepository
import com.streak.app.domain.repository.TaskRepository
import com.streak.app.backup.BackupManager
import com.streak.app.session.SessionController
import com.streak.app.shortcuts.ShortcutPublisher
import com.streak.app.widget.WidgetUpdater
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class StreakApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var notificationHelper: NotificationHelper
    @Inject lateinit var alarmScheduler: AlarmScheduler
    @Inject lateinit var database: StreakDatabase
    @Inject lateinit var databaseGuard: DatabaseGuard
    @Inject lateinit var recalculator: StreakRecalculator
    @Inject lateinit var dayClock: DayClock
    @Inject lateinit var sessionController: SessionController
    @Inject lateinit var shortcutPublisher: ShortcutPublisher
    @Inject lateinit var reminderDispatcher: ReminderDispatcher
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var habitRepository: HabitRepository
    @Inject lateinit var taskRepository: TaskRepository
    @Inject lateinit var backupManager: BackupManager
    @Inject lateinit var widgetUpdater: WidgetUpdater
    @Inject @ApplicationScope lateinit var appScope: CoroutineScope

    @OptIn(FlowPreview::class)
    override fun onCreate() {
        super.onCreate()
        notificationHelper.createChannels()

        appScope.launch {
            // Odpri bazo (in sprozi morebitno migracijo) v ozadju, z obnovo ob napaki
            databaseGuard.openVerified(database, StreakDatabase.NAME)
            // Alarme lahko pobrise sistem (prisilna ustavitev, posodobitev): vedno jih obnovi
            alarmScheduler.rescheduleAll()
            // Predpomnjeni streaki morajo veljati za danasnji dan
            recalculator.onDayChanged()
            // Integriteta se preveri odlozeno, da ne upocasni zagona
            delay(INTEGRITY_CHECK_DELAY_MS)
            databaseGuard.checkIntegrityIfDue(database)
        }

        // Tekoca seja iz prejsnjega procesa: obvestilo in alarm za cilj
        appScope.launch { sessionController.restore() }
        appScope.launch { shortcutPublisher.run() }
        // Opravljena navada pospravi svoj opomnik, vse opravljeno pa vecerni opomnik
        appScope.launch { reminderDispatcher.syncWithProgress() }
        // Widgeti sledijo podatkom (tudi menjavi dneva, ki sprozi zagon procesa)
        appScope.launch { widgetUpdater.run() }
        // Samodejni posnetek za Androidovo varnostno kopijo, 10 s po zadnji spremembi
        appScope.launch {
            combine(
                habitRepository.observeHabits(),
                taskRepository.observeTasks(),
                settingsRepository.settings.map { it.cloudSnapshotEnabled }.distinctUntilChanged()
            ) { _, _, _ -> Unit }
                .debounce(SNAPSHOT_DEBOUNCE_MS)
                .collect { runCatching { backupManager.writeSnapshot() } }
        }
        // Sprememba ur povzetka/opomnika ali meje dneva: prerazporedi
        appScope.launch {
            settingsRepository.settings
                .map { listOf(it.dayEndsAt, it.briefingEnabled, it.briefingTime, it.nudgeEnabled, it.nudgeTime) }
                .distinctUntilChanged()
                .drop(1)
                .collect {
                    alarmScheduler.scheduleDailyReset()
                    alarmScheduler.scheduleDaily()
                }
        }

        // Ob vsaki menjavi logicnega dne (meja dneva, sprememba ure ali pasu) med
        // delovanjem procesa posodobi streake in prerazporedi dnevni alarm
        appScope.launch {
            dayClock.todayFlow.drop(1).collect {
                recalculator.onDayChanged()
                alarmScheduler.scheduleDailyReset()
            }
        }
    }

    // WorkManager z Hilt injekcijo
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    private companion object {
        const val INTEGRITY_CHECK_DELAY_MS = 10_000L
        const val SNAPSHOT_DEBOUNCE_MS = 10_000L
    }
}
