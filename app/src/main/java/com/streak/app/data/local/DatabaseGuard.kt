package com.streak.app.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

sealed interface DatabaseHealth {
    data object Ok : DatabaseHealth
    // Migracija ni uspela; originalna baza je obnovljena iz kopije
    data class MigrationFailed(val fromVersion: Int, val error: String) : DatabaseHealth
    // PRAGMA quick_check je nasel napake
    data class Corrupted(val details: String) : DatabaseHealth
}

// Varuje podatke ob nadgradnji sheme:
// 1. pred migracijo kopira datoteko baze (in WAL) v no_backup/db-backups
// 2. ce odpiranje/migracija spodleti, obnovi kopijo in sporoci stanje UI-ju
// 3. obcasno (odlozeno, ne ob zagonu) preveri integriteto baze
@Singleton
class DatabaseGuard @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _health = MutableStateFlow<DatabaseHealth>(DatabaseHealth.Ok)
    val health: StateFlow<DatabaseHealth> = _health.asStateFlow()

    private val prefs by lazy { context.getSharedPreferences("db_guard", Context.MODE_PRIVATE) }
    private val backupDir: File get() = File(context.noBackupFilesDir, "db-backups")

    private var pendingBackup: File? = null
    private var pendingFromVersion: Int = 0

    // Klice se tik pred gradnjo Room baze. Prebere verzijo obstojece datoteke
    // in, ce bo potrebna migracija, naredi kopijo.
    fun prepareForOpen(name: String, targetVersion: Int) {
        val dbFile = context.getDatabasePath(name)
        if (!dbFile.exists()) return
        val version = readVersion(dbFile) ?: return
        if (version in 1 until targetVersion) {
            pendingFromVersion = version
            pendingBackup = runCatching { backup(dbFile, version) }
                .onFailure { Log.e(TAG, "Kopija baze pred migracijo ni uspela", it) }
                .getOrNull()
        }
    }

    // Odpre bazo (sprozi migracijo) v ozadju. Ob napaki obnovi kopijo.
    suspend fun openVerified(db: StreakDatabase, name: String) = withContext(Dispatchers.IO) {
        try {
            db.openHelper.writableDatabase
            pendingBackup = null
        } catch (e: Exception) {
            Log.e(TAG, "Odpiranje/migracija baze ni uspela", e)
            db.close()
            pendingBackup?.let { restore(it, context.getDatabasePath(name)) }
            _health.value = DatabaseHealth.MigrationFailed(pendingFromVersion, e.message ?: e.javaClass.simpleName)
        }
    }

    // Hitro preverjanje integritete najvec enkrat na INTEGRITY_INTERVAL
    suspend fun checkIntegrityIfDue(db: StreakDatabase) = withContext(Dispatchers.IO) {
        if (_health.value != DatabaseHealth.Ok) return@withContext
        val last = prefs.getLong(KEY_LAST_CHECK, 0L)
        val now = System.currentTimeMillis()
        if (now - last < INTEGRITY_INTERVAL_MS) return@withContext
        val result = runCatching {
            db.openHelper.readableDatabase.query("PRAGMA quick_check").use { c ->
                buildList { while (c.moveToNext()) add(c.getString(0)) }
            }
        }.getOrElse { listOf(it.message ?: "error") }
        prefs.edit().putLong(KEY_LAST_CHECK, now).apply()
        if (result != listOf("ok")) {
            _health.value = DatabaseHealth.Corrupted(result.take(5).joinToString("; "))
        }
    }

    private fun readVersion(dbFile: File): Int? = runCatching {
        SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READONLY).use { it.version }
    }.getOrNull()

    private fun backup(dbFile: File, version: Int): File {
        backupDir.mkdirs()
        val target = File(backupDir, "streak-v$version-${System.currentTimeMillis()}.db")
        dbFile.copyTo(target, overwrite = true)
        companions(dbFile).forEach { (suffix, f) -> f.copyTo(File(target.path + suffix), overwrite = true) }
        pruneOldBackups()
        return target
    }

    private fun restore(backup: File, dbFile: File) {
        runCatching {
            companions(dbFile).forEach { (_, f) -> f.delete() }
            backup.copyTo(dbFile, overwrite = true)
            listOf("-wal", "-shm").forEach { suffix ->
                val b = File(backup.path + suffix)
                if (b.exists()) b.copyTo(File(dbFile.path + suffix), overwrite = true)
            }
        }.onFailure { Log.e(TAG, "Obnova kopije baze ni uspela", it) }
    }

    private fun companions(dbFile: File): List<Pair<String, File>> =
        listOf("-wal", "-shm").map { it to File(dbFile.path + it) }.filter { it.second.exists() }

    private fun pruneOldBackups() {
        val dbs = backupDir.listFiles { f -> f.name.endsWith(".db") }?.sortedByDescending { it.lastModified() } ?: return
        dbs.drop(MAX_BACKUPS).forEach { old ->
            old.delete()
            File(old.path + "-wal").delete()
            File(old.path + "-shm").delete()
        }
    }

    private companion object {
        const val TAG = "DatabaseGuard"
        const val KEY_LAST_CHECK = "last_integrity_check"
        const val MAX_BACKUPS = 3
        val INTEGRITY_INTERVAL_MS = TimeUnit.DAYS.toMillis(7)
    }
}
