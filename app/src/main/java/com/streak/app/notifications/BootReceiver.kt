package com.streak.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.streak.app.domain.repository.HabitRepository
import com.streak.app.session.SessionController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

// Po ponovnem zagonu telefona znova razporedi vse alarme
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var habitRepository: HabitRepository
    @Inject lateinit var alarmScheduler: AlarmScheduler
    @Inject lateinit var sessionController: SessionController

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                alarmScheduler.scheduleDailyReset()
                habitRepository.getHabits().forEach { alarmScheduler.schedule(it) }
                // Tekoca seja: obnovi obvestilo in alarm za cilj
                sessionController.restore()
            } finally {
                pending.finish()
            }
        }
    }
}
