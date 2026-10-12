package com.streak.app.domain.backup

import java.time.Instant

enum class ImportMode { MERGE, REPLACE }

// Podatki samodejnega posnetka (za obnovo po novi namestitvi)
data class SnapshotInfo(val createdAt: Instant?, val habits: Int)

// Izvoz, uvoz, brisanje vseh podatkov in samodejni posnetek za Androidovo varnostno kopijo
interface BackupService {
    suspend fun readContents(): BackupContents

    // JSON varnostna kopija (z geslom sifrirana); oznaci cas zadnje varnostne kopije
    suspend fun exportJson(password: CharArray?): String
    suspend fun exportCsv(): String

    fun isEncrypted(text: String): Boolean
    suspend fun parse(text: String, password: CharArray? = null): ParseResult

    // Uvoz: zamenjava ali spoj, nato preracun streakov in alarmov
    suspend fun import(incoming: BackupContents, mode: ImportMode)

    // Ponujena obnova: baza je prazna, posnetek pa obstaja
    suspend fun pendingSnapshot(): SnapshotInfo?
    suspend fun restoreSnapshot(): Boolean
    suspend fun discardSnapshot()
    suspend fun writeSnapshot()

    // Izbrise vse: podatke, nastavitve, posnetek, tekoco sejo, obvestila in alarme
    suspend fun wipeAll()
}
