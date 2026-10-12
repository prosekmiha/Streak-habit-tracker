package com.streak.app.domain.backup

import com.streak.app.domain.model.AnimationLevel
import com.streak.app.domain.model.AppSettings
import com.streak.app.domain.model.Completion
import com.streak.app.domain.model.CompletionStatus
import com.streak.app.domain.model.GoalType
import com.streak.app.domain.model.Habit
import com.streak.app.domain.model.HabitCategory
import com.streak.app.domain.model.HabitSchedule
import com.streak.app.domain.model.HabitSession
import com.streak.app.domain.model.MoodEntry
import com.streak.app.domain.model.Priority
import com.streak.app.domain.model.Reminder
import com.streak.app.domain.model.ScheduleType
import com.streak.app.domain.model.Task
import com.streak.app.domain.model.TaskReminder
import com.streak.app.domain.model.ThemeMode
import com.streak.app.domain.model.TimeOfDay
import com.streak.app.domain.model.Vacation
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

// Vsebina varnostne kopije v domenskih modelih
data class BackupContents(
    val habits: List<Habit> = emptyList(),
    val completions: List<Completion> = emptyList(),
    val sessions: List<HabitSession> = emptyList(),
    val vacations: List<Vacation> = emptyList(),
    val tasks: List<Task> = emptyList(),
    val moods: List<MoodEntry> = emptyList(),
    val settings: BSettings? = null
)

// Povzetek za predogled pred uvozom
data class BackupSummary(
    val createdAt: Instant?,
    val habits: Int,
    val completions: Int,
    val sessions: Int,
    val tasks: Int,
    val firstDate: LocalDate?,
    val lastDate: LocalDate?,
    // Zapisi, ki kazejo na neobstojeco navado ali so neveljavni (ne uvozijo se)
    val skipped: Int,
    val moods: Int = 0
)

sealed interface ParseResult {
    data class Ok(val contents: BackupContents, val summary: BackupSummary) : ParseResult
    data object NeedsPassword : ParseResult
    data object WrongPassword : ParseResult
    data object NotABackup : ParseResult
    data object NewerVersion : ParseResult
    data object Corrupt : ParseResult
}

// Pretvorba med domenskimi modeli in datoteko varnostne kopije, s preverjanjem ob uvozu
class BackupCodec(private val crypto: BackupCrypto = BackupCrypto()) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    fun encode(contents: BackupContents, createdAt: Instant, appVersion: String, password: CharArray? = null): String {
        val file = BackupFile(
            createdAt = createdAt.toString(),
            appVersion = appVersion,
            habits = contents.habits.map { it.toBackup() },
            completions = contents.completions.map { it.toBackup() },
            sessions = contents.sessions.map { it.toBackup() },
            vacations = contents.vacations.map { BVacation(it.startDate.toString(), it.endDate.toString()) },
            tasks = contents.tasks.map { it.toBackup() },
            moods = contents.moods.map { it.toBackup() },
            settings = contents.settings
        )
        val plain = json.encodeToString(BackupFile.serializer(), file)
        if (password == null || password.isEmpty()) return plain
        return json.encodeToString(EncryptedBackup.serializer(), crypto.encrypt(plain.toByteArray(), password))
    }

    fun isEncrypted(text: String): Boolean = formatOf(text) == BackupFile.FORMAT_ENCRYPTED

    fun parse(text: String, password: CharArray? = null): ParseResult {
        val plain = when (formatOf(text)) {
            BackupFile.FORMAT_PLAIN -> text
            BackupFile.FORMAT_ENCRYPTED -> {
                if (password == null || password.isEmpty()) return ParseResult.NeedsPassword
                val envelope = runCatching { json.decodeFromString(EncryptedBackup.serializer(), text) }.getOrNull()
                    ?: return ParseResult.Corrupt
                val bytes = runCatching { crypto.decrypt(envelope, password) }.getOrElse { return ParseResult.Corrupt }
                    ?: return ParseResult.WrongPassword
                bytes.decodeToString()
            }
            else -> return ParseResult.NotABackup
        }
        val file = try {
            json.decodeFromString(BackupFile.serializer(), plain)
        } catch (_: SerializationException) {
            return ParseResult.Corrupt
        } catch (_: IllegalArgumentException) {
            return ParseResult.Corrupt
        }
        if (file.schemaVersion > BackupFile.SCHEMA_VERSION) return ParseResult.NewerVersion
        return try {
            toContents(file)
        } catch (_: RuntimeException) {
            ParseResult.Corrupt
        }
    }

    private fun formatOf(text: String): String? = runCatching {
        json.parseToJsonElement(text).jsonObject["format"]?.jsonPrimitive?.content
    }.getOrNull()

    private fun toContents(file: BackupFile): ParseResult.Ok {
        val habits = file.habits.map { it.toDomain() }.distinctBy { it.id }
        val ids = habits.map { it.id }.toSet()
        val completions = file.completions.map { it.toDomain() }
        val sessions = file.sessions.map { it.toDomain() }
        val validCompletions = completions.filter { it.habitId in ids }.distinctBy { it.habitId to it.date }
        val validSessions = sessions.filter { it.habitId in ids }.distinctBy { it.id }
        val moods = file.moods.mapNotNull { runCatching { it.toDomain() }.getOrNull() }.distinctBy { it.date }
        val contents = BackupContents(
            habits = habits,
            completions = validCompletions,
            sessions = validSessions,
            vacations = file.vacations.map { Vacation(startDate = LocalDate.parse(it.startDate), endDate = LocalDate.parse(it.endDate)) },
            tasks = file.tasks.map { it.toDomain() }.distinctBy { it.id },
            moods = moods,
            settings = file.settings
        )
        val dates = validCompletions.map { it.date }
        val summary = BackupSummary(
            createdAt = runCatching { Instant.parse(file.createdAt) }.getOrNull(),
            habits = habits.size,
            completions = validCompletions.size,
            sessions = validSessions.size,
            tasks = contents.tasks.size,
            firstDate = dates.minOrNull(),
            lastDate = dates.maxOrNull(),
            skipped = (completions.size - validCompletions.size) + (sessions.size - validSessions.size) +
                (file.moods.size - moods.size),
            moods = moods.size
        )
        return ParseResult.Ok(contents, summary)
    }

    // --- preslikave ---

    private fun Habit.toBackup() = BHabit(
        id = id, title = title, description = description, icon = icon, colorHex = colorHex,
        category = category.name, goalType = goalType.name, targetValue = targetValue, unit = unit,
        scheduleType = schedule.type.name, weekdays = schedule.weekdays.map { it.value }.sorted(),
        timesPerPeriod = schedule.timesPerPeriod, timeOfDay = timeOfDay.name,
        reminders = reminders.map { BReminder(it.time.toString(), it.enabled) },
        startDate = startDate.toString(), endDate = endDate?.toString(), archivedAt = archivedAt,
        createdAt = createdAt, sortOrder = sortOrder
    )

    private fun BHabit.toDomain(): Habit {
        require(id.isNotBlank() && title.isNotBlank())
        return Habit(
            id = id, title = title, description = description, icon = icon, colorHex = colorHex,
            category = enumValueOf<HabitCategory>(category), goalType = enumValueOf<GoalType>(goalType),
            targetValue = targetValue.also { require(it.isFinite() && it >= 0) }, unit = unit,
            schedule = HabitSchedule(
                enumValueOf<ScheduleType>(scheduleType),
                weekdays.map { DayOfWeek.of(it) }.toSet().ifEmpty { DayOfWeek.entries.toSet() },
                timesPerPeriod.coerceAtLeast(1)
            ),
            timeOfDay = enumValueOf<TimeOfDay>(timeOfDay),
            reminders = reminders.map { Reminder(time = LocalTime.parse(it.time), enabled = it.enabled) },
            startDate = LocalDate.parse(startDate), endDate = endDate?.let(LocalDate::parse),
            archivedAt = archivedAt, createdAt = createdAt, sortOrder = sortOrder
        )
    }

    private fun Completion.toBackup() = BCompletion(
        id = id, habitId = habitId, date = date.toString(), status = status.name, value = value,
        loggedAt = loggedAt.toEpochMilli(), zoneOffsetSeconds = zoneOffset.totalSeconds,
        note = note, manuallyEdited = manuallyEdited
    )

    private fun BCompletion.toDomain() = Completion(
        id = id, habitId = habitId, date = LocalDate.parse(date), status = enumValueOf<CompletionStatus>(status),
        value = value.also { require(it.isFinite()) }, loggedAt = Instant.ofEpochMilli(loggedAt),
        zoneOffset = ZoneOffset.ofTotalSeconds(zoneOffsetSeconds), note = note, manuallyEdited = manuallyEdited
    )

    private fun HabitSession.toBackup() = BSession(
        id, habitId, date.toString(), startedAt.toEpochMilli(), endedAt.toEpochMilli(), durationSeconds
    )

    private fun BSession.toDomain() = HabitSession(
        id = id, habitId = habitId, date = LocalDate.parse(date), startedAt = Instant.ofEpochMilli(startedAt),
        endedAt = Instant.ofEpochMilli(endedAt), durationSeconds = durationSeconds.coerceAtLeast(0)
    )

    private fun MoodEntry.toBackup() = BMood(date.toString(), mood, energy, note, loggedAt.toEpochMilli())

    private fun BMood.toDomain() = MoodEntry(
        date = LocalDate.parse(date), mood = mood, energy = energy, note = note, loggedAt = Instant.ofEpochMilli(loggedAt)
    )

    private fun Task.toBackup() = BTask(
        id, title, dueDate?.toString(), dueMinuteOfDay, priority.name, isDone, isRecurring, recurrenceRule, createdAt,
        reminders.map { it.toBackup() }
    )

    private fun BTask.toDomain() = Task(
        id = id, title = title, dueDate = dueDate?.let(LocalDate::parse), dueMinuteOfDay = dueMinuteOfDay,
        priority = enumValueOf<Priority>(priority), isDone = isDone, isRecurring = isRecurring,
        recurrenceRule = recurrenceRule, createdAt = createdAt,
        // Neznane vrste (novejsa razlicica) se preskocijo
        reminders = reminders.mapNotNull { runCatching { it.toDomain() }.getOrNull() }.take(TaskReminder.MAX)
    )

    private fun TaskReminder.toBackup() = when (this) {
        is TaskReminder.BeforeDue -> BTaskReminder("BEFORE", minutes = minutes)
        is TaskReminder.DayAt -> BTaskReminder("DAY_AT", days = daysBefore, time = time.toString())
        is TaskReminder.Repeat -> BTaskReminder("REPEAT", minutes = everyMinutes, time = from.toString(), until = until.toString())
    }

    private fun BTaskReminder.toDomain(): TaskReminder? = when (kind) {
        "BEFORE" -> TaskReminder.BeforeDue(minutes.coerceAtLeast(0))
        "DAY_AT" -> TaskReminder.DayAt(days.coerceAtLeast(0), LocalTime.parse(time))
        "REPEAT" -> TaskReminder.Repeat(minutes.coerceAtLeast(TaskReminder.MIN_INTERVAL), LocalTime.parse(time), LocalTime.parse(until))
        else -> null
    }

    companion object {
        // Nastavitve brez varnostnih podatkov (koda za zaklep ni del AppSettings)
        fun settingsOf(s: AppSettings) = BSettings(
            userName = s.userName, themeMode = s.themeMode.name, dayEndsAtMinute = s.dayEndsAt.toMinute(),
            firstDayOfWeek = s.firstDayOfWeek.value, animationLevel = s.animationLevel.name,
            hideNamesInNotifications = s.hideNamesInNotifications,
            quietHoursStartMinute = s.quietHoursStart?.toMinute(), quietHoursEndMinute = s.quietHoursEnd?.toMinute(),
            dailyNotificationCap = s.dailyNotificationCap, briefingEnabled = s.briefingEnabled,
            briefingMinute = s.briefingTime.toMinute(), nudgeEnabled = s.nudgeEnabled, nudgeMinute = s.nudgeTime.toMinute()
        )

        fun applySettings(b: BSettings, to: AppSettings): AppSettings = to.copy(
            userName = b.userName,
            themeMode = runCatching { enumValueOf<ThemeMode>(b.themeMode) }.getOrDefault(to.themeMode),
            dayEndsAt = minute(b.dayEndsAtMinute).coerceAtMost(AppSettings.MAX_DAY_ENDS_AT),
            firstDayOfWeek = runCatching { DayOfWeek.of(b.firstDayOfWeek) }.getOrDefault(to.firstDayOfWeek),
            animationLevel = runCatching { enumValueOf<AnimationLevel>(b.animationLevel) }.getOrDefault(to.animationLevel),
            hideNamesInNotifications = b.hideNamesInNotifications,
            quietHoursStart = b.quietHoursStartMinute?.let(::minute),
            quietHoursEnd = b.quietHoursEndMinute?.let(::minute),
            dailyNotificationCap = b.dailyNotificationCap.coerceAtLeast(0),
            briefingEnabled = b.briefingEnabled, briefingTime = minute(b.briefingMinute),
            nudgeEnabled = b.nudgeEnabled, nudgeTime = minute(b.nudgeMinute),
            onboardingDone = true
        )

        private fun LocalTime.toMinute() = hour * 60 + minute
        private fun minute(m: Int): LocalTime = LocalTime.of((m / 60).mod(24), m.mod(60))
    }
}

// Spoj: lokalni podatki ostanejo, dodajo se manjkajoci. Pri zapisu za isto navado in dan
// zmaga kasnejsi vpis (loggedAt).
object BackupMerge {
    fun merge(local: BackupContents, incoming: BackupContents): BackupContents {
        val habitIds = local.habits.map { it.id }.toSet()
        val habits = local.habits + incoming.habits.filter { it.id !in habitIds }
        val completions = (local.completions + incoming.completions)
            .groupBy { it.habitId to it.date }
            .map { (_, list) -> list.maxBy { it.loggedAt } }
        val sessionIds = local.sessions.map { it.id }.toSet()
        val vacationRanges = local.vacations.map { it.startDate to it.endDate }.toSet()
        val taskIds = local.tasks.map { it.id }.toSet()
        val moods = (local.moods + incoming.moods).groupBy { it.date }.map { (_, list) -> list.maxBy { it.loggedAt } }
        return BackupContents(
            habits = habits,
            completions = completions,
            sessions = local.sessions + incoming.sessions.filter { it.id !in sessionIds },
            vacations = local.vacations + incoming.vacations.filter { (it.startDate to it.endDate) !in vacationRanges },
            tasks = local.tasks + incoming.tasks.filter { it.id !in taskIds },
            moods = moods,
            settings = local.settings
        )
    }
}

// Izvoz zapisov v CSV (za preglednice). RFC 4180: polja z vejico, narekovajem ali novo
// vrstico so v narekovajih, narekovaji se podvojijo.
object CompletionsCsv {
    private const val HEADER = "date,habit_id,habit,status,value,unit,note"

    fun build(habits: List<Habit>, completions: List<Completion>): String {
        val byId = habits.associateBy { it.id }
        val rows = completions.sortedWith(compareBy({ it.date }, { byId[it.habitId]?.title.orEmpty() })).map { c ->
            val habit = byId[c.habitId]
            listOf(
                quote(c.date.toString()), quote(c.habitId), text(habit?.title.orEmpty()), quote(c.status.name),
                quote(formatValue(c.value)), text(habit?.unit.orEmpty()), text(c.note.orEmpty())
            ).joinToString(",")
        }
        return (listOf(HEADER) + rows).joinToString("\r\n", postfix = "\r\n")
    }

    private fun formatValue(v: Double): String = if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString()

    // Uporabnikovo besedilo, ki se zacne z =, +, - ali @, bi preglednica izvedla kot formulo
    private fun text(field: String): String = quote(if (field.isNotEmpty() && field[0] in "=+-@") "'$field" else field)

    private fun quote(field: String): String =
        if (field.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + field.replace("\"", "\"\"") + "\"" else field
}
