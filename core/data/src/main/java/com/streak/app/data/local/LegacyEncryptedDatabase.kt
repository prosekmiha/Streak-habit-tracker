package com.streak.app.data.local

import android.content.Context
import android.util.Log
import java.io.File
import java.security.KeyStore

// Baza je bila v starejsih razlicicah sifrirana (SQLCipher). Sifriranje je odstranjeno:
// taksne baze brez knjiznice ni mogoce brati, zato jo enkrat odmaknemo in zacnemo s
// prazno bazo; ce obstaja samodejni posnetek, ga aplikacija ponudi za obnovo.
object LegacyEncryptedDatabase {
    private const val TAG = "LegacyEncryptedDatabase"
    private val PLAIN_HEADER = "SQLite format 3\u0000".toByteArray()

    // Ostanki sifriranja: ovit kljuc v no_backup in kljuc za ovijanje v Keystoru
    private const val WRAPPED_KEY_FILE = "db.key"
    private const val KEYSTORE_ALIAS = "streak_db_wrap"

    fun discardIfEncrypted(context: Context, dbFile: File) {
        if (dbFile.exists() && isEncrypted(dbFile)) {
            Log.w(TAG, "Sifrirana baza iz starejse razlicice: odmaknjena, zacetek s prazno bazo")
            val suffix = ".encrypted-" + System.currentTimeMillis()
            listOf("", "-wal", "-shm", "-journal").forEach { ext ->
                val f = File(dbFile.path + ext)
                if (f.exists()) f.renameTo(File(dbFile.path + suffix + ext))
            }
        }
        cleanUpKey(context)
    }

    // Nesifrirana SQLite datoteka se zacne z znano glavo; sifrirana je nakljucna.
    // Prazna ali prekratka datoteka ni sifrirana baza (z njo opravi Room/DatabaseGuard).
    private fun isEncrypted(file: File): Boolean = runCatching {
        if (file.length() < PLAIN_HEADER.size) return false
        file.inputStream().use { input ->
            val header = ByteArray(PLAIN_HEADER.size)
            input.read(header) == header.size && !header.contentEquals(PLAIN_HEADER)
        }
    }.getOrDefault(false)

    private fun cleanUpKey(context: Context) {
        val wrapped = File(context.noBackupFilesDir, WRAPPED_KEY_FILE)
        if (!wrapped.exists()) return
        wrapped.delete()
        runCatching { KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.deleteEntry(KEYSTORE_ALIAS) }
    }
}
