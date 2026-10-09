package com.streak.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.streak.app.R
import com.streak.app.domain.model.DayState
import com.streak.app.domain.model.GoalType
import com.streak.app.domain.model.Habit
import com.streak.app.domain.model.AnimationLevel
import com.streak.app.ui.theme.InkOnPastel
import com.streak.app.ui.theme.LocalAnimationLevel
import com.streak.app.ui.theme.StreakTheme

data class HabitCardUi(
    val habit: Habit,
    val state: DayState,
    val value: Double,
    val currentStreak: Int
) {
    val isDone: Boolean get() = state == DayState.DONE
    val progress: Float
        get() = if (habit.targetValue <= 0.0) 0f else (value / habit.targetValue).toFloat().coerceIn(0f, 1f)
}

// Kartica navade v mrezi: pastelno ozadje, ikona levo zgoraj, krog/kljukica desno zgoraj.
// Opravljena kartica je siva in zbledela IN ima kljukico (stanje ni nikoli le barva).
@Composable
fun HabitGridCard(
    item: HabitCardUi,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = StreakTheme.palette
    val animate = LocalAnimationLevel.current != AnimationLevel.OFF
    val pastel = parseHexColor(item.habit.colorHex)
    val neutral = item.state == DayState.SKIPPED || item.state == DayState.FROZEN || item.state == DayState.VACATION
    val bg by animateColorAsState(
        targetValue = if (item.isDone || neutral) palette.cardDone else pastel,
        animationSpec = if (animate) spring() else snap(),
        label = "cardBg"
    )
    val ink = if (item.isDone || neutral) palette.cardDoneInk else InkOnPastel
    val checkScale by animateFloatAsState(
        targetValue = if (item.isDone) 1f else 0.9f,
        animationSpec = if (animate) spring(dampingRatio = Spring.DampingRatioMediumBouncy) else snap(),
        label = "check"
    )

    val doneLabel = stringResource(R.string.a11y_mark_done)
    val undoLabel = stringResource(R.string.a11y_mark_not_done)
    val detailsLabel = stringResource(R.string.a11y_habit_actions)
    val stateText = stateLabel(item)
    val subtitle = subtitle(item)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 132.dp)
            .background(bg, RoundedCornerShape(22.dp))
            .combinedClickable(
                role = Role.Checkbox,
                onClickLabel = if (item.isDone) undoLabel else doneLabel,
                onLongClickLabel = detailsLabel,
                onClick = onTap,
                onLongClick = onLongPress
            )
            .semantics(mergeDescendants = true) {
                contentDescription = listOf(item.habit.title, subtitle).filter { it.isNotBlank() }.joinToString(", ")
                stateDescription = stateText
                customActions = listOf(
                    CustomAccessibilityAction(if (item.isDone) undoLabel else doneLabel) { onTap(); true },
                    CustomAccessibilityAction(detailsLabel) { onLongPress(); true }
                )
            }
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = IconMap.resolve(item.habit.icon),
                contentDescription = null,
                tint = ink,
                modifier = Modifier.size(26.dp)
            )
            Spacer(Modifier.weight(1f))
            StatusBadge(item, ink, Modifier.scale(checkScale))
        }
        Text(
            item.habit.title,
            style = MaterialTheme.typography.titleMedium,
            color = ink,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 18.dp)
        )
        if (subtitle.isNotBlank()) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = ink.copy(alpha = 0.8f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (item.habit.goalType != GoalType.YES_NO && item.state == DayState.PARTIAL) {
            LinearProgressIndicator(
                progress = { item.progress },
                color = InkOnPastel,
                trackColor = Color.White.copy(alpha = 0.5f),
                strokeCap = StrokeCap.Round,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun StatusBadge(item: HabitCardUi, ink: Color, modifier: Modifier) {
    val size = 26.dp
    when (item.state) {
        DayState.DONE -> Box(
            modifier.size(size).background(StreakTheme.palette.ink, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.surface, modifier = Modifier.size(17.dp))
        }
        DayState.SKIPPED, DayState.VACATION -> Icon(Icons.Filled.SkipNext, null, tint = ink, modifier = modifier.size(size))
        DayState.FROZEN -> Icon(Icons.Filled.AcUnit, null, tint = ink, modifier = modifier.size(size))
        else -> Box(modifier.size(size).border(2.dp, ink, CircleShape))
    }
}

@Composable
private fun subtitle(item: HabitCardUi): String {
    val habit = item.habit
    return when (habit.goalType) {
        GoalType.YES_NO -> habit.description
        GoalType.QUANTITY -> {
            val unit = habit.unit.orEmpty()
            stringResource(R.string.habit_progress_quantity, formatAmount(item.value), formatAmount(habit.targetValue), unit).trim()
        }
        GoalType.DURATION -> {
            val minutes = (habit.targetValue / 60).toInt()
            val done = (item.value / 60).toInt()
            stringResource(R.string.habit_progress_minutes, done, minutes)
        }
    }
}

@Composable
private fun stateLabel(item: HabitCardUi): String = when (item.state) {
    DayState.DONE -> stringResource(R.string.state_done)
    DayState.PARTIAL -> stringResource(R.string.state_partial)
    DayState.SKIPPED -> stringResource(R.string.day_skipped)
    DayState.FROZEN -> stringResource(R.string.day_frozen)
    DayState.VACATION -> stringResource(R.string.day_vacation)
    DayState.MISSED -> stringResource(R.string.day_missed)
    else -> stringResource(R.string.state_not_done)
} + if (item.currentStreak > 0) ", " + pluralStringResource(R.plurals.streak_days_a11y, item.currentStreak, item.currentStreak) else ""

// 8.0 -> "8", 2.5 -> "2.5"
fun formatAmount(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else "%.1f".format(value)
