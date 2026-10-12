package com.streak.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.streak.app.domain.model.AnimationLevel
import com.streak.app.domain.model.AppSettings
import com.streak.app.domain.model.ThemeMode
import com.streak.app.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.WeekFields
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {

    private object Keys {
        val NAME = stringPreferencesKey("user_name")
        val THEME = stringPreferencesKey("theme_mode")
        val DAY_ENDS_AT = intPreferencesKey("day_ends_at_minute")
        val FIRST_DAY = intPreferencesKey("first_day_of_week")
        val ANIMATIONS = stringPreferencesKey("animation_level")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val LAST_KNOWN_DATE = longPreferencesKey("last_known_epoch_day")
        val MOOD_DISMISSED_DAY = longPreferencesKey("mood_prompt_dismissed_epoch_day")
        val TILE_HABIT = stringPreferencesKey("tile_habit_id")
        val TILE_SESSION_HABIT = stringPreferencesKey("tile_session_habit_id")
        val HIDE_NOTIF_NAMES = booleanPreferencesKey("hide_notification_names")
        val HIDE_WIDGET_DETAILS = booleanPreferencesKey("hide_widget_details")
        val QUIET_START = intPreferencesKey("quiet_hours_start_minute")
        val QUIET_END = intPreferencesKey("quiet_hours_end_minute")
        val NOTIF_CAP = intPreferencesKey("daily_notification_cap")
        val BRIEFING_ON = booleanPreferencesKey("briefing_enabled")
        val BRIEFING_AT = intPreferencesKey("briefing_minute")
        val NUDGE_ON = booleanPreferencesKey("nudge_enabled")
        val NUDGE_AT = intPreferencesKey("nudge_minute")
        val LAST_BACKUP = longPreferencesKey("last_backup_millis")
        val BACKUP_REMINDER = booleanPreferencesKey("backup_reminder_enabled")
        val BACKUP_SNOOZE = longPreferencesKey("backup_reminder_snoozed_until")
        val CLOUD_SNAPSHOT = booleanPreferencesKey("cloud_snapshot_enabled")
        val PRIVACY_SCREEN = booleanPreferencesKey("privacy_screen")
    }

    // Privzeti prvi dan tedna po jeziku naprave, dokler ga uporabnik ne nastavi
    private val localeFirstDay: DayOfWeek get() = WeekFields.of(Locale.getDefault()).firstDayOfWeek

    override val settings: Flow<AppSettings> = context.dataStore.data
        .map { it.toSettings() }
        .distinctUntilChanged()

    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        context.dataStore.edit { prefs ->
            val updated = transform(prefs.toSettings())
            prefs.write(updated)
        }
    }

    private fun Preferences.toSettings() = AppSettings(
        userName = this[Keys.NAME] ?: "",
        themeMode = enumOrDefault(this[Keys.THEME], ThemeMode.SYSTEM),
        dayEndsAt = this[Keys.DAY_ENDS_AT]?.let(::minuteToTime) ?: LocalTime.MIDNIGHT,
        firstDayOfWeek = this[Keys.FIRST_DAY]?.let { DayOfWeek.of(it) } ?: localeFirstDay,
        animationLevel = enumOrDefault(this[Keys.ANIMATIONS], AnimationLevel.FULL),
        onboardingDone = this[Keys.ONBOARDING_DONE] ?: false,
        lastKnownDate = this[Keys.LAST_KNOWN_DATE]?.let { LocalDate.ofEpochDay(it) },
        moodPromptDismissedDay = this[Keys.MOOD_DISMISSED_DAY]?.let { LocalDate.ofEpochDay(it) },
        tileHabitId = this[Keys.TILE_HABIT],
        tileSessionHabitId = this[Keys.TILE_SESSION_HABIT],
        hideNamesInNotifications = this[Keys.HIDE_NOTIF_NAMES] ?: false,
        hideWidgetDetails = this[Keys.HIDE_WIDGET_DETAILS] ?: false,
        quietHoursStart = this[Keys.QUIET_START]?.let(::minuteToTime),
        quietHoursEnd = this[Keys.QUIET_END]?.let(::minuteToTime),
        dailyNotificationCap = this[Keys.NOTIF_CAP] ?: 0,
        briefingEnabled = this[Keys.BRIEFING_ON] ?: true,
        briefingTime = this[Keys.BRIEFING_AT]?.let(::minuteToTime) ?: LocalTime.of(8, 0),
        nudgeEnabled = this[Keys.NUDGE_ON] ?: true,
        nudgeTime = this[Keys.NUDGE_AT]?.let(::minuteToTime) ?: LocalTime.of(20, 0),
        lastBackupAt = this[Keys.LAST_BACKUP]?.let { Instant.ofEpochMilli(it) },
        backupReminderEnabled = this[Keys.BACKUP_REMINDER] ?: true,
        backupReminderSnoozedUntil = this[Keys.BACKUP_SNOOZE]?.let { Instant.ofEpochMilli(it) },
        cloudSnapshotEnabled = this[Keys.CLOUD_SNAPSHOT] ?: true,
        privacyScreen = this[Keys.PRIVACY_SCREEN] ?: false
    )

    private fun MutablePreferences.write(s: AppSettings) {
        this[Keys.NAME] = s.userName
        this[Keys.THEME] = s.themeMode.name
        this[Keys.DAY_ENDS_AT] = timeToMinute(s.dayEndsAt.coerceAtMost(AppSettings.MAX_DAY_ENDS_AT))
        this[Keys.FIRST_DAY] = s.firstDayOfWeek.value
        this[Keys.ANIMATIONS] = s.animationLevel.name
        this[Keys.ONBOARDING_DONE] = s.onboardingDone
        setOrRemove(Keys.LAST_KNOWN_DATE, s.lastKnownDate?.toEpochDay())
        setOrRemove(Keys.MOOD_DISMISSED_DAY, s.moodPromptDismissedDay?.toEpochDay())
        setOrRemove(Keys.TILE_HABIT, s.tileHabitId)
        setOrRemove(Keys.TILE_SESSION_HABIT, s.tileSessionHabitId)
        this[Keys.HIDE_NOTIF_NAMES] = s.hideNamesInNotifications
        this[Keys.HIDE_WIDGET_DETAILS] = s.hideWidgetDetails
        setOrRemove(Keys.QUIET_START, s.quietHoursStart?.let(::timeToMinute))
        setOrRemove(Keys.QUIET_END, s.quietHoursEnd?.let(::timeToMinute))
        this[Keys.NOTIF_CAP] = s.dailyNotificationCap.coerceAtLeast(0)
        this[Keys.BRIEFING_ON] = s.briefingEnabled
        this[Keys.BRIEFING_AT] = timeToMinute(s.briefingTime)
        this[Keys.NUDGE_ON] = s.nudgeEnabled
        this[Keys.NUDGE_AT] = timeToMinute(s.nudgeTime)
        setOrRemove(Keys.LAST_BACKUP, s.lastBackupAt?.toEpochMilli())
        this[Keys.BACKUP_REMINDER] = s.backupReminderEnabled
        setOrRemove(Keys.BACKUP_SNOOZE, s.backupReminderSnoozedUntil?.toEpochMilli())
        this[Keys.CLOUD_SNAPSHOT] = s.cloudSnapshotEnabled
        this[Keys.PRIVACY_SCREEN] = s.privacyScreen
    }

    private fun <T : Any> MutablePreferences.setOrRemove(key: Preferences.Key<T>, value: T?) {
        if (value == null) remove(key) else this[key] = value
    }

    private inline fun <reified E : Enum<E>> enumOrDefault(name: String?, default: E): E =
        name?.let { runCatching { enumValueOf<E>(it) }.getOrNull() } ?: default

    private fun minuteToTime(m: Int): LocalTime = LocalTime.of(m / 60 % 24, m % 60)
    private fun timeToMinute(t: LocalTime): Int = t.hour * 60 + t.minute
}
