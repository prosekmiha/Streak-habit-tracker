package com.streak.app.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.streak.app.R
import com.streak.app.domain.model.DayState
import com.streak.app.domain.model.GoalType
import com.streak.app.domain.model.Habit
import com.streak.app.domain.model.ScheduleType
import com.streak.app.domain.model.TimeOfDay
import com.streak.app.ui.addhabit.timeOfDayLabel
import com.streak.app.ui.components.IconMap
import com.streak.app.ui.components.KpiTile
import com.streak.app.ui.components.RoundIconButton
import com.streak.app.ui.components.formatAmount
import com.streak.app.ui.components.parseHexColor
import com.streak.app.ui.components.shortDayName
import com.streak.app.ui.stats.dayStateLabel
import com.streak.app.ui.theme.InkOnPastel
import com.streak.app.ui.theme.Sky
import com.streak.app.ui.theme.StreakTheme
import com.streak.app.ui.theme.Sun
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun HabitDetailScreen(onBack: () -> Unit, onEdit: (String) -> Unit, viewModel: HabitDetailViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val palette = StreakTheme.palette
    val locale = LocalConfiguration.current.locales[0]
    val snackbar = remember { SnackbarHostState() }
    val futureMsg = stringResource(R.string.future_blocked)
    LaunchedEffect(Unit) { viewModel.blocked.collect { snackbar.showSnackbar(futureMsg) } }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                RoundIconButton(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back), onBack)
                RoundIconButton(Icons.Rounded.Edit, stringResource(R.string.action_edit), { onEdit(viewModel.habitId) })
            }
            val item = state.item ?: return@Column
            val habit = item.habit
            val pastel = parseHexColor(habit.colorHex)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(top = 14.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(pastel)
                    .padding(18.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(58.dp).clip(RoundedCornerShape(18.dp)).background(InkOnPastel)
                ) { androidx.compose.material3.Icon(IconMap.resolve(habit.icon), null, tint = pastel, modifier = Modifier.size(28.dp)) }
                Column(Modifier.padding(start = 14.dp)) {
                    Text(habit.title, style = MaterialTheme.typography.headlineMedium, color = InkOnPastel, modifier = Modifier.semantics { heading() })
                    Text(habitSummary(habit, locale), style = MaterialTheme.typography.labelLarge, color = InkOnPastel, modifier = Modifier.padding(top = 2.dp))
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 12.dp)) {
                KpiTile(item.streak.currentStreak.toString(), streakUnitLabel(item), Color(0xFFFFE7D6), Modifier.weight(1f), big = true)
                KpiTile(item.streak.bestStreak.toString(), stringResource(R.string.stat_best).lowercase(locale), Color(0xFFE4E5FB), Modifier.weight(1f), big = true)
                KpiTile("${(item.streak.completionRate * 100).toInt()}%", stringResource(R.string.days_30), Color(0xFFDCF5B6), Modifier.weight(1f), big = true)
            }

            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 22.dp)) {
                Text(stringResource(R.string.last_5_weeks), style = MaterialTheme.typography.titleMedium, color = palette.ink, modifier = Modifier.weight(1f).semantics { heading() })
                Text(stringResource(R.string.tap_to_backfill), style = MaterialTheme.typography.labelMedium, color = palette.inkSoft)
            }
            Row(Modifier.fillMaxWidth().padding(top = 10.dp)) {
                state.grid.take(7).forEach { d ->
                    Text(shortDayName(d.date.dayOfWeek, locale), style = MaterialTheme.typography.labelSmall, color = palette.inkSoft, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
            state.grid.chunked(7).forEach { week ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 6.dp)) {
                    week.forEach { d -> DayCell(d, Modifier.weight(1f), onClick = { viewModel.toggleDay(d.date) }) }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(top = 10.dp)) {
                Legend(palette.ink, stringResource(R.string.state_done))
                Legend(Sun, stringResource(R.string.state_partial))
                Legend(Sky, stringResource(R.string.legend_skipped))
            }

            if (state.sessions.isNotEmpty()) {
                Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 22.dp)) {
                    Text(stringResource(R.string.detail_sessions), style = MaterialTheme.typography.titleMedium, color = palette.ink, modifier = Modifier.weight(1f).semantics { heading() })
                    Text(
                        stringResource(R.string.detail_sessions_total, stringResource(R.string.duration_minutes_value, (state.sessionTotalSeconds / 60).toInt())) +
                            " · " + pluralStringResource(R.plurals.detail_session_count, state.sessionCount, state.sessionCount),
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.inkSoft
                    )
                }
                val zone = remember { java.time.ZoneId.systemDefault() }
                state.sessions.forEach { s ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 12.dp)) {
                        Text(
                            s.startedAt.atZone(zone).format(DateTimeFormatter.ofPattern("d. MMM · HH:mm", locale)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.ink,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            stringResource(R.string.session_elapsed, s.durationSeconds / 60, s.durationSeconds % 60),
                            style = MaterialTheme.typography.labelLarge,
                            color = palette.ink
                        )
                    }
                    HorizontalDivider(color = palette.border)
                }
            }

            if (state.notes.isNotEmpty()) {
                Text(stringResource(R.string.notes_title), style = MaterialTheme.typography.titleMedium, color = palette.ink, modifier = Modifier.padding(top = 22.dp).semantics { heading() })
                state.notes.forEach { c ->
                    Row(Modifier.padding(vertical = 12.dp)) {
                        Text(c.date.format(DateTimeFormatter.ofPattern("d. MMM", locale)), style = MaterialTheme.typography.labelMedium, color = palette.inkSoft, modifier = Modifier.width(64.dp))
                        Text(c.note.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = palette.ink)
                    }
                    HorizontalDivider(color = palette.border)
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(16.dp))
    }
}

@Composable
private fun DayCell(d: DetailDay, modifier: Modifier, onClick: () -> Unit) {
    val palette = StreakTheme.palette
    val locale = LocalConfiguration.current.locales[0]
    val bg = when (d.state) {
        DayState.DONE -> palette.ink
        DayState.PARTIAL -> Sun
        DayState.SKIPPED, DayState.FROZEN, DayState.VACATION -> Sky
        DayState.FUTURE -> Color.Transparent
        else -> palette.surfaceSoft
    }
    val desc = d.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)) + ", " + stringResource(dayStateLabel(d.state))
    Box(
        modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(9.dp))
            .background(bg)
            .then(if (d.state == DayState.FUTURE) Modifier.border(1.5.dp, palette.border, RoundedCornerShape(9.dp)) else Modifier)
            .clickable(enabled = d.state != DayState.FUTURE, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = desc }
    )
}

@Composable
private fun Legend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(color))
        Text(label, style = MaterialTheme.typography.labelSmall, color = StreakTheme.palette.inkSoft, modifier = Modifier.padding(start = 5.dp))
    }
}

@Composable
private fun streakUnitLabel(item: com.streak.app.domain.model.HabitWithStreak): String = pluralStringResource(
    when (item.streak.unit) {
        com.streak.app.domain.model.StreakUnit.DAY -> R.plurals.streak_label_days
        com.streak.app.domain.model.StreakUnit.WEEK -> R.plurals.streak_label_weeks
        com.streak.app.domain.model.StreakUnit.MONTH -> R.plurals.streak_label_months
    },
    item.streak.currentStreak
)

// Povzetek: cilj · urnik · del dneva
@Composable
fun habitSummary(habit: Habit, locale: java.util.Locale): String {
    val goal = when (habit.goalType) {
        GoalType.YES_NO -> null
        GoalType.QUANTITY -> "${formatAmount(habit.targetValue)} ${habit.unit.orEmpty()}".trim()
        GoalType.DURATION -> stringResource(R.string.duration_minutes_value, (habit.targetValue / 60).toInt())
    }
    val schedule = when (habit.schedule.type) {
        ScheduleType.DAILY -> stringResource(R.string.interval_daily).lowercase(locale)
        ScheduleType.WEEKDAYS -> habit.schedule.weekdays.sortedBy { it.value }.joinToString(", ") { shortDayName(it, locale).lowercase(locale) }
        ScheduleType.TIMES_PER_WEEK -> stringResource(R.string.times_per_week_value, habit.schedule.timesPerPeriod)
        ScheduleType.TIMES_PER_MONTH -> stringResource(R.string.times_per_month_value, habit.schedule.timesPerPeriod)
    }
    val tod = if (habit.timeOfDay == TimeOfDay.ANYTIME) null else timeOfDayLabel(habit.timeOfDay).lowercase(locale)
    return listOfNotNull(goal, schedule, tod).joinToString(" · ")
}
