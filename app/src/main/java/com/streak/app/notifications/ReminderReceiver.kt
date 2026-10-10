package com.streak.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.streak.app.di.ApplicationScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

// Sprozen opomnik navade (ali odlog "cez 15 min")
@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {

    @Inject lateinit var dispatcher: ReminderDispatcher
    @Inject @ApplicationScope lateinit var scope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getStringExtra(EXTRA_HABIT_ID) ?: return
        val snoozed = intent.getBooleanExtra(EXTRA_SNOOZE, false)
        val pending = goAsync()
        scope.launch {
            try {
                dispatcher.onReminder(habitId, snoozed)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val EXTRA_HABIT_ID = "habit_id"
        const val EXTRA_SNOOZE = "snooze"
    }
}
