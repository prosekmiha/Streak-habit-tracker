package com.streak.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.streak.app.di.ApplicationScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

// Sprozen opomnik naloge
@AndroidEntryPoint
class TaskReminderReceiver : BroadcastReceiver() {

    @Inject lateinit var dispatcher: TaskReminderDispatcher
    @Inject @ApplicationScope lateinit var scope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return
        val occurrence = intent.getLongExtra(EXTRA_OCCURRENCE, Long.MIN_VALUE)
            .takeIf { it != Long.MIN_VALUE }?.let(LocalDate::ofEpochDay)
        val pending = goAsync()
        scope.launch {
            try {
                dispatcher.onReminder(taskId, occurrence)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_OCCURRENCE = "occurrence"
    }
}
