package com.streak.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.streak.app.di.ApplicationScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

// Dejanji opomnika: "Opravljeno" in "Cez 15 min"
@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    @Inject lateinit var dispatcher: ReminderDispatcher
    @Inject @ApplicationScope lateinit var scope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getStringExtra(EXTRA_HABIT_ID) ?: return
        val pending = goAsync()
        scope.launch {
            try {
                when (intent.action) {
                    ACTION_DONE -> dispatcher.markDone(habitId)
                    ACTION_SNOOZE -> dispatcher.snooze(habitId)
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_DONE = "com.streak.app.reminder.DONE"
        const val ACTION_SNOOZE = "com.streak.app.reminder.SNOOZE"
        const val EXTRA_HABIT_ID = "habit_id"
    }
}
