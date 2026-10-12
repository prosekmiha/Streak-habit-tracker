package com.streak.app.domain.backup

import kotlinx.serialization.Serializable

// Oblika datoteke varnostne kopije. Je stabilna in neodvisna od sheme baze: nova polja
// imajo privzete vrednosti, neznana polja se pri uvozu prezrejo.
@Serializable
data class BackupFile(
    val format: String = FORMAT_PLAIN,
    val schemaVersion: Int = SCHEMA_VERSION,
    // ISO-8601 trenutek nastanka
    val createdAt: String,
    val appVersion: String = "",
    val habits: List<BHabit> = emptyList(),
    val completions: List<BCompletion> = emptyList(),
    val sessions: List<BSession> = emptyList(),
    val vacations: List<BVacation> = emptyList(),
    val tasks: List<BTask> = emptyList(),
    // Od sheme 3
    val moods: List<BMood> = emptyList(),
    // Nastavitve brez varnostnih podatkov (koda za zaklep se nikoli ne izvozi)
    val settings: BSettings? = null
) {
    companion object {
        const val FORMAT_PLAIN = "streak-backup"
        const val FORMAT_ENCRYPTED = "streak-backup-encrypted"
        const val SCHEMA_VERSION = 3
    }
}

@Serializable
data class BHabit(
    val id: String,
    val title: String,
    val description: String = "",
    val icon: String,
    val colorHex: String,
    val category: String = "NONE",
    val goalType: String = "YES_NO",
    val targetValue: Double = 1.0,
    val unit: String? = null,
    val scheduleType: String = "DAILY",
    // ISO dnevi v tednu (1 = ponedeljek)
    val weekdays: List<Int> = (1..7).toList(),
    val timesPerPeriod: Int = 1,
    val timeOfDay: String = "ANYTIME",
    val reminders: List<BReminder> = emptyList(),
    val startDate: String,
    val endDate: String? = null,
    val archivedAt: Long? = null,
    val createdAt: Long,
    val sortOrder: Int = 0
)

@Serializable
data class BReminder(val time: String, val enabled: Boolean = true)

@Serializable
data class BCompletion(
    val id: String,
    val habitId: String,
    val date: String,
    val status: String,
    val value: Double,
    val loggedAt: Long,
    val zoneOffsetSeconds: Int = 0,
    val note: String? = null,
    val manuallyEdited: Boolean = false
)

@Serializable
data class BSession(
    val id: String,
    val habitId: String,
    val date: String,
    val startedAt: Long,
    val endedAt: Long,
    val durationSeconds: Long
)

@Serializable
data class BMood(
    val date: String,
    val mood: Int,
    val energy: Int? = null,
    val note: String? = null,
    val loggedAt: Long = 0
)

@Serializable
data class BVacation(val startDate: String, val endDate: String)

@Serializable
data class BTask(
    val id: String,
    val title: String,
    val dueDate: String? = null,
    val dueMinuteOfDay: Int? = null,
    val priority: String = "NORMAL",
    val isDone: Boolean = false,
    val isRecurring: Boolean = false,
    val recurrenceRule: String? = null,
    val createdAt: Long,
    val reminders: List<BTaskReminder> = emptyList()
)

// kind: BEFORE (minutes), DAY_AT (days, time), REPEAT (minutes, time-until); ure v ISO obliki
@Serializable
data class BTaskReminder(
    val kind: String,
    val minutes: Int = 0,
    val days: Int = 0,
    val time: String? = null,
    val until: String? = null
)

@Serializable
data class BSettings(
    val userName: String = "",
    val themeMode: String = "SYSTEM",
    val dayEndsAtMinute: Int = 0,
    val firstDayOfWeek: Int = 1,
    val animationLevel: String = "FULL",
    val hideNamesInNotifications: Boolean = false,
    val quietHoursStartMinute: Int? = null,
    val quietHoursEndMinute: Int? = null,
    val dailyNotificationCap: Int = 0,
    val briefingEnabled: Boolean = true,
    val briefingMinute: Int = 8 * 60,
    val nudgeEnabled: Boolean = true,
    val nudgeMinute: Int = 20 * 60
)

// Sifrirana ovojnica: AES-256-GCM, kljuc iz gesla s PBKDF2-HMAC-SHA256
@Serializable
data class EncryptedBackup(
    val format: String = BackupFile.FORMAT_ENCRYPTED,
    val version: Int = 1,
    val kdf: String = "PBKDF2WithHmacSHA256",
    val iterations: Int,
    val salt: String,
    val iv: String,
    val data: String
)
