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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.streak.app.R
import com.streak.app.domain.model.DayState
import com.streak.app.domain.model.Task
import com.streak.app.ui.components.IconMap
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

// Pregled: tedenski povzetek, mesecni koledar z delezem opravljenih navad in
// seznam navad/nalog za izbran dan (z moznostjo naknadnega vpisa).
@Composable
fun CalendarTab(viewModel: CalendarViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val locale = LocalConfiguration.current.locales[0]
    val palette = StreakTheme.palette

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp)
    ) {
        WeeklySummary(state)

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = viewModel::prevMonth) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.previous_month))
            }
            Text(
                "${state.month.month.getDisplayName(TextStyle.FULL_STANDALONE, locale).replaceFirstChar { it.titlecase(locale) }} ${state.month.year}",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f).semantics { heading() }
            )
            IconButton(onClick = viewModel::nextMonth) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.next_month))
            }
        }

        // Glave dni v tednu glede na nastavljen prvi dan
        Row(Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
            (0L..6L).map { state.firstDayOfWeek.plus(it) }.forEach { d ->
                Text(
                    d.getDisplayName(TextStyle.SHORT_STANDALONE, locale).replaceFirstChar { it.titlecase(locale) },
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.inkSoft
                )
            }
        }

        val first = state.month.atDay(1)
        val leading = ((first.dayOfWeek.value - state.firstDayOfWeek.value) + 7) % 7
        val cells = List(leading) { null } + (1..state.month.lengthOfMonth()).toList()
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                (week + List(7 - week.size) { null }).forEach { dayNum ->
                    Box(Modifier.weight(1f)) {
                        if (dayNum != null) {
                            val date = state.month.atDay(dayNum)
                            DayCell(
                                date = date,
                                ratio = state.completionByDate[date] ?: 0f,
                                isToday = date == state.today,
                                isSelected = date == state.selectedDate,
                                isFuture = date.isAfter(state.today),
                                onClick = { viewModel.selectDate(date) }
                            )
                        }
                    }
                }
            }
        }
        Text(
            stringResource(R.string.calendar_legend),
            style = MaterialTheme.typography.bodySmall,
            color = palette.inkSoft,
            modifier = Modifier.padding(top = 8.dp)
        )

        state.selectedDate?.let { date ->
            DayPanel(
                date = date,
                isFuture = date.isAfter(state.today),
                habits = state.habitsForDate,
                tasks = state.tasksForDate,
                onToggle = { viewModel.toggleHabitOnDate(it.habit, date) }
            )
        }
    }
}

@Composable
private fun WeeklySummary(state: CalendarUiState) {
    val percent = (state.thisWeek.ratio * 100).roundToInt()
    val diff = percent - (state.lastWeek.ratio * 100).roundToInt()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .background(Lime, RoundedCornerShape(22.dp))
            .padding(14.dp)
    ) {
        MascotView(if (percent >= 80) MascotMood.HAPPY else if (percent >= 40) MascotMood.THUMBS_UP else MascotMood.THINKING, Modifier.size(72.dp))
        Column(Modifier.padding(start = 12.dp)) {
            Text(stringResource(R.string.weekly_summary_title), style = MaterialTheme.typography.titleLarge, color = InkOnPastel)
            Text(
                stringResource(R.string.weekly_summary_value, state.thisWeek.done, state.thisWeek.scheduled, percent),
                style = MaterialTheme.typography.bodyMedium,
                color = InkOnPastel
            )
            if (state.lastWeek.scheduled > 0) {
                Text(
                    stringResource(if (diff >= 0) R.string.weekly_summary_up else R.string.weekly_summary_down, kotlin.math.abs(diff)),
                    style = MaterialTheme.typography.bodySmall,
                    color = InkOnPastel
                )
            }
        }
    }
}

@Composable
private fun DayCell(date: LocalDate, ratio: Float, isToday: Boolean, isSelected: Boolean, isFuture: Boolean, onClick: () -> Unit) {
    val palette = StreakTheme.palette
    val shape = RoundedCornerShape(12.dp)
    val bg = when {
        isSelected -> palette.daySelected
        ratio > 0f -> Lime.copy(alpha = 0.3f + 0.7f * ratio)
        else -> palette.surfaceSoft
    }
    val percent = (ratio * 100).roundToInt()
    val description = stringResource(R.string.calendar_day_a11y, date.dayOfMonth, percent)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .aspectRatio(1f)
            .padding(3.dp)
            .background(bg, shape)
            .then(if (isToday && !isSelected) Modifier.border(1.5.dp, palette.ink, shape) else Modifier)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics {
                selected = isSelected
                contentDescription = description
            }
    ) {
        Text(
            date.dayOfMonth.toString(),
            style = MaterialTheme.typography.titleSmall,
            color = when {
                isSelected -> palette.daySelectedInk
                ratio > 0f -> InkOnPastel
                isFuture -> palette.inkSoft
                else -> palette.ink
            }
        )
    }
}

@Composable
private fun DayPanel(date: LocalDate, isFuture: Boolean, habits: List<HabitDay>, tasks: List<Task>, onToggle: (HabitDay) -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    val palette = StreakTheme.palette
    Text(
        date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale)),
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp).semantics { heading() }
    )
    if (habits.isEmpty()) {
        Text(stringResource(R.string.calendar_no_habits), style = MaterialTheme.typography.bodyMedium, color = palette.inkSoft)
    }
    habits.forEach { item ->
        val done = item.state == DayState.DONE
        val stateText = stringResource(dayStateLabel(item.state))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .heightIn(min = 56.dp)
                .background(if (done) palette.cardDone else parseHexColor(item.habit.colorHex), RoundedCornerShape(18.dp))
                .clickable(enabled = !isFuture, role = Role.Checkbox) { onToggle(item) }
                .semantics(mergeDescendants = true) { stateDescription = stateText }
                .padding(horizontal = 14.dp)
        ) {
            val ink = if (done) palette.cardDoneInk else InkOnPastel
            Icon(IconMap.resolve(item.habit.icon), null, tint = ink)
            Text(item.habit.title, style = MaterialTheme.typography.titleMedium, color = ink, modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
            if (done) {
                Box(Modifier.size(24.dp).background(palette.ink, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.surface, modifier = Modifier.size(16.dp))
                }
            } else {
                Text(stateText, style = MaterialTheme.typography.labelMedium, color = ink)
            }
        }
    }
    if (tasks.isNotEmpty()) {
        Text(stringResource(R.string.nav_tasks), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp, bottom = 8.dp))
        tasks.forEach { task ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
                Box(Modifier.size(10.dp).background(priorityColor(task.priority), CircleShape))
                Text(task.title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 10.dp))
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
