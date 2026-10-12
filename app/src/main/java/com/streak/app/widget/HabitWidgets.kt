package com.streak.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.streak.app.R
import com.streak.app.domain.model.GoalType
import com.streak.app.domain.model.StreakUnit
import com.streak.app.domain.widget.WidgetHabit
import com.streak.app.ui.components.formatAmount
import com.streak.app.ui.components.parseHexColor
import kotlinx.coroutines.flow.first

// Izbrana navada widgeta (nastavi jo WidgetConfigActivity)
object WidgetKeys {
    val HabitId = stringPreferencesKey("habit_id")
}

// Skupna osnova za widgete ene navade: nalozi izbrano navado in jo spremlja
abstract class SingleHabitWidget : GlanceAppWidget() {
    // Ali navada ustreza widgetu (npr. le kolicina)
    open fun accepts(item: WidgetHabit): Boolean = true

    @Composable
    abstract fun Content(state: HabitWidgetState.Ready)

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = widgetEntryPoint(context).widgetRepository()
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val initialId = getAppWidgetState(context, PreferencesGlanceStateDefinition, id)[WidgetKeys.HabitId]
        val initial = repository.habit(initialId, ::accepts).first()
        provideContent {
            val habitId = currentState<Preferences>()[WidgetKeys.HabitId]
            val flow = remember(habitId) { repository.habit(habitId, ::accepts) }
            val state by flow.collectAsState(if (habitId == initialId) initial else HabitWidgetState.Loading)
            when (val s = state) {
                is HabitWidgetState.Ready -> Content(s)
                HabitWidgetState.NeedsHabit -> PickHabit(appWidgetId, quantity = this is QuantityWidget)
                HabitWidgetState.Loading -> Box(GlanceModifier.fillMaxSize().widgetBackground(WidgetColors(null).background)) {}
            }
        }
    }
}

// Widget se ni nastavljen ali navade ni vec: dotik odpre izbiro navade
@Composable
private fun PickHabit(appWidgetId: Int, quantity: Boolean) {
    val context = LocalContext.current
    val colors = WidgetColors(null)
    Box(
        contentAlignment = Alignment.Center,
        modifier = GlanceModifier
            .fillMaxSize()
            .widgetBackground(colors.background)
            .clickable(actionStartActivity(WidgetIntents.configure(context, appWidgetId)))
            .padding(WidgetPadding)
    ) {
        Text(
            context.getString(if (quantity) R.string.widget_pick_quantity else R.string.widget_pick_habit),
            style = TextStyle(color = colors.inkSoft, fontSize = 13.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
        )
    }
}

// ---------------- Streak ----------------

class StreakWidget : SingleHabitWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(DpSize(110.dp, 110.dp), DpSize(110.dp, 160.dp)))

    @Composable
    override fun Content(state: HabitWidgetState.Ready) = StreakContent(state)
}

class StreakWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = StreakWidget()
}

@Composable
private fun StreakContent(state: HabitWidgetState.Ready) {
    val context = LocalContext.current
    val item = state.item
    val hidden = state.look.hideDetails
    val unitLabel = context.resources.getQuantityString(streakUnitPlural(item.streakUnit), item.currentStreak)
    val tall = LocalSize.current.height >= 160.dp
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .widgetBackground(WidgetColors.fixed(parseHexColor(item.habit.colorHex)))
            .clickable(actionStartActivity(WidgetIntents.openHabit(context, item.habit.id)))
            .padding(WidgetPadding)
            .semantics { contentDescription = streakDescription(context, item, hidden) }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = GlanceModifier.fillMaxWidth()) {
            if (hidden) {
                Image(ImageProvider(R.drawable.widget_ic_flame), contentDescription = null, colorFilter = ColorFilter.tint(WidgetColors.OnPastel), modifier = GlanceModifier.size(22.dp))
            } else {
                Image(ImageProvider(WidgetBitmaps.habitIcon(context, item.habit.icon, 22.dp)), contentDescription = null, modifier = GlanceModifier.size(22.dp))
            }
            Spacer(GlanceModifier.defaultWeight())
            if (item.isDone) Image(ImageProvider(R.drawable.widget_check_on_pastel), contentDescription = null, modifier = GlanceModifier.size(24.dp))
        }
        Spacer(GlanceModifier.defaultWeight())
        Text(item.currentStreak.toString(), maxLines = 1, style = TextStyle(color = WidgetColors.OnPastel, fontSize = 40.sp, fontWeight = FontWeight.Bold))
        Text(unitLabel, maxLines = 1, style = TextStyle(color = WidgetColors.OnPastelSoft, fontSize = 13.sp, fontWeight = FontWeight.Medium))
        if (!hidden) {
            Spacer(GlanceModifier.height(2.dp))
            Text(item.habit.title, maxLines = 1, style = TextStyle(color = WidgetColors.OnPastel, fontSize = 13.sp, fontWeight = FontWeight.Bold))
        }
        if (tall) {
            Text(context.getString(R.string.widget_best, item.bestStreak), maxLines = 1, style = TextStyle(color = WidgetColors.OnPastelSoft, fontSize = 12.sp))
        }
    }
}

private fun streakUnitPlural(unit: StreakUnit) = when (unit) {
    StreakUnit.DAY -> R.plurals.widget_unit_days
    StreakUnit.WEEK -> R.plurals.widget_unit_weeks
    StreakUnit.MONTH -> R.plurals.widget_unit_months
}

private fun streakDescription(context: Context, item: WidgetHabit, hidden: Boolean): String {
    val plural = when (item.streakUnit) {
        StreakUnit.DAY -> R.plurals.streak_days
        StreakUnit.WEEK -> R.plurals.streak_weeks
        StreakUnit.MONTH -> R.plurals.streak_months
    }
    val streak = context.resources.getQuantityString(plural, item.currentStreak, item.currentStreak)
    return if (hidden) streak else "${item.habit.title}, $streak"
}

// ---------------- Kolicina ----------------

class QuantityWidget : SingleHabitWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(DpSize(110.dp, 110.dp), DpSize(180.dp, 110.dp)))

    override fun accepts(item: WidgetHabit) = item.habit.goalType == GoalType.QUANTITY

    @Composable
    override fun Content(state: HabitWidgetState.Ready) = QuantityContent(state)
}

class QuantityWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuantityWidget()
}

@Composable
private fun QuantityContent(state: HabitWidgetState.Ready) {
    val context = LocalContext.current
    val item = state.item
    val habit = item.habit
    val hidden = state.look.hideDetails
    val amount = "${formatAmount(item.value)} / ${formatAmount(habit.targetValue)}"
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .widgetBackground(WidgetColors.fixed(parseHexColor(habit.colorHex)))
            .padding(WidgetPadding)
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .defaultWeight()
                .clickable(actionStartActivity(WidgetIntents.openHabit(context, habit.id)))
                .semantics { contentDescription = listOfNotNull(habit.title.takeIf { !hidden }, amount, habit.unit).joinToString(", ") }
        ) {
            if (!hidden) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(ImageProvider(WidgetBitmaps.habitIcon(context, habit.icon, 18.dp)), contentDescription = null, modifier = GlanceModifier.size(18.dp))
                    Spacer(GlanceModifier.width(6.dp))
                    Text(habit.title, maxLines = 1, style = TextStyle(color = WidgetColors.OnPastel, fontSize = 13.sp, fontWeight = FontWeight.Bold))
                }
            }
            Spacer(GlanceModifier.defaultWeight())
            Row(verticalAlignment = Alignment.Bottom) {
                Text(amount, maxLines = 1, style = TextStyle(color = WidgetColors.OnPastel, fontSize = 24.sp, fontWeight = FontWeight.Bold))
                val unit = habit.unit
                if (!unit.isNullOrBlank()) {
                    Spacer(GlanceModifier.width(4.dp))
                    Text(unit, maxLines = 1, style = TextStyle(color = WidgetColors.OnPastelSoft, fontSize = 12.sp))
                }
            }
            Spacer(GlanceModifier.height(6.dp))
            LinearProgressIndicator(
                progress = item.progress,
                color = WidgetColors.OnPastel,
                backgroundColor = WidgetColors.PastelButton,
                modifier = GlanceModifier.fillMaxWidth().height(6.dp)
            )
        }
        Spacer(GlanceModifier.height(10.dp))
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            StepButton(R.drawable.widget_ic_remove, context.getString(R.string.decrease), habit.id, -1.0, state.look.clockOk)
            Spacer(GlanceModifier.defaultWeight())
            StepButton(R.drawable.widget_ic_add, context.getString(R.string.increase), habit.id, 1.0, state.look.clockOk)
        }
    }
}

@Composable
private fun StepButton(icon: Int, description: String, habitId: String, delta: Double, clockOk: Boolean) {
    val context = LocalContext.current
    val action = if (clockOk) actionRunCallback<AddProgressAction>(actionParametersOf(WidgetParams.HabitId to habitId, WidgetParams.Delta to delta))
    else actionStartActivity(WidgetIntents.openApp(context))
    Box(
        contentAlignment = Alignment.Center,
        modifier = GlanceModifier
            .size(44.dp)
            .circleBackground(WidgetColors.PastelButton)
            .clickable(action)
            .semantics { contentDescription = description }
    ) {
        Image(ImageProvider(icon), contentDescription = null, colorFilter = ColorFilter.tint(WidgetColors.OnPastel), modifier = GlanceModifier.size(22.dp))
    }
}
