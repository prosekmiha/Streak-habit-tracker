package com.streak.app.backup

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.streak.app.BuildConfig
import com.streak.app.data.local.dao.BackupDao
import com.streak.app.data.mapper.reminderEntities
import com.streak.app.data.mapper.toDomain
import com.streak.app.data.mapper.toEntity
import com.streak.app.domain.backup.BackupCodec
import com.streak.app.domain.backup.BackupContents
import com.streak.app.domain.backup.BackupMerge
import com.streak.app.domain.backup.CompletionsCsv
import com.streak.app.domain.backup.ParseResult
import com.streak.app.domain.model.AppSettings
import com.streak.app.domain.repository.HabitRepository
import com.streak.app.domain.repository.SettingsRepository
import com.streak.app.notifications.AlarmScheduler
import com.streak.app.session.SessionController
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

enum class ImportMode { MERGE, REPLACE }

// Podatki samodejnega posnetka (za obnovo po novi namestitvi)
data class SnapshotInfo(val createdAt: Instant?, val habits: Int)

// Izvoz, uvoz, brisanje vseh podatkov in samodejni posnetek za Androidovo varnostno kopijo
@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: BackupDao,
    private val settingsRepository: SettingsRepository,
    private val habitRepository: HabitRepository,
    private val alarmScheduler: AlarmScheduler,
    private val sessionController: SessionController
) {
    private val codec = BackupCodec()
    private val mutex = Mutex()
    private val snapshotFile get() = File(context.filesDir, "$SNAPSHOT_DIR/$SNAPSHOT_NAME")

    suspend fun readContents(): BackupContents = withContext(Dispatchers.IO) {
        val reminders = dao.reminders().groupBy { it.habitId }
        BackupContents(
            habits = dao.habits().map { it.toDomain(reminders[it.id].orEmpty()) },
            completions = dao.completions().map { it.toDomain() },
            sessions = dao.sessions().map { it.toDomain() },
            vacations = dao.vacations().map { it.toDomain() },
            tasks = dao.tasks().map { it.toDomain() },
            settings = BackupCodec.settingsOf(settingsRepository.settings.first())
        )
    }

    // JSON varnostna kopija (z geslom sifrirana); oznaci cas zadnje varnostne kopije
    suspend fun exportJson(password: CharArray?): String {
        val text = withContext(Dispatchers.Default) {
            codec.encode(readContents(), Instant.now(), BuildConfig.VERSION_NAME, password)
        }
        settingsRepository.update { it.copy(lastBackupAt = Instant.now(), backupReminderSnoozedUntil = null) }
        return text
    }

    suspend fun exportCsv(): String {
        val contents = readContents()
        return withContext(Dispatchers.Default) { CompletionsCsv.build(contents.habits, contents.completions) }
    }

    fun isEncrypted(text: String) = codec.isEncrypted(text)

    // Dekodiranje (PBKDF2 z geslom traja nekaj sekund, zato na Default)
    suspend fun parse(text: String, password: CharArray? = null): ParseResult =
        withContext(Dispatchers.Default) { codec.parse(text, password) }

    // Uvoz: zamenjava ali spoj, nato preracun streakov in alarmov
    suspend fun import(incoming: BackupContents, mode: ImportMode) = mutex.withLock {
        val local = readContents()
        val result = when (mode) {
            ImportMode.REPLACE -> incoming
            ImportMode.MERGE -> BackupMerge.merge(local, incoming)
        }
        // Alarmi starih navad (pri zamenjavi lahko izginejo)
        local.habits.forEach { alarmScheduler.cancel(it.id) }
        writeAll(result)
        if (mode == ImportMode.REPLACE) {
            incoming.settings?.let { s -> settingsRepository.update { BackupCodec.applySettings(s, it) } }
        }
        afterDataChange()
    }

    // Obnova iz samodejnega posnetka (npr. po novi namestitvi)
    suspend fun restoreSnapshot(): Boolean {
        val text = withContext(Dispatchers.IO) { snapshotFile.takeIf { it.exists() }?.readText() } ?: return false
        val parsed = parse(text) as? ParseResult.Ok ?: return false
        import(parsed.contents, ImportMode.REPLACE)
        return true
    }

    // Ponujena obnova: baza je prazna, posnetek pa obstaja (Androidova varnostna kopija)
    suspend fun pendingSnapshot(): SnapshotInfo? = withContext(Dispatchers.IO) {
        if (dao.habitCount() > 0 || !snapshotFile.exists()) return@withContext null
        (codec.parse(snapshotFile.readText()) as? ParseResult.Ok)
            ?.takeIf { it.summary.habits > 0 }
            ?.let { SnapshotInfo(it.summary.createdAt, it.summary.habits) }
    }

    suspend fun discardSnapshot() = withContext(Dispatchers.IO) { snapshotFile.delete() }

    // Posnetek se nikoli ne prepise s prazno bazo (sicer bi po novi namestitvi izgubili kopijo)
    suspend fun writeSnapshot() = mutex.withLock {
        withContext(Dispatchers.IO) {
            if (!settingsRepository.settings.first().cloudSnapshotEnabled) {
                snapshotFile.delete()
                return@withContext
            }
            if (dao.habitCount() == 0) return@withContext
            val text = codec.encode(readContents(), Instant.now(), BuildConfig.VERSION_NAME)
            val dir = snapshotFile.parentFile!!.apply { mkdirs() }
            val tmp = File(dir, "$SNAPSHOT_NAME.tmp")
            tmp.writeText(text)
            if (!tmp.renameTo(snapshotFile)) {
                snapshotFile.delete()
                tmp.renameTo(snapshotFile)
            }
        }
    }

    // Izbrise vse: podatke, nastavitve, posnetek, tekoco sejo, obvestila in alarme
    suspend fun wipeAll() = mutex.withLock {
        sessionController.finish(save = false)
        readContents().habits.forEach { alarmScheduler.cancel(it.id) }
        withContext(Dispatchers.IO) {
            dao.clearAll()
            snapshotFile.delete()
        }
        NotificationManagerCompat.from(context).cancelAll()
        settingsRepository.update { AppSettings() }
    }

    private suspend fun writeAll(c: BackupContents) = withContext(Dispatchers.IO) {
        dao.replaceAll(
            habits = c.habits.map { it.toEntity() },
            reminders = c.habits.flatMap { it.reminderEntities() },
            completions = c.completions.map { it.toEntity() },
            sessions = c.sessions.map { it.toEntity() },
            vacations = c.vacations.map { it.copy(id = 0).toEntity() },
            tasks = c.tasks.map { it.toEntity() }
        )
    }

    private suspend fun afterDataChange() {
        habitRepository.recalculateAll()
        alarmScheduler.rescheduleAll()
    }

    companion object {
        // Vkljucen v Androidovo varnostno kopijo (res/xml/data_extraction_rules.xml)
        const val SNAPSHOT_DIR = "snapshot"
        const val SNAPSHOT_NAME = "latest.json"
    }
}
