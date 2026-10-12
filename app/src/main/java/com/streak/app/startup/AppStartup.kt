package com.streak.app.startup

import com.streak.app.data.local.DatabaseGuard
import com.streak.app.data.local.StreakDatabase
import com.streak.app.data.repository.StreakRecalculator
import com.streak.app.di.ApplicationScope
import com.streak.app.domain.backup.BackupService
import com.streak.app.domain.gamification.GameEvents
import com.streak.app.domain.health.DatabaseHealth
import com.streak.app.domain.notify.ReminderScheduler
import com.streak.app.domain.repository.GameRepository
import com.streak.app.domain.repository.HabitRepository
import com.streak.app.domain.repository.MoodRepository
import com.streak.app.domain.repository.SettingsRepository
import com.streak.app.domain.repository.TaskRepository
import com.streak.app.domain.session.SessionController
import com.streak.app.domain.time.DayClock
import com.streak.app.notifications.NotificationHelper
import com.streak.app.notifications.ReminderDispatcher
import com.streak.app.shortcuts.ShortcutPublisher
import com.streak.app.tiles.TileUpdater
import com.streak.app.widget.WidgetUpdater
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

// Vse, kar mora teci, dokler zivi proces: odpiranje baze, alarmi, seja, widgeti,
// igrifikacija in samodejni posnetek. Klice se enkrat iz Application.onCreate.
class AppStartup @Inject constructor(
    private val notificationHelper: NotificationHelper,
    private val reminders: ReminderScheduler,
    private val database: StreakDatabase,
    private val databaseGuard: DatabaseGuard,
    private val recalculator: StreakRecalculator,
    private val dayClock: DayClock,
    private val sessionController: SessionController,
    private val shortcutPublisher: ShortcutPublisher,
    private val reminderDispatcher: ReminderDispatcher,
    private val settingsRepository: SettingsRepository,
    private val habitRepository: HabitRepository,
    private val taskRepository: TaskRepository,
    private val moodRepository: MoodRepository,
    private val gameRepository: GameRepository,
    private val backupService: BackupService,
    private val widgetUpdater: WidgetUpdater,
    private val tileUpdater: TileUpdater,
    private val gameEvents: GameEvents,
    @ApplicationScope private val appScope: CoroutineScope
) {

    fun start() {
        notificationHelper.createChannels()

        appScope.launch {
            // Odpri bazo (in sprozi morebitno migracijo) v ozadju, z obnovo ob napaki
            databaseGuard.openVerified(database, StreakDatabase.NAME)
            // Neuspela migracija zapre bazo: brez nje ne zaganjamo nicesar, kar jo bere,
            // da lahko UI pokaze stanje baze namesto sesutja
            if (databaseGuard.health.value != DatabaseHealth.Ok) return@launch
            // Prazna baza (nova namestitev, nastavitve obnovljene iz oblaka): stari "zadnji znani
            // dan" ni merilo za skok ure
            if (habitRepository.getHabits(includeArchived = true).isEmpty()) {
                settingsRepository.update { if (it.lastKnownDate == null) it else it.copy(lastKnownDate = null) }
            }
            startDatabaseConsumers()
            // Alarme lahko pobrise sistem (prisilna ustavitev, posodobitev): vedno jih obnovi
            reminders.rescheduleAll()
            // Predpomnjeni streaki morajo veljati za danasnji dan
            recalculator.onDayChanged()
            // Integriteta se preveri odlozeno, da ne upocasni zagona
            delay(INTEGRITY_CHECK_DELAY_MS)
            databaseGuard.checkIntegrityIfDue(database)
        }

        // Tekoca seja iz prejsnjega procesa: obvestilo in alarm za cilj
        appScope.launch { sessionController.restore() }
        // Ploscica seje sledi tekoci seji (zacetek, pavza, konec)
        appScope.launch {
            sessionController.active.map { it?.habitId to it?.running }.distinctUntilChanged().collect { tileUpdater.requestAll() }
        }
        appScope.launch { rescheduleDailyOnSettingsChange() }
    }

    // Opravila, ki berejo bazo; zazenejo se sele, ko je baza uspesno odprta
    private fun startDatabaseConsumers() {
        appScope.launch { shortcutPublisher.run() }
        // Opravljena navada pospravi svoj opomnik, vse opravljeno pa vecerni opomnik
        appScope.launch { reminderDispatcher.syncWithProgress() }
        // Widgeti sledijo podatkom (tudi menjavi dneva, ki sprozi zagon procesa)
        appScope.launch { widgetUpdater.run() }
        // Nove znacke, nivo in opravljen izziv dneva -> nabiralnik in praznovanje
        appScope.launch { gameEvents.run() }
        appScope.launch { writeSnapshotOnChanges() }
        appScope.launch { followDayChanges() }
    }

    // Samodejni posnetek za Androidovo varnostno kopijo, 10 s po zadnji spremembi
    @OptIn(FlowPreview::class)
    private suspend fun writeSnapshotOnChanges() {
        // Razpolozenje in seje (tudi pri navadah, ki niso casovne) ne spremenijo seznama navad,
        // zato jih spremljamo posebej (stanje igrifikacije vsebuje seje)
        combine(
            habitRepository.observeHabits(),
            taskRepository.observeTasks(),
            moodRepository.observeAll(),
            gameRepository.state,
            settingsRepository.settings.map { it.cloudSnapshotEnabled }.distinctUntilChanged()
        ) { _, _, _, _, _ -> Unit }
            .debounce(SNAPSHOT_DEBOUNCE_MS)
            .collect { runCatching { backupService.writeSnapshot() } }
    }

    // Sprememba ur povzetka/opomnika ali meje dneva: prerazporedi
    private suspend fun rescheduleDailyOnSettingsChange() {
        settingsRepository.settings
            .map { listOf(it.dayEndsAt, it.briefingEnabled, it.briefingTime, it.nudgeEnabled, it.nudgeTime) }
            .distinctUntilChanged()
            .drop(1)
            .collect {
                reminders.scheduleDailyReset()
                reminders.scheduleDaily()
            }
    }

    // Ob vsaki menjavi logicnega dne (meja dneva, sprememba ure ali pasu) med
    // delovanjem procesa posodobi streake in prerazporedi dnevni alarm
    private suspend fun followDayChanges() {
        dayClock.todayFlow.drop(1).collect {
            recalculator.onDayChanged()
            reminders.scheduleDailyReset()
        }
    }

    private companion object {
        const val INTEGRITY_CHECK_DELAY_MS = 10_000L
        const val SNAPSHOT_DEBOUNCE_MS = 10_000L
    }
}
