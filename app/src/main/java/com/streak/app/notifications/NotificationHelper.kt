package com.streak.app.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.streak.app.MainActivity
import com.streak.app.R
import com.streak.app.domain.model.GoalType
import com.streak.app.domain.model.HabitWithStreak
import com.streak.app.ui.components.formatAmount
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

// Kanali in gradnja obvestil. Na zaklenjenem zaslonu je vedno javna razlicica brez imen;
// z nastavitvijo "Skrij imena" so imena skrita tudi v odklenjenem stanju.
@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_REMINDERS = "reminders"
        const val CHANNEL_NUDGE = "nudge"
        const val CHANNEL_BRIEFING = "briefing"
        const val CHANNEL_SESSION = "session"
        const val CHANNEL_SESSION_ALERTS = "session_alerts"

        private const val GROUP_REMINDERS = "com.streak.app.REMINDERS"
        private const val TAG_HABIT = "habit:"
        private const val ID_REMINDER = 1
        private const val ID_BRIEFING = 9000
        private const val ID_NUDGE = 9001
        private const val ID_GROUP_SUMMARY = 9002
    }

    private val nm get() = NotificationManagerCompat.from(context)

    fun createChannels() {
        val manager = context.getSystemService(NotificationManager::class.java)
        // Stari kanal iz prve razlicice (escalation) ni vec v uporabi
        manager.deleteNotificationChannel("escalation")
        val channels = listOf(
            NotificationChannel(CHANNEL_REMINDERS, context.getString(R.string.notif_channel_reminders), NotificationManager.IMPORTANCE_HIGH),
            NotificationChannel(CHANNEL_NUDGE, context.getString(R.string.notif_channel_nudge), NotificationManager.IMPORTANCE_DEFAULT),
            NotificationChannel(CHANNEL_BRIEFING, context.getString(R.string.notif_channel_briefing), NotificationManager.IMPORTANCE_LOW),
            // Tekoca seja: tiho, brez znacke
            NotificationChannel(CHANNEL_SESSION, context.getString(R.string.notif_channel_session), NotificationManager.IMPORTANCE_LOW)
                .apply { setShowBadge(false) },
            NotificationChannel(CHANNEL_SESSION_ALERTS, context.getString(R.string.notif_channel_session_alerts), NotificationManager.IMPORTANCE_HIGH)
        )
        manager.createNotificationChannels(channels)
    }

    fun enabled(): Boolean = nm.areNotificationsEnabled()

    // Opomnik za navado z dejanji "Opravljeno" (ali "Zacni sejo") in "Cez 15 min"
    fun showReminder(item: HabitWithStreak, hideNames: Boolean, silent: Boolean) {
        if (!nm.areNotificationsEnabled()) return
        val habit = item.habit
        val title = if (hideNames) context.getString(R.string.notif_reminder_title) else habit.title
        val text = if (hideNames) context.getString(R.string.notif_reminder_hidden_text) else reminderText(item)
        val builder = base(CHANNEL_REMINDERS, title, text)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setGroup(GROUP_REMINDERS)
            .setSilent(silent)
            .setContentIntent(openApp(habit.id.hashCode()))
        if (habit.goalType == GoalType.DURATION) {
            builder.addAction(0, context.getString(R.string.start_session), startSession(habit.id))
        } else {
            builder.addAction(0, context.getString(R.string.notif_action_done), action(NotificationActionReceiver.ACTION_DONE, habit.id))
        }
        builder.addAction(0, context.getString(R.string.notif_action_snooze, AlarmScheduler.SNOOZE_MINUTES), action(NotificationActionReceiver.ACTION_SNOOZE, habit.id))
        runCatching {
            nm.notify(TAG_HABIT + habit.id, ID_REMINDER, builder.build())
            nm.notify(ID_GROUP_SUMMARY, groupSummary())
        }
    }

    // Z zadnjim opomnikom izgine tudi povzetek skupine
    fun cancelReminder(habitId: String) {
        nm.cancel(TAG_HABIT + habitId, ID_REMINDER)
        val active = context.getSystemService(NotificationManager::class.java).activeNotifications
        if (active.none { it.tag?.startsWith(TAG_HABIT) == true }) nm.cancel(ID_GROUP_SUMMARY)
    }

    fun showBriefing(text: String) {
        if (!nm.areNotificationsEnabled()) return
        val n = base(CHANNEL_BRIEFING, context.getString(R.string.notif_briefing_title), text)
            .setContentIntent(openApp(ID_BRIEFING))
            .build()
        runCatching { nm.notify(ID_BRIEFING, n) }
    }

    // silent: posodobitev ze prikazanega opomnika (po napredku) brez ponovnega zvoka
    fun showNudge(text: String, silent: Boolean = false) {
        if (!nm.areNotificationsEnabled()) return
        val n = base(CHANNEL_NUDGE, context.getString(R.string.notif_nudge_title), text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setSilent(silent)
            .setContentIntent(openApp(ID_NUDGE))
            .build()
        runCatching { nm.notify(ID_NUDGE, n) }
    }

    fun cancelNudge() = nm.cancel(ID_NUDGE)

    fun isNudgeShown(): Boolean =
        context.getSystemService(NotificationManager::class.java).activeNotifications.any { it.id == ID_NUDGE && it.tag == null }

    private fun base(channel: String, title: String, text: String) = NotificationCompat.Builder(context, channel)
        .setSmallIcon(R.drawable.ic_stat_habit)
        .setContentTitle(title)
        .setContentText(text)
        .setAutoCancel(true)
        .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
        .setPublicVersion(publicVersion(channel))

    // Zaklenjen zaslon: brez imen navad
    private fun publicVersion(channel: String): Notification = NotificationCompat.Builder(context, channel)
        .setSmallIcon(R.drawable.ic_stat_habit)
        .setContentTitle(context.getString(R.string.notif_reminder_title))
        .build()

    private fun groupSummary(): Notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
        .setSmallIcon(R.drawable.ic_stat_habit)
        .setGroup(GROUP_REMINDERS)
        .setGroupSummary(true)
        .setSilent(true)
        .setAutoCancel(true)
        .build()

    // "Se 12 strani do cilja", "Cilj danes: 10 min" ali opis navade
    private fun reminderText(item: HabitWithStreak): String {
        val habit = item.habit
        val done = item.completionOn(item.today)?.value ?: 0.0
        return when (habit.goalType) {
            GoalType.QUANTITY -> context.getString(
                R.string.notif_reminder_quantity,
                formatAmount((habit.targetValue - done).coerceAtLeast(0.0)),
                habit.unit.orEmpty()
            ).trim()
            GoalType.DURATION -> context.getString(R.string.notif_reminder_duration, (habit.targetValue / 60).toInt())
            GoalType.YES_NO -> habit.description.ifBlank { context.getString(R.string.notif_reminder_default) }
        }
    }

    private fun openApp(requestCode: Int): PendingIntent = PendingIntent.getActivity(
        context, requestCode,
        Intent(context, MainActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun startSession(habitId: String): PendingIntent = PendingIntent.getActivity(
        context, habitId.hashCode(),
        Intent(context, MainActivity::class.java)
            .setAction(MainActivity.ACTION_START_SESSION)
            .putExtra(MainActivity.EXTRA_HABIT_ID, habitId)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun action(action: String, habitId: String): PendingIntent = PendingIntent.getBroadcast(
        context, (action + habitId).hashCode(),
        Intent(context, NotificationActionReceiver::class.java)
            .setAction(action)
            .putExtra(NotificationActionReceiver.EXTRA_HABIT_ID, habitId),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}
