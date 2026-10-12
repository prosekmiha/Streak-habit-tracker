package com.streak.app.session

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.streak.app.di.ApplicationScope
import com.streak.app.domain.session.SessionController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

// Dejanja iz obvestila seje in alarm za dosezen cilj
@AndroidEntryPoint
class SessionReceiver : BroadcastReceiver() {

    @Inject lateinit var controller: SessionController
    @Inject @ApplicationScope lateinit var scope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        scope.launch {
            try {
                when (intent.action) {
                    ACTION_TOGGLE -> controller.togglePause()
                    ACTION_FINISH -> controller.finish(save = true)
                    ACTION_GOAL -> controller.onGoalReached()
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_TOGGLE = "com.streak.app.session.TOGGLE"
        const val ACTION_FINISH = "com.streak.app.session.FINISH"
        const val ACTION_GOAL = "com.streak.app.session.GOAL"
    }
}
