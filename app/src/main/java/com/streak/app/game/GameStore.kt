package com.streak.app.game

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.gameStore: DataStore<Preferences> by preferencesDataStore(name = "gamification")

enum class InboxType { BADGE, LEVEL, CHALLENGE }

// Dogodek v nabiralniku (zvonec na domacem zaslonu)
@Serializable
data class InboxEvent(val type: InboxType, val key: String, val atMillis: Long)

data class GameMemory(
    // Prvi izracun po namestitvi/obnovi le zapomni stanje, brez praznovanj
    val initialized: Boolean,
    val seenBadges: Set<String>,
    val lastLevel: Int,
    val lastChallengeDay: Long?,
    val events: List<InboxEvent>,
    val readUntil: Long
) {
    val unread: Int get() = events.count { it.atMillis > readUntil }
}

// Kaj je uporabnik ze videl (znacke, nivo, izziv) in nabiralnik. XP in znacke same so izpeljane
// iz podatkov; ta shramba le prepreci ponovna praznovanja. Je v Androidovi varnostni kopiji.
@Singleton
class GameStore @Inject constructor(@ApplicationContext private val context: Context) {

    private object Keys {
        val INITIALIZED = booleanPreferencesKey("initialized")
        val SEEN = stringSetPreferencesKey("seen_badges")
        val LEVEL = intPreferencesKey("last_level")
        val CHALLENGE_DAY = longPreferencesKey("last_challenge_day")
        val EVENTS = stringPreferencesKey("inbox_events")
        val READ_UNTIL = longPreferencesKey("inbox_read_until")
    }

    private val json = Json { ignoreUnknownKeys = true }

    val memory: Flow<GameMemory> = context.gameStore.data.map { it.toMemory() }.distinctUntilChanged()

    suspend fun current(): GameMemory = memory.first()

    suspend fun update(transform: (GameMemory) -> GameMemory) {
        context.gameStore.edit { p ->
            val m = transform(p.toMemory())
            p[Keys.INITIALIZED] = m.initialized
            p[Keys.SEEN] = m.seenBadges
            p[Keys.LEVEL] = m.lastLevel
            if (m.lastChallengeDay == null) p.remove(Keys.CHALLENGE_DAY) else p[Keys.CHALLENGE_DAY] = m.lastChallengeDay
            p[Keys.EVENTS] = json.encodeToString(m.events.sortedByDescending { it.atMillis }.take(MAX_EVENTS))
            p[Keys.READ_UNTIL] = m.readUntil
        }
    }

    suspend fun markRead(now: Long) = update { it.copy(readUntil = now) }

    // Po uvozu ali brisanju podatkov: naslednji izracun stanje le zapomni (brez plaza praznovanj)
    suspend fun resync() = update { it.copy(initialized = false) }

    suspend fun clear() = context.gameStore.edit { it.clear() }

    private fun Preferences.toMemory() = GameMemory(
        initialized = this[Keys.INITIALIZED] ?: false,
        seenBadges = this[Keys.SEEN].orEmpty(),
        lastLevel = this[Keys.LEVEL] ?: 1,
        lastChallengeDay = this[Keys.CHALLENGE_DAY],
        events = this[Keys.EVENTS]?.let { runCatching { json.decodeFromString<List<InboxEvent>>(it) }.getOrNull() }.orEmpty(),
        readUntil = this[Keys.READ_UNTIL] ?: 0L
    )

    private companion object {
        const val MAX_EVENTS = 50
    }
}
