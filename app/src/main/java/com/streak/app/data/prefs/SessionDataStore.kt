package com.streak.app.data.session

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.streak.app.domain.session.ActiveSession
import com.streak.app.domain.session.AmbientSound
import com.streak.app.domain.session.TimePoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

private val Context.sessionStore: DataStore<Preferences> by preferencesDataStore(name = "active_session")

// Trajno shranjena tekoca seja (locena datoteka od nastavitev, pise se pogosteje)
@Singleton
class SessionStore @Inject constructor(@ApplicationContext private val context: Context) {

    private object Keys {
        val HABIT = stringPreferencesKey("habit_id")
        val TITLE = stringPreferencesKey("title")
        val TARGET = longPreferencesKey("target_seconds")
        val STARTED = longPreferencesKey("started_at")
        val ACCUMULATED = longPreferencesKey("accumulated_ms")
        val RUN_ELAPSED = longPreferencesKey("run_elapsed_ms")
        val RUN_WALL = longPreferencesKey("run_wall_ms")
        val RUN_BOOT = intPreferencesKey("run_boot_count")
        val AMBIENT = stringPreferencesKey("ambient")
        val GOAL_NOTIFIED = booleanPreferencesKey("goal_notified")
        val LAST_AMBIENT = stringPreferencesKey("last_ambient")
    }

    suspend fun load(): ActiveSession? = context.sessionStore.data.first().toSession()

    suspend fun lastAmbient(): AmbientSound = ambientOf(context.sessionStore.data.first()[Keys.LAST_AMBIENT])

    suspend fun save(session: ActiveSession?) {
        context.sessionStore.edit { p ->
            listOf(Keys.HABIT, Keys.TITLE, Keys.AMBIENT).forEach { p.remove(it) }
            listOf(Keys.TARGET, Keys.STARTED, Keys.ACCUMULATED, Keys.RUN_ELAPSED, Keys.RUN_WALL).forEach { p.remove(it) }
            p.remove(Keys.RUN_BOOT)
            p.remove(Keys.GOAL_NOTIFIED)
            if (session == null) return@edit
            p[Keys.HABIT] = session.habitId
            p[Keys.TITLE] = session.title
            p[Keys.TARGET] = session.targetSeconds
            p[Keys.STARTED] = session.startedAt.toEpochMilli()
            p[Keys.ACCUMULATED] = session.accumulatedMs
            session.runningSince?.let {
                p[Keys.RUN_ELAPSED] = it.elapsedMs
                p[Keys.RUN_WALL] = it.wall.toEpochMilli()
                p[Keys.RUN_BOOT] = it.bootCount
            }
            p[Keys.AMBIENT] = session.ambient.name
            p[Keys.GOAL_NOTIFIED] = session.goalNotified
            p[Keys.LAST_AMBIENT] = session.ambient.name
        }
    }

    private fun Preferences.toSession(): ActiveSession? {
        val habitId = this[Keys.HABIT] ?: return null
        val runElapsed = this[Keys.RUN_ELAPSED]
        val runWall = this[Keys.RUN_WALL]
        val runBoot = this[Keys.RUN_BOOT]
        return ActiveSession(
            habitId = habitId,
            title = this[Keys.TITLE].orEmpty(),
            targetSeconds = this[Keys.TARGET] ?: 0,
            startedAt = Instant.ofEpochMilli(this[Keys.STARTED] ?: 0),
            accumulatedMs = this[Keys.ACCUMULATED] ?: 0,
            runningSince = if (runElapsed != null && runWall != null && runBoot != null) {
                TimePoint(runElapsed, Instant.ofEpochMilli(runWall), runBoot)
            } else null,
            ambient = ambientOf(this[Keys.AMBIENT]),
            goalNotified = this[Keys.GOAL_NOTIFIED] ?: false
        )
    }

    private fun ambientOf(name: String?) = AmbientSound.entries.firstOrNull { it.name == name } ?: AmbientSound.NONE
}
