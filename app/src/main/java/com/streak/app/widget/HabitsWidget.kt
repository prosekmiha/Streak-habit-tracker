package com.streak.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
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
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.streak.app.R
import com.streak.app.domain.model.DayState
import com.streak.app.domain.model.GoalType
import com.streak.app.domain.widget.WidgetHabit
import com.streak.app.domain.widget.WidgetMood
import com.streak.app.domain.widget.WidgetTap
import com.streak.app.ui.components.formatAmount
import kotlinx.coroutines.flow.first

// Srednji/velik widget: danasnje navade, ki se jih oznaci kar z domacega zaslona
class HabitsWidget : GlanceAppWidget() {
    // Natancna velikost: stevilo vrstic sledi dejanski visini widgeta
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val source = widgetEntryPoint(context).widgetRepository().today()
        val initial = source.first()
        provideContent {
            val state by source.collectAsState(initial)
            HabitsContent(state)
        }
    }
}

class HabitsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HabitsWidget()
}

private val RowHeight = 48.dp
private val HeaderHeight = 24.dp

@Composable
private fun HabitsContent(state: TodayWidgetState) {
    val context = LocalContext.current
    val colors = WidgetColors(state.look.dark)
    val s = state.summary
    val open = actionStartActivity(WidgetIntents.openApp(context))
    // Seznam, dokler je kaj neopravljenega; sicer maskota s sporocilom
    val actionable = s.items.any { !it.isDone && !it.isSettled }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .widgetBackground(colors.background)
            .padding(WidgetPadding)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = GlanceModifier.fillMaxWidth().height(HeaderHeight).clickable(open)
        ) {
            Text(
                context.getString(R.string.widget_today),
                maxLines = 1,
                style = TextStyle(color = colors.ink, fontSize = 16.sp, fontWeight = FontWeight.Bold),
                modifier = GlanceModifier.defaultWeight()
            )
            if (s.due > 0) {
                Text("${s.done}/${s.due}", style = TextStyle(color = colors.accent, fontSize = 15.sp, fontWeight = FontWeight.Bold))
            }
        }
        Spacer(GlanceModifier.height(6.dp))
        when {
            state.look.hideDetails || !actionable -> Message(s.mood, progressCaption(context, s), colors, hidden = state.look.hideDetails && actionable)
            else -> {
                val available = LocalSize.current.height - WidgetPadding * 2 - HeaderHeight - 6.dp
                val rows = (available.value / RowHeight.value).toInt().coerceIn(1, MAX_ROWS)
                s.items.take(rows).forEach { HabitRow(it, state.look.clockOk, colors) }
            }
        }
    }
}

@Composable
private fun Message(mood: WidgetMood, caption: String, colors: WidgetColors, hidden: Boolean) {
    val context = LocalContext.current
    val target = if (mood == WidgetMood.NO_HABITS) WidgetIntents.newHabit(context) else WidgetIntents.openApp(context)
    val modifier = GlanceModifier.fillMaxSize().clickable(actionStartActivity(target))
    val hint = if (hidden) context.getString(R.string.widget_open_app) else null
    // Ozek widget: maskota nad besedilom, da se besede ne lomijo
    if (LocalSize.current.width < 220.dp) {
        Column(verticalAlignment = Alignment.CenterVertically, horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
            MascotPose(mood, 56)
            MessageText(caption, hint, colors, TextAlign.Center)
        }
    } else {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
            MascotPose(mood, 64)
            Spacer(GlanceModifier.width(12.dp))
            Column { MessageText(caption, hint, colors, TextAlign.Start) }
        }
    }
}

@Composable
private fun MessageText(caption: String, hint: String?, colors: WidgetColors, align: TextAlign) {
    Text(caption, maxLines = 2, style = TextStyle(color = colors.ink, fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = align))
    if (hint != null) {
        Text(hint, maxLines = 2, style = TextStyle(color = colors.inkSoft, fontSize = 13.sp, textAlign = align))
    }
}

@Composable
private fun HabitRow(item: WidgetHabit, clockOk: Boolean, colors: WidgetColors) {
    val context = LocalContext.current
    val habit = item.habit
    val action: Action = when {
        !clockOk -> actionStartActivity(WidgetIntents.openApp(context))
        item.tap == WidgetTap.OPEN_SESSION -> actionStartActivity(WidgetIntents.openSession(context, habit.id))
        else -> actionRunCallback<TapHabitAction>(actionParametersOf(WidgetParams.HabitId to habit.id))
    }
    val subtitle = subtitle(context, item)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(RowHeight)
            .clickable(action)
            .semantics { contentDescription = rowDescription(context, item, subtitle) }
    ) {
        Image(ImageProvider(WidgetBitmaps.habitTile(context, habit.icon, habit.colorHex, 34.dp)), contentDescription = null, modifier = GlanceModifier.size(34.dp))
        Spacer(GlanceModifier.width(10.dp))
        Column(modifier = GlanceModifier.defaultWeight()) {
            val faded = item.isDone || item.isSettled
            Text(
                habit.title,
                maxLines = 1,
                style = TextStyle(color = if (faded) colors.inkSoft else colors.ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            )
            if (subtitle != null) {
                Text(subtitle, maxLines = 1, style = TextStyle(color = colors.inkSoft, fontSize = 12.sp))
            }
        }
        Spacer(GlanceModifier.width(8.dp))
        Indicator(item, colors)
    }
}

@Composable
private fun Indicator(item: WidgetHabit, colors: WidgetColors) {
    when {
        item.isDone -> Image(ImageProvider(R.drawable.widget_check_done), contentDescription = null, modifier = GlanceModifier.size(30.dp))
        item.isSettled -> Text("–", style = TextStyle(color = colors.inkSoft, fontSize = 18.sp, fontWeight = FontWeight.Bold))
        item.tap == WidgetTap.TOGGLE -> Image(
            ImageProvider(R.drawable.widget_circle_outline), contentDescription = null,
            colorFilter = ColorFilter.tint(colors.inkSoft), modifier = GlanceModifier.size(30.dp)
        )
        else -> Box(contentAlignment = Alignment.Center, modifier = GlanceModifier.size(32.dp).circleBackground(colors.surfaceSoft)) {
            Image(
                ImageProvider(if (item.tap == WidgetTap.ADD_ONE) R.drawable.widget_ic_add else R.drawable.widget_ic_play),
                contentDescription = null,
                colorFilter = ColorFilter.tint(colors.ink),
                modifier = GlanceModifier.size(18.dp)
            )
        }
    }
}

private fun subtitle(context: Context, item: WidgetHabit): String? {
    val habit = item.habit
    return when {
        item.state == DayState.SKIPPED -> context.getString(R.string.day_skipped)
        item.state == DayState.FROZEN -> context.getString(R.string.day_frozen)
        item.state == DayState.VACATION -> context.getString(R.string.day_vacation)
        habit.goalType == GoalType.QUANTITY ->
            context.getString(R.string.habit_progress_quantity, formatAmount(item.value), formatAmount(habit.targetValue), habit.unit.orEmpty()).trim()
        habit.goalType == GoalType.DURATION ->
            context.getString(R.string.habit_progress_minutes, (item.value / 60).toInt(), (habit.targetValue / 60).toInt())
        else -> null
    }
}

private fun rowDescription(context: Context, item: WidgetHabit, subtitle: String?): String {
    val name = listOfNotNull(item.habit.title, subtitle).joinToString(", ")
    return when {
        item.isDone -> context.getString(R.string.widget_a11y_undo, name)
        item.tap == WidgetTap.ADD_ONE -> context.getString(R.string.widget_a11y_add_one, name)
        item.tap == WidgetTap.OPEN_SESSION -> context.getString(R.string.widget_a11y_session, name)
        else -> context.getString(R.string.widget_a11y_done, name)
    }
}

private const val MAX_ROWS = 7
