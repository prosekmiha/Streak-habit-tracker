package com.streak.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.streak.app.di.ApplicationScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

// Jutranji povzetek in vecerni opomnik ob nastavljenih urah
@AndroidEntryPoint
class DailyNotificationReceiver : BroadcastReceiver() {

    @Inject lateinit var dispatcher: ReminderDispatcher
    @Inject @ApplicationScope lateinit var scope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        scope.launch {
            try {
                when (intent.action) {
                    ACTION_BRIEFING -> dispatcher.onBriefing()
                    ACTION_NUDGE -> dispatcher.onNudge()
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_BRIEFING = "com.streak.app.daily.BRIEFING"
        const val ACTION_NUDGE = "com.streak.app.daily.NUDGE"
    }
}
