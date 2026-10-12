package com.streak.app.data.local

import android.util.Log
import net.zetetic.database.sqlcipher.SQLiteDatabase
import java.io.File

// Pomozne operacije za SQLCipher: zaznava nesifrirane baze, enkratna pretvorba in branje
// verzije sheme sifrirane baze (za kopijo pred migracijo)
object DatabaseEncryption {

    private val PLAIN_HEADER = "SQLite format 3\u0000".toByteArray()

    @Volatile private var loaded = false

    fun loadLibrary() {
        if (!loaded) synchronized(this) {
            if (!loaded) {
                System.loadLibrary("sqlcipher")
                loaded = true
            }
        }
    }

    // Nesifrirana SQLite datoteka se zacne z znano glavo; sifrirana je nakljucna
    fun isPlaintext(file: File): Boolean = runCatching {
        file.inputStream().use { input ->
            val header = ByteArray(PLAIN_HEADER.size)
            input.read(header) == header.size && header.contentEquals(PLAIN_HEADER)
        }
    }.getOrDefault(false)

    // Obstojeco nesifrirano bazo (starejse razlicice) prepise v sifrirano. Ob napaki
    // ostane izvirnik nedotaknjen. Kljuc se poda prek API-ja in nikoli ni del SQL
    // stavka (SQLite ob napaki stavek izpise v log).
    fun encryptInPlace(file: File, rawKey: String) {
        loadLibrary()
        val tmp = File(file.path + ".encrypting")
        listOf("", "-wal", "-shm", "-journal").forEach { File(tmp.path + it).delete() }
        val encrypted = SQLiteDatabase.openDatabase(
            tmp.path, rawKey, null, SQLiteDatabase.OPEN_READWRITE or SQLiteDatabase.CREATE_IF_NECESSARY, null
        )
        try {
            // Prazen kljuc = nesifrirana baza
            encrypted.rawExecSQL("ATTACH DATABASE '${file.path}' AS plaintext KEY ''")
            encrypted.rawExecSQL("SELECT sqlcipher_export('main', 'plaintext')")
            val version = encrypted.rawQuery("PRAGMA plaintext.user_version", null).use { c -> if (c.moveToFirst()) c.getInt(0) else 0 }
            encrypted.rawExecSQL("DETACH DATABASE plaintext")
            encrypted.version = version
        } finally {
            encrypted.close()
        }
        listOf("-wal", "-shm", "-journal").forEach { File(tmp.path + it).delete() }
        listOf("-wal", "-shm", "-journal").forEach { File(file.path + it).delete() }
        if (!tmp.renameTo(file)) {
            file.delete()
            check(tmp.renameTo(file)) { "Sifrirane baze ni mogoce premakniti" }
        }
        Log.i("DatabaseEncryption", "Baza je sifrirana")
    }

    fun readVersion(file: File, rawKey: String): Int? = runCatching {
        loadLibrary()
        SQLiteDatabase.openDatabase(file.path, rawKey, null, SQLiteDatabase.OPEN_READONLY, null).use { it.version }
    }.getOrNull()
}
