package com.streak.app.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import com.streak.app.locale.AppLocale
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.streak.app.R
import com.streak.app.domain.model.GoalType
import com.streak.app.domain.model.Habit
import com.streak.app.domain.model.ThemeMode
import com.streak.app.domain.repository.HabitRepository
import com.streak.app.domain.repository.SettingsRepository
import com.streak.app.ui.components.IconMap
import com.streak.app.ui.components.ScreenTopBar
import com.streak.app.ui.components.SettingsGroup
import com.streak.app.ui.components.SettingsRow
import com.streak.app.ui.components.parseHexColor
import com.streak.app.ui.theme.StreakTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

// Izbira navade za widget streaka ali kolicine (ob dodajanju in ob ponovni nastavitvi)
@AndroidEntryPoint
class WidgetConfigActivity : ComponentActivity() {

    @Inject lateinit var habitRepository: HabitRepository
    @Inject lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Preklic (nazaj) ne doda widgeta
        setResult(RESULT_CANCELED)
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        val provider = AppWidgetManager.getInstance(this).getAppWidgetInfo(appWidgetId)?.provider?.className
        val quantityOnly = provider == QuantityWidgetReceiver::class.java.name
        val habits = habitRepository.observeHabits().map { list ->
            list.map { it.habit }.filter { !quantityOnly || it.goalType == GoalType.QUANTITY }
        }
        val themeMode = settingsRepository.settings.map { it.themeMode }
        enableEdgeToEdge()
        setContent {
            val theme by themeMode.collectAsStateWithLifecycle(ThemeMode.SYSTEM)
            val items by habits.collectAsStateWithLifecycle(null)
            val dark = when (theme) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            StreakTheme(darkTheme = dark) {
                ConfigScreen(items, quantityOnly, onPick = { pick(appWidgetId, quantityOnly, it) }, onClose = ::finish)
            }
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocale.wrap(newBase))
    }

    private fun pick(appWidgetId: Int, quantity: Boolean, habit: Habit) {
        lifecycleScope.launch {
            val glanceId = GlanceAppWidgetManager(this@WidgetConfigActivity).getGlanceIdBy(appWidgetId)
            updateAppWidgetState(this@WidgetConfigActivity, glanceId) { it[WidgetKeys.HabitId] = habit.id }
            val widget = if (quantity) QuantityWidget() else StreakWidget()
            widget.update(this@WidgetConfigActivity, glanceId)
            setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
            finish()
        }
    }
}

@Composable
private fun ConfigScreen(habits: List<Habit>?, quantityOnly: Boolean, onPick: (Habit) -> Unit, onClose: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp)
    ) {
        ScreenTopBar(
            stringResource(if (quantityOnly) R.string.widget_config_quantity_title else R.string.widget_config_title),
            Icons.Rounded.Close,
            stringResource(R.string.close),
            onClose
        )
        when {
            habits == null -> Unit
            habits.isEmpty() -> Text(
                stringResource(if (quantityOnly) R.string.widget_config_empty_quantity else R.string.widget_config_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = StreakTheme.palette.inkSoft,
                modifier = Modifier.padding(top = 16.dp, start = 6.dp, end = 6.dp)
            )
            else -> SettingsGroup {
                habits.forEachIndexed { i, habit ->
                    SettingsRow(
                        habit.title,
                        IconMap.resolve(habit.icon),
                        parseHexColor(habit.colorHex),
                        { onPick(habit) },
                        showDivider = i < habits.lastIndex
                    )
                }
            }
        }
    }
}
