package com.streak.app.domain.backup

import java.time.Duration
import java.time.Instant

// Kdaj opomniti na varnostno kopijo: 30 dni po zadnji, prvic pa, ko so navade stare
// 14 dni (prej ni veliko za izgubiti). "Kasneje" odlozi opomnik.
object BackupReminder {
    val INTERVAL: Duration = Duration.ofDays(30)
    val FIRST_AFTER: Duration = Duration.ofDays(14)
    val SNOOZE: Duration = Duration.ofDays(7)

    fun isDue(
        now: Instant,
        enabled: Boolean,
        lastBackup: Instant?,
        snoozedUntil: Instant?,
        oldestHabitCreatedAt: Instant?
    ): Boolean {
        if (!enabled || oldestHabitCreatedAt == null) return false
        if (snoozedUntil != null && now.isBefore(snoozedUntil)) return false
        val since = lastBackup ?: return Duration.between(oldestHabitCreatedAt, now) >= FIRST_AFTER
        return Duration.between(since, now) >= INTERVAL
    }
}
