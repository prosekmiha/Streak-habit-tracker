package com.streak.app.shortcuts

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.streak.app.MainActivity
import com.streak.app.R
import com.streak.app.domain.model.GoalType
import com.streak.app.domain.repository.HabitRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// Dinamicne bliznjice "Zacni sejo" za casovne navade (dolg pritisk na ikono aplikacije)
@Singleton
class ShortcutPublisher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val habitRepository: HabitRepository
) {
    suspend fun run() {
        habitRepository.observeHabits()
            .map { list -> list.map { it.habit }.filter { it.goalType == GoalType.DURATION }.map { it.id to it.title } }
            .distinctUntilChanged()
            .collect { publish(it) }
    }

    private fun publish(habits: List<Pair<String, String>>) {
        // Ena mesto ostane staticni bliznjici "Nova navada"
        val max = (ShortcutManagerCompat.getMaxShortcutCountPerActivity(context) - 1).coerceIn(0, MAX_SESSIONS)
        val shortcuts = habits.take(max).mapIndexed { index, (id, title) ->
            ShortcutInfoCompat.Builder(context, "session_$id")
                .setShortLabel(title)
                .setLongLabel(context.getString(R.string.shortcut_session_long, title))
                .setIcon(IconCompat.createWithResource(context, R.drawable.ic_shortcut_session))
                .setRank(index)
                .setIntent(
                    Intent(context, MainActivity::class.java)
                        .setAction(MainActivity.ACTION_START_SESSION)
                        .putExtra(MainActivity.EXTRA_HABIT_ID, id)
                )
                .build()
        }
        runCatching { ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts) }
        // Pripete bliznjice izbrisanih, arhiviranih ali spremenjenih navad onemogoci
        val keep = habits.map { "session_${it.first}" }.toSet()
        val stale = ShortcutManagerCompat.getShortcuts(context, ShortcutManagerCompat.FLAG_MATCH_PINNED)
            .map { it.id }
            .filter { it.startsWith("session_") && it !in keep }
        if (stale.isNotEmpty()) {
            runCatching { ShortcutManagerCompat.disableShortcuts(context, stale, context.getString(R.string.shortcut_disabled)) }
        }
    }

    // Launcher se uci, katere bliznjice uporabnik res uporablja (vrstni red, predlogi)
    fun reportSessionStarted(habitId: String) {
        runCatching { ShortcutManagerCompat.reportShortcutUsed(context, "session_$habitId") }
    }

    companion object {
        private const val MAX_SESSIONS = 3
    }
}
