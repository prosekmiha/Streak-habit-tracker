package com.streak.app.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.streak.app.R
import com.streak.app.domain.model.DayState
import com.streak.app.ui.components.BadgeKind
import com.streak.app.ui.components.IconMap
import com.streak.app.ui.components.KpiTile
import com.streak.app.ui.components.MonthGrid
import com.streak.app.ui.components.RoundIconButton
import com.streak.app.ui.components.ScreenReadyTrace
import com.streak.app.ui.components.StatusBadge
import com.streak.app.ui.components.StreakSheet
import com.streak.app.ui.components.parseHexColor
import com.streak.app.ui.mascot.MascotMood
import com.streak.app.ui.mascot.MascotView
import com.streak.app.ui.tasks.priorityColor
import com.streak.app.ui.theme.InkOnPastel
import com.streak.app.ui.theme.Lime
import com.streak.app.ui.theme.StreakTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import kotlin.math.roundToInt

// Pregled: tedenski povzetek, kazalnika in mesecni koledar (dotik dneva odpre list z navadami)
@Composable
fun CalendarTab(viewModel: CalendarViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ScreenReadyTrace("Stats.ready", state.loaded)
    val locale = LocalConfiguration.current.locales[0]
    val palette = StreakTheme.palette

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp)
    ) {
        WeeklySummary(state)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 10.dp)) {
            KpiTile(state.bestStreak.toString(), stringResource(R.string.longest_streak), Color(0xFFFFE7D6), Modifier.weight(1f))
            KpiTile(state.totalDone.toString(), stringResource(R.string.stat_total).lowercase(locale), Color(0xFFE4E5FB), Modifier.weight(1f))
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 18.dp, bottom = 4.dp)) {
            RoundIconButton(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, stringResource(R.string.previous_month), viewModel::prevMonth)
            Text(
                "${state.month.month.getDisplayName(TextStyle.FULL_STANDALONE, locale).replaceFirstChar { it.titlecase(locale) }} ${state.month.year}",
                style = MaterialTheme.typography.titleLarge,
                color = palette.ink,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            RoundIconButton(Icons.AutoMirrored.Rounded.KeyboardArrowRight, stringResource(R.string.next_month), viewModel::nextMonth)
        }
        MonthGrid(state.month, state.firstDayOfWeek, cellSpacing = 3.dp) { date ->
            DayCell(
                date = date,
                ratio = state.completionByDate[date] ?: 0f,
                isToday = date == state.today,
                isFuture = date.isAfter(state.today),
                onClick = { viewModel.selectDate(date) }
            )
        }
        Text(stringResource(R.string.calendar_legend), style = MaterialTheme.typography.bodySmall, color = palette.inkSoft, modifier = Modifier.padding(top = 10.dp))
    }

    state.selectedDate?.let { date ->
        DaySheet(
            date = date,
            isFuture = date.isAfter(state.today),
            habits = state.habitsForDate,
            tasks = state.tasksForDate,
            onToggle = { viewModel.toggleHabitOnDate(it.habit, date) },
            onDismiss = { viewModel.selectDate(null) }
        )
    }
}

@Composable
private fun WeeklySummary(state: CalendarUiState) {
    val percent = (state.thisWeek.ratio * 100).roundToInt()
    val diff = percent - (state.lastWeek.ratio * 100).roundToInt()
    Box(
        Modifier
            .fillMaxWidth()
            .height(128.dp)
            .padding(top = 0.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(Lime)
    ) {
        Column(Modifier.align(Alignment.CenterStart).padding(start = 18.dp).fillMaxWidth(0.66f)) {
            Text(stringResource(R.string.weekly_summary_title), style = MaterialTheme.typography.titleSmall, color = InkOnPastel)
            Text("$percent %", style = MaterialTheme.typography.displaySmall, color = InkOnPastel)
            val compare = if (state.lastWeek.scheduled > 0) {
                " · " + stringResource(if (diff >= 0) R.string.weekly_summary_up else R.string.weekly_summary_down, kotlin.math.abs(diff))
            } else ""
            Text(
                stringResource(R.string.weekly_summary_count, state.thisWeek.done, state.thisWeek.scheduled) + compare,
                style = MaterialTheme.typography.labelMedium,
                color = InkOnPastel
            )
        }
        MascotView(
            if (percent >= 80) MascotMood.HAPPY else if (percent >= 40) MascotMood.THUMBS_UP else MascotMood.THINKING,
            Modifier.align(Alignment.BottomEnd).offset(x = 4.dp, y = 24.dp).size(width = 118.dp, height = 128.dp)
        )
    }
}

@Composable
private fun DayCell(date: LocalDate, ratio: Float, isToday: Boolean, isFuture: Boolean, onClick: () -> Unit) {
    val palette = StreakTheme.palette
    val locale = LocalConfiguration.current.locales[0]
    val shape = RoundedCornerShape(11.dp)
    val bg = when {
        isFuture -> palette.surfaceSoft
        ratio >= 1f -> Lime
        ratio > 0.5f -> Color(0xFFD3F2A4)
        ratio > 0f -> Color(0xFFEAF8D6)
        else -> palette.surfaceSoft
    }
    val percent = (ratio * 100).roundToInt()
    val description = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)) + ", " +
        stringResource(R.string.calendar_day_percent, percent)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .padding(horizontal = 2.5.dp)
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(shape)
            .background(bg)
            .then(if (isToday) Modifier.border(2.5.dp, palette.ink, shape) else Modifier)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description }
    ) {
        Text(
            date.dayOfMonth.toString(),
            style = MaterialTheme.typography.titleSmall,
            color = if (isFuture) palette.inkSoft else if (ratio > 0f) InkOnPastel else palette.ink
        )
    }
}

// List z navadami in nalogami izbranega dne; dotik navade preklopi naknadni vpis
@Composable
private fun DaySheet(
    date: LocalDate,
    isFuture: Boolean,
    habits: List<HabitDay>,
    tasks: List<com.streak.app.domain.model.Task>,
    onToggle: (HabitDay) -> Unit,
    onDismiss: () -> Unit
) {
    val locale = LocalConfiguration.current.locales[0]
    val palette = StreakTheme.palette
    StreakSheet(
        onDismiss = onDismiss,
        title = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale)).replaceFirstChar { it.titlecase(locale) },
        showClose = true
    ) {
        if (habits.isEmpty()) {
            Text(stringResource(R.string.calendar_no_habits), style = MaterialTheme.typography.bodyLarge, color = palette.inkSoft)
        }
        habits.forEach { item ->
            val done = item.state == DayState.DONE
            val stateText = stringResource(dayStateLabel(item.state))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .heightIn(min = 60.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (done) palette.cardDone else parseHexColor(item.habit.colorHex))
                    .toggleable(value = done, enabled = !isFuture, role = Role.Checkbox) { onToggle(item) }
                    .semantics(mergeDescendants = true) { stateDescription = stateText }
                    .padding(horizontal = 14.dp)
            ) {
                val ink = if (done) palette.cardDoneInk else InkOnPastel
                Icon(IconMap.resolve(item.habit.icon), null, tint = ink)
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(item.habit.title, style = MaterialTheme.typography.titleMedium, color = ink)
                    if (!done) Text(stateText, style = MaterialTheme.typography.labelMedium, color = ink.copy(alpha = .8f))
                }
                StatusBadge(if (done) BadgeKind.Done else BadgeKind.Ring, ink)
            }
        }
        if (tasks.isNotEmpty()) {
            Text(stringResource(R.string.nav_tasks), style = MaterialTheme.typography.titleMedium, color = palette.ink, modifier = Modifier.padding(top = 8.dp, bottom = 6.dp))
            tasks.forEach { task ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(priorityColor(task.priority)))
                    Text(task.title, style = MaterialTheme.typography.bodyLarge, color = palette.ink, modifier = Modifier.padding(start = 10.dp))
                }
            }
        }
    }
}

fun dayStateLabel(state: DayState): Int = when (state) {
    DayState.DONE -> R.string.state_done
    DayState.PARTIAL -> R.string.state_partial
    DayState.SKIPPED -> R.string.day_skipped
    DayState.FROZEN -> R.string.day_frozen
    DayState.VACATION -> R.string.day_vacation
    DayState.MISSED -> R.string.day_missed
    DayState.FUTURE -> R.string.week_day_future
    else -> R.string.state_not_done
}
