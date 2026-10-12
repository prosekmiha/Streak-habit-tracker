package com.streak.app.data.prefs

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
import com.streak.app.domain.gamification.GameMemory
import com.streak.app.domain.gamification.InboxEvent
import com.streak.app.domain.gamification.InboxType
import com.streak.app.domain.repository.GameMemoryRepository
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

// Zapis dogodka v JSON (oblika je ze shranjena na napravah, zato se ne sme spremeniti)
@Serializable
private data class InboxEventJson(val type: InboxType, val key: String, val atMillis: Long)

// Spomin igrifikacije v DataStore. Je v Androidovi varnostni kopiji.
@Singleton
class GameDataStore @Inject constructor(@ApplicationContext private val context: Context) : GameMemoryRepository {

    private object Keys {
        val INITIALIZED = booleanPreferencesKey("initialized")
        val SEEN = stringSetPreferencesKey("seen_badges")
        val LEVEL = intPreferencesKey("last_level")
        val CHALLENGE_DAY = longPreferencesKey("last_challenge_day")
        val EVENTS = stringPreferencesKey("inbox_events")
        val READ_UNTIL = longPreferencesKey("inbox_read_until")
    }

    private val json = Json { ignoreUnknownKeys = true }

    override val memory: Flow<GameMemory> = context.gameStore.data.map { it.toMemory() }.distinctUntilChanged()

    override suspend fun current(): GameMemory = memory.first()

    override suspend fun update(transform: (GameMemory) -> GameMemory) {
        context.gameStore.edit { p ->
            val m = transform(p.toMemory())
            p[Keys.INITIALIZED] = m.initialized
            p[Keys.SEEN] = m.seenBadges
            p[Keys.LEVEL] = m.lastLevel
            m.lastChallengeDay?.let { p[Keys.CHALLENGE_DAY] = it } ?: p.remove(Keys.CHALLENGE_DAY)
            p[Keys.EVENTS] = json.encodeToString(m.events.sortedByDescending { it.atMillis }.take(MAX_EVENTS).map { InboxEventJson(it.type, it.key, it.atMillis) })
            p[Keys.READ_UNTIL] = m.readUntil
        }
    }

    override suspend fun clear() {
        context.gameStore.edit { it.clear() }
    }

    private fun Preferences.toMemory() = GameMemory(
        initialized = this[Keys.INITIALIZED] ?: false,
        seenBadges = this[Keys.SEEN].orEmpty(),
        lastLevel = this[Keys.LEVEL] ?: 1,
        lastChallengeDay = this[Keys.CHALLENGE_DAY],
        events = this[Keys.EVENTS]?.let { runCatching { json.decodeFromString<List<InboxEventJson>>(it) }.getOrNull() }.orEmpty()
            .map { InboxEvent(it.type, it.key, it.atMillis) },
        readUntil = this[Keys.READ_UNTIL] ?: 0L
    )

    private companion object {
        const val MAX_EVENTS = 50
    }
}
