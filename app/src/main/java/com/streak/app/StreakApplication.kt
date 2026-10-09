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
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop
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
    @Inject @ApplicationScope lateinit var appScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        notificationHelper.createChannels()

        appScope.launch {
            // Odpri bazo (in sprozi morebitno migracijo) v ozadju, z obnovo ob napaki
            databaseGuard.openVerified(database, StreakDatabase.NAME)
            alarmScheduler.scheduleDailyReset()
            // Predpomnjeni streaki morajo veljati za danasnji dan
            recalculator.onDayChanged()
            // Integriteta se preveri odlozeno, da ne upocasni zagona
            delay(INTEGRITY_CHECK_DELAY_MS)
            databaseGuard.checkIntegrityIfDue(database)
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
    }
}
