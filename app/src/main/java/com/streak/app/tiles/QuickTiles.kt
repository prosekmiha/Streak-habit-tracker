package com.streak.app.tiles

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.streak.app.MainActivity
import com.streak.app.R
import com.streak.app.domain.model.GoalType
import com.streak.app.domain.model.Habit
import com.streak.app.domain.widget.WidgetHabit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

// Kaj naredi dotik ploscice navade
enum class TileClick {
    CONFIGURE, // navada ni izbrana (ali ne obstaja vec): odpri aplikacijo
    LOG        // da/ne preklopi, kolicina +1 (enako kot widget)
}

object QuickTiles {
    // Ploscica navade podpira le navade, ki jih lahko vpise brez aplikacije
    fun supportsHabitTile(habit: Habit): Boolean = habit.goalType == GoalType.YES_NO || habit.goalType == GoalType.QUANTITY

    fun supportsSessionTile(habit: Habit): Boolean = habit.goalType == GoalType.DURATION

    fun habitClick(item: WidgetHabit?): TileClick = when {
        item == null || !supportsHabitTile(item.habit) -> TileClick.CONFIGURE
        else -> TileClick.LOG
    }
}

// Osvezi ploscici (sistem pokliče onStartListening, ko sta vidni)
@Singleton
class TileUpdater @Inject constructor(@ApplicationContext private val context: Context) {
    fun requestAll() {
        runCatching {
            TileService.requestListeningState(context, ComponentName(context, HabitTileService::class.java))
            TileService.requestListeningState(context, ComponentName(context, SessionTileService::class.java))
        }
    }

    // Android 13+: sistemsko vprasanje "Dodaj ploscico" brez iskanja v urejanju hitrih nastavitev
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun requestAdd(session: Boolean) {
        val (cls, label, icon) = if (session) {
            Triple(SessionTileService::class.java, R.string.tile_session_label, R.drawable.widget_ic_play)
        } else {
            Triple(HabitTileService::class.java, R.string.tile_habit_label, R.drawable.ic_stat_habit)
        }
        context.getSystemService(StatusBarManager::class.java)?.requestAddTileService(
            ComponentName(context, cls),
            context.getString(label),
            Icon.createWithResource(context, icon),
            context.mainExecutor
        ) { }
    }
}

// Odpre aplikacijo iz ploscice in zapre hitre nastavitve (Intent razlicica le pod Androidom 14)
@SuppressLint("StartActivityAndCollapseDeprecated")
internal fun TileService.openApp(intent: Intent) {
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        startActivityAndCollapse(PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
    } else {
        @Suppress("DEPRECATION")
        startActivityAndCollapse(intent)
    }
}

internal fun TileService.mainIntent(action: String? = null, habitId: String? = null): Intent =
    Intent(this, MainActivity::class.java).apply {
        action?.let { setAction(it) }
        habitId?.let { putExtra(MainActivity.EXTRA_HABIT_ID, it) }
    }
