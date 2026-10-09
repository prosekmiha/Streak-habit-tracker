package com.streak.app.domain.model

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

data class AppSettings(
    val userName: String = "",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    // Logicni dan se konca ob tej uri (00:00 - 06:00)
    val dayEndsAt: LocalTime = LocalTime.MIDNIGHT,
    val firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    val animationLevel: AnimationLevel = AnimationLevel.FULL,
    val onboardingDone: Boolean = false,
    // Zadnji znani logicni dan (za zaznavo skokov ure)
    val lastKnownDate: LocalDate? = null,
    val hideNamesInNotifications: Boolean = false,
    val hideWidgetDetails: Boolean = false,
    val quietHoursStart: LocalTime? = null,
    val quietHoursEnd: LocalTime? = null,
    // 0 = brez omejitve
    val dailyNotificationCap: Int = 0,
    val lastBackupAt: Instant? = null,
    val backupReminderEnabled: Boolean = true
) {
    companion object {
        val MAX_DAY_ENDS_AT: LocalTime = LocalTime.of(6, 0)
    }
}
