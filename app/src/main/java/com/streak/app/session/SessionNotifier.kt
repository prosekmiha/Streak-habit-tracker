package com.streak.app.session

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.streak.app.MainActivity
import com.streak.app.R
import com.streak.app.di.ApplicationScope
import com.streak.app.domain.repository.SettingsRepository
import com.streak.app.domain.session.ActiveSession
import com.streak.app.domain.session.TimePoint
import com.streak.app.notifications.NotificationHelper
import com.streak.app.ui.components.formatClock
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Singleton

// Trajno obvestilo tekoce seje (stoparica, Premor/Nadaljuj, Koncaj) in opozorilo ob cilju
@Singleton
class SessionNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
    settings: SettingsRepository,
    @ApplicationScope scope: CoroutineScope
) {
    @Volatile private var hideNames = false

    init {
        settings.settings.onEach { hideNames = it.hideNamesInNotifications }.launchIn(scope)
    }

    fun build(session: ActiveSession, now: TimePoint): Notification {
        val elapsed = session.elapsedMs(now)
        val title = if (hideNames) context.getString(R.string.notif_session_generic) else session.title
        val builder = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_SESSION)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setVisibility(if (hideNames) NotificationCompat.VISIBILITY_PRIVATE else NotificationCompat.VISIBILITY_PUBLIC)
            .setPublicVersion(publicVersion())
            .setContentIntent(openIntent(session.habitId))
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        if (session.running) {
            builder.setUsesChronometer(true)
                .setShowWhen(true)
                .setWhen(System.currentTimeMillis() - elapsed)
                .setContentText(goalText(session) ?: context.getString(R.string.notif_session_running))
        } else {
            builder.setShowWhen(false)
                .setContentText(context.getString(R.string.notif_session_paused, formatClock(elapsed / 1000)))
        }
        builder.addAction(
            0,
            context.getString(if (session.running) R.string.pause else R.string.resume),
            actionIntent(SessionReceiver.ACTION_TOGGLE)
        )
        builder.addAction(0, context.getString(R.string.finish), actionIntent(SessionReceiver.ACTION_FINISH))
        return builder.build()
    }

    fun show(session: ActiveSession, now: TimePoint) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        runCatching { NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, build(session, now)) }
    }

    // Seja je koncana: pospravi tudi opozorilo o cilju
    fun cancel() {
        val nm = NotificationManagerCompat.from(context)
        nm.cancel(NOTIFICATION_ID)
        nm.cancel(GOAL_NOTIFICATION_ID)
    }

    fun showGoalReached(session: ActiveSession) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        val text = context.getString(R.string.notif_goal_text, session.targetSeconds / 60)
        val n = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_SESSION_ALERTS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(if (hideNames) context.getString(R.string.notif_goal_title) else context.getString(R.string.notif_goal_title_named, session.title))
            .setContentText(text)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openIntent(session.habitId))
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(GOAL_NOTIFICATION_ID, n) }
    }

    // Na zaklenjenem zaslonu brez imena navade
    private fun publicVersion(): Notification =
        NotificationCompat.Builder(context, NotificationHelper.CHANNEL_SESSION)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notif_session_generic))
            .build()

    private fun goalText(session: ActiveSession): String? =
        if (session.targetSeconds > 0) context.getString(R.string.session_goal, session.targetSeconds / 60) else null

    private fun openIntent(habitId: String): PendingIntent = PendingIntent.getActivity(
        context, OPEN_REQUEST,
        Intent(context, MainActivity::class.java)
            .setAction(MainActivity.ACTION_OPEN_SESSION)
            .putExtra(MainActivity.EXTRA_HABIT_ID, habitId)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun actionIntent(action: String): PendingIntent = PendingIntent.getBroadcast(
        context, action.hashCode(),
        Intent(context, SessionReceiver::class.java).setAction(action),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    companion object {
        const val NOTIFICATION_ID = 7300
        const val GOAL_NOTIFICATION_ID = 7302
        private const val OPEN_REQUEST = 7303
    }
}
