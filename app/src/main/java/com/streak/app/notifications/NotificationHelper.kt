package com.streak.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.streak.app.MainActivity
import com.streak.app.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_REMINDERS = "reminders"
        const val CHANNEL_BRIEFING = "briefing"
        const val CHANNEL_SESSION = "session"
        const val CHANNEL_SESSION_ALERTS = "session_alerts"
    }

    fun createChannels() {
        val nm = context.getSystemService(NotificationManager::class.java)
        val reminders = NotificationChannel(
            CHANNEL_REMINDERS,
            context.getString(R.string.notif_channel_reminders),
            NotificationManager.IMPORTANCE_HIGH
        )
        val briefing = NotificationChannel(
            CHANNEL_BRIEFING,
            context.getString(R.string.notif_channel_briefing),
            NotificationManager.IMPORTANCE_DEFAULT
        )
        // Tekoca seja: tiho, brez znacke
        val session = NotificationChannel(
            CHANNEL_SESSION,
            context.getString(R.string.notif_channel_session),
            NotificationManager.IMPORTANCE_LOW
        ).apply { setShowBadge(false) }
        val sessionAlerts = NotificationChannel(
            CHANNEL_SESSION_ALERTS,
            context.getString(R.string.notif_channel_session_alerts),
            NotificationManager.IMPORTANCE_HIGH
        )
        nm.createNotificationChannels(listOf(reminders, briefing, session, sessionAlerts))
    }

    private fun contentIntent(notifId: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            context,
            notifId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    // Prikaze opomnik za navado z naprej narascajoco nujnostjo (escalation)
    fun showReminder(notifId: Int, habitTitle: String, escalationLevel: Int) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return

        val title = context.getString(R.string.notif_reminder_title)
        val builder = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(habitTitle)
            .setAutoCancel(true)
            .setContentIntent(contentIntent(notifId))

        when (escalationLevel) {
            0 -> builder.setPriority(NotificationCompat.PRIORITY_DEFAULT).setSilent(true)
            1 -> builder.setPriority(NotificationCompat.PRIORITY_HIGH).setDefaults(NotificationCompat.DEFAULT_SOUND)
            else -> builder.setPriority(NotificationCompat.PRIORITY_MAX)
                .setOngoing(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
        }

        runCatching { NotificationManagerCompat.from(context).notify(notifId, builder.build()) }
    }

    fun showBriefing(text: String) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        val builder = NotificationCompat.Builder(context, CHANNEL_BRIEFING)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.notif_briefing_title))
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(contentIntent(9000))
        runCatching { NotificationManagerCompat.from(context).notify(9000, builder.build()) }
    }

    fun cancel(notifId: Int) {
        NotificationManagerCompat.from(context).cancel(notifId)
    }
}
