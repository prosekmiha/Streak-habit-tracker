package com.streak.app.domain.health

import kotlinx.coroutines.flow.StateFlow

sealed interface DatabaseHealth {
    data object Ok : DatabaseHealth
    // Migracija ni uspela; originalna baza je obnovljena iz kopije
    data class MigrationFailed(val fromVersion: Int, val error: String) : DatabaseHealth
    // PRAGMA quick_check je nasel napake
    data class Corrupted(val details: String) : DatabaseHealth
}

interface DatabaseHealthMonitor {
    val health: StateFlow<DatabaseHealth>
}
