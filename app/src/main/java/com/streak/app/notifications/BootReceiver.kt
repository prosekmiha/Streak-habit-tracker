package com.streak.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.streak.app.data.local.dao.HabitDao
import com.streak.app.data.mapper.toDomain
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

// Po ponovnem zagonu telefona znova razporedi vse alarme za navade z opomniki
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var habitDao: HabitDao
    @Inject lateinit var alarmScheduler: AlarmScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        alarmScheduler.scheduleDailyReset()
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                habitDao.getHabitsWithReminders().forEach { entity ->
                    alarmScheduler.schedule(entity.toDomain())
                }
            } finally {
                pending.finish()
            }
        }
    }
}
