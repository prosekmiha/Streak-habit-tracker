package com.streak.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
import com.streak.app.domain.model.AnimationLevel
import com.streak.app.domain.model.DayState
import com.streak.app.domain.model.GoalType
import com.streak.app.domain.model.Habit
import com.streak.app.domain.model.StreakUnit
import com.streak.app.ui.theme.InkOnPastel
import com.streak.app.ui.theme.LocalAnimationLevel
import com.streak.app.ui.theme.StreakTheme

data class HabitCardUi(
    val habit: Habit,
    val state: DayState,
    val value: Double,
    val currentStreak: Int,
    val bestStreak: Int = 0,
    val unit: StreakUnit = StreakUnit.DAY,
    val note: String? = null
) {
    val isDone: Boolean get() = state == DayState.DONE
    val isNeutral: Boolean get() = state == DayState.SKIPPED || state == DayState.FROZEN || state == DayState.VACATION
    val progress: Float
        get() = if (habit.targetValue <= 0.0) 0f else (value / habit.targetValue).toFloat().coerceIn(0f, 1f)
}

// Kartica navade po dizajnu: pastelno ozadje, polna ikona levo zgoraj, znacka desno zgoraj
// (krog / krog napredka / gumb za sejo / crna kljukica). Opravljena = siva + kljukica.
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
    val faded = item.isDone || item.isNeutral
    val bg by animateColorAsState(if (faded) palette.cardDone else pastel, if (animate) spring() else snap(), label = "cardBg")
    val ink = if (faded) palette.cardDoneInk else InkOnPastel
    val badgeScale by animateFloatAsState(
        if (item.isDone) 1f else 0.92f,
        if (animate) spring(dampingRatio = Spring.DampingRatioMediumBouncy) else snap(),
        label = "badge"
    )
    val badge = when {
        item.isDone -> BadgeKind.Done
        item.habit.goalType == GoalType.DURATION && !item.isNeutral -> BadgeKind.Play
        item.habit.goalType == GoalType.QUANTITY && item.value > 0 -> BadgeKind.Progress
        else -> BadgeKind.Ring
    }

    val doneLabel = stringResource(if (item.habit.goalType == GoalType.QUANTITY) R.string.a11y_add_one else R.string.a11y_mark_done)
    val undoLabel = stringResource(R.string.a11y_mark_not_done)
    val sessionLabel = stringResource(R.string.a11y_start_session)
    val detailsLabel = stringResource(R.string.a11y_habit_actions)
    val tapLabel = when {
        item.habit.isQuit -> detailsLabel
        item.isDone -> undoLabel
        badge == BadgeKind.Play -> sessionLabel
        else -> doneLabel
    }
    val stateText = stateLabel(item)
    val subtitle = cardSubtitle(item)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 136.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(bg)
            .combinedClickable(
                role = Role.Button,
                onClickLabel = tapLabel,
                onLongClickLabel = detailsLabel,
                onClick = onTap,
                onLongClick = onLongPress
            )
            .semantics(mergeDescendants = true) {
                contentDescription = listOf(item.habit.title, subtitle).filter { it.isNotBlank() }.joinToString(", ")
                stateDescription = stateText
                customActions = listOf(
                    CustomAccessibilityAction(tapLabel) { onTap(); true },
                    CustomAccessibilityAction(detailsLabel) { onLongPress(); true }
                )
            }
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(IconMap.resolve(item.habit.icon), contentDescription = null, tint = ink, modifier = Modifier.size(26.dp))
            Spacer(Modifier.weight(1f))
            StatusBadge(badge, ink, Modifier.scale(badgeScale), progress = item.progress)
        }
        Text(
            item.habit.title,
            style = MaterialTheme.typography.titleMedium,
            color = ink,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 26.dp)
        )
        if (subtitle.isNotBlank()) {
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = ink.copy(alpha = 0.82f), maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun cardSubtitle(item: HabitCardUi): String {
    val habit = item.habit
    return when (item.state) {
        DayState.SKIPPED -> stringResource(R.string.day_skipped)
        DayState.FROZEN -> stringResource(R.string.day_frozen)
        DayState.VACATION -> stringResource(R.string.day_vacation)
        else -> when (habit.goalType) {
            GoalType.QUIT -> if (item.state == DayState.MISSED) {
                stringResource(R.string.quit_relapses_today, item.value.toInt().coerceAtLeast(1))
            } else stringResource(R.string.quit_clean_days, item.currentStreak)
            GoalType.YES_NO -> habit.description
            GoalType.QUANTITY -> stringResource(R.string.habit_progress_quantity, formatAmount(item.value), formatAmount(habit.targetValue), habit.unit.orEmpty()).trim()
            GoalType.DURATION -> if (item.value > 0) {
                stringResource(R.string.habit_progress_minutes, (item.value / 60).toInt(), (habit.targetValue / 60).toInt())
            } else stringResource(R.string.duration_minutes_value, (habit.targetValue / 60).toInt())
        }
    }
}

@Composable
private fun stateLabel(item: HabitCardUi): String = when (item.state) {
    DayState.DONE -> stringResource(if (item.habit.isQuit) R.string.state_clean else R.string.state_done)
    DayState.MISSED -> stringResource(if (item.habit.isQuit) R.string.state_relapse else R.string.day_missed)
    DayState.PARTIAL -> stringResource(R.string.state_partial)
    DayState.SKIPPED -> stringResource(R.string.day_skipped)
    DayState.FROZEN -> stringResource(R.string.day_frozen)
    DayState.VACATION -> stringResource(R.string.day_vacation)
    else -> stringResource(R.string.state_not_done)
} + if (item.currentStreak > 0) ", " + pluralStringResource(R.plurals.streak_days_a11y, item.currentStreak, item.currentStreak) else ""
