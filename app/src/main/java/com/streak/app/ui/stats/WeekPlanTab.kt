package com.streak.app.ui.stats

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.streak.app.R
import com.streak.app.domain.model.ScheduleType
import com.streak.app.domain.plan.PlanCell
import com.streak.app.domain.plan.PlanRow
import com.streak.app.domain.plan.WeekPlan
import com.streak.app.ui.components.IconMap
import com.streak.app.ui.components.RoundIconButton
import com.streak.app.ui.components.ScreenReadyTrace
import com.streak.app.ui.components.parseHexColor
import com.streak.app.ui.components.shortDayName
import com.streak.app.ui.mascot.MascotMood
import com.streak.app.ui.mascot.MascotView
import com.streak.app.ui.theme.InkOnPastel
import com.streak.app.ui.theme.Lime
import com.streak.app.ui.theme.Sky
import com.streak.app.ui.theme.StreakTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle

private val LimeSoft = Color(0xFFD3F2A4)

// Tedenski plan: navade po pogostosti, za vsako 7 dni (kaj je kateri dan na urniku)
@Composable
fun WeekPlanTab(onOpenHabit: (String) -> Unit, viewModel: WeekPlanViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ScreenReadyTrace("StatsPlan.ready", state.loaded)
    WeekPlanContent(state, viewModel::prevWeek, viewModel::nextWeek, viewModel::thisWeek, onOpenHabit)
}

@Composable
fun WeekPlanContent(
    state: WeekPlanUiState,
    onPrevWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onThisWeek: () -> Unit,
    onOpenHabit: (String) -> Unit
) {
    val palette = StreakTheme.palette

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
            RoundIconButton(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, stringResource(R.string.previous_week), onPrevWeek)
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Text(
                    weekRange(state.weekStart),
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.ink,
                    textAlign = TextAlign.Center
                )
                if (!state.isCurrentWeek) {
                    Text(
                        stringResource(R.string.weekly_summary_title),
                        style = MaterialTheme.typography.labelLarge,
                        color = palette.ink,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(role = Role.Button, onClick = onThisWeek)
                            .heightIn(min = 48.dp)
                            .padding(horizontal = 12.dp, vertical = 14.dp)
                    )
                }
            }
            RoundIconButton(Icons.AutoMirrored.Rounded.KeyboardArrowRight, stringResource(R.string.next_week), onNextWeek)
        }

        if (state.loaded && state.plan.isEmpty) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                MascotView(MascotMood.THINKING, Modifier.size(width = 150.dp, height = 162.dp))
                Text(
                    stringResource(R.string.plan_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = palette.ink,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
            return@Column
        }
        if (!state.loaded) return@Column

        DayHeader(state.plan, state.today)
        state.plan.groups.forEach { group ->
            Text(
                stringResource(groupTitle(group.type)),
                style = MaterialTheme.typography.titleMedium,
                color = palette.ink,
                modifier = Modifier.padding(top = 18.dp).semantics { heading() }
            )
            Text(groupHint(group.type), style = MaterialTheme.typography.bodySmall, color = palette.inkSoft)
            group.rows.forEach { row -> PlanRowCard(row, state.plan.days) { onOpenHabit(row.habit.id) } }
        }
        Legend(Modifier.padding(top = 18.dp))
    }
}

// Glava stolpcev: dan, datum in stevilo navad na urniku; poravnana s pikami v vrsticah
@Composable
private fun DayHeader(plan: WeekPlan, today: LocalDate) {
    val palette = StreakTheme.palette
    val locale = LocalConfiguration.current.locales[0]
    // Pri veliki pisavi kratka imena ("P", "T") ne bi bila odrezana
    val narrow = LocalDensity.current.fontScale > 1.4f
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        plan.days.forEachIndexed { i, date ->
            val isToday = date == today
            val planned = plan.plannedPerDay[i]
            val description = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale)) + ", " +
                stringResource(R.string.plan_planned_day, planned)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 2.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isToday) palette.daySelected else Color.Transparent)
                    .padding(vertical = 6.dp)
                    .semantics(mergeDescendants = true) { contentDescription = description }
            ) {
                val ink = if (isToday) palette.daySelectedInk else palette.ink
                Text(dayName(date, locale, narrow), style = MaterialTheme.typography.labelMedium, color = ink, maxLines = 1, overflow = TextOverflow.Clip)
                Text(date.dayOfMonth.toString(), style = MaterialTheme.typography.titleSmall, color = ink, maxLines = 1)
                Text(
                    if (planned > 0) planned.toString() else "–",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isToday) palette.daySelectedInk.copy(alpha = .75f) else palette.inkSoft,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun PlanRowCard(row: PlanRow, days: List<LocalDate>, onClick: () -> Unit) {
    val palette = StreakTheme.palette
    val locale = LocalConfiguration.current.locales[0]
    val habitColor = parseHexColor(row.habit.colorHex)
    val progressDescription = stringResource(
        if (row.habit.schedule.type == ScheduleType.TIMES_PER_MONTH) R.string.plan_progress_month else R.string.plan_progress_week,
        row.done, row.target
    )
    Column(
        Modifier
            .padding(top = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(palette.surfaceSoft)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {}
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(30.dp).clip(CircleShape).background(habitColor)) {
                Icon(IconMap.resolve(row.habit.icon), null, tint = InkOnPastel, modifier = Modifier.size(18.dp))
            }
            Text(
                row.habit.title,
                style = MaterialTheme.typography.titleSmall,
                color = palette.ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(horizontal = 10.dp)
            )
            Text(
                "${row.done}/${row.target}",
                style = MaterialTheme.typography.labelLarge,
                color = if (row.target > 0 && row.done >= row.target) InkOnPastel else palette.ink,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (row.target > 0 && row.done >= row.target) Lime else Color.Transparent)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
                    .semantics { contentDescription = progressDescription }
            )
        }
        Row(Modifier.fillMaxWidth().padding(top = 10.dp)) {
            row.cells.forEachIndexed { i, cell ->
                val description = shortDayName(days[i].dayOfWeek, locale) + ": " + stringResource(cellLabel(cell))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.weight(1f).semantics { contentDescription = description }
                ) {
                    PlanDot(cell, habitColor)
                }
            }
        }
    }
}

@Composable
private fun PlanDot(cell: PlanCell, habitColor: Color) {
    val palette = StreakTheme.palette
    val base = Modifier.size(26.dp)
    when (cell) {
        PlanCell.PLANNED -> Box(base.clip(CircleShape).background(habitColor).border(1.5.dp, InkOnPastel.copy(alpha = .35f), CircleShape))
        PlanCell.DONE -> Box(contentAlignment = Alignment.Center, modifier = base.clip(CircleShape).background(Lime)) {
            Icon(Icons.Rounded.Check, null, tint = InkOnPastel, modifier = Modifier.size(18.dp))
        }
        PlanCell.PARTIAL -> Box(base.clip(CircleShape).background(LimeSoft))
        PlanCell.MISSED -> Box(base.border(2.dp, palette.danger, CircleShape))
        PlanCell.SKIPPED -> Box(contentAlignment = Alignment.Center, modifier = base.clip(CircleShape).background(palette.border)) {
            Icon(Icons.Rounded.Remove, null, tint = palette.inkSoft, modifier = Modifier.size(16.dp))
        }
        PlanCell.FLEXIBLE -> {
            val stroke = palette.inkSoft
            Box(base.drawBehind {
                val width = 1.5.dp.toPx()
                drawCircle(
                    color = stroke,
                    radius = size.minDimension / 2 - width,
                    style = Stroke(width, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())))
                )
            })
        }
        PlanCell.OFF, PlanCell.INACTIVE -> Box(base, contentAlignment = Alignment.Center) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(palette.border))
        }
    }
}

@Composable
private fun Legend(modifier: Modifier = Modifier) {
    val palette = StreakTheme.palette
    val locale = LocalConfiguration.current.locales[0]
    // Vrednost null je stevilka iz glave stolpca (navade na urniku ta dan)
    val items = listOf(PlanCell.PLANNED, PlanCell.DONE, PlanCell.MISSED, PlanCell.SKIPPED, PlanCell.FLEXIBLE, null)
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth()) {
                pair.forEach { cell ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        if (cell != null) {
                            PlanDot(cell, Sky)
                        } else {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(26.dp)) {
                                Text("2", style = MaterialTheme.typography.labelSmall, color = palette.inkSoft)
                            }
                        }
                        Text(
                            if (cell != null) stringResource(cellLabel(cell)).replaceFirstChar { it.titlecase(locale) }
                            else stringResource(R.string.plan_legend_count),
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.inkSoft,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
                if (pair.size == 1) Box(Modifier.weight(1f))
            }
        }
    }
}

private fun dayName(date: LocalDate, locale: java.util.Locale, narrow: Boolean): String =
    if (narrow) date.dayOfWeek.getDisplayName(TextStyle.NARROW_STANDALONE, locale).uppercase(locale)
    else shortDayName(date.dayOfWeek, locale)

// Obseg tedna v jeziku naprave, npr. "5. okt. – 11. okt."
@Composable
private fun weekRange(start: LocalDate): String {
    val locale = LocalConfiguration.current.locales[0]
    val formatter = DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "dMMM"), locale)
    return start.format(formatter) + " – " + start.plusDays(6).format(formatter)
}

private fun groupTitle(type: ScheduleType): Int = when (type) {
    ScheduleType.DAILY -> R.string.interval_daily
    ScheduleType.WEEKDAYS -> R.string.interval_weekdays
    ScheduleType.TIMES_PER_WEEK -> R.string.interval_times_week
    ScheduleType.TIMES_PER_MONTH -> R.string.interval_times_month
}

@Composable
private fun groupHint(type: ScheduleType): String = stringResource(
    when (type) {
        ScheduleType.DAILY -> R.string.interval_daily_desc
        ScheduleType.WEEKDAYS -> R.string.plan_weekdays_desc
        ScheduleType.TIMES_PER_WEEK -> R.string.interval_times_week_desc
        ScheduleType.TIMES_PER_MONTH -> R.string.interval_times_month_desc
    }
)

private fun cellLabel(cell: PlanCell): Int = when (cell) {
    PlanCell.PLANNED -> R.string.plan_legend_planned
    PlanCell.DONE -> R.string.state_done
    PlanCell.PARTIAL -> R.string.state_partial
    PlanCell.MISSED -> R.string.day_missed
    PlanCell.SKIPPED -> R.string.day_skipped
    PlanCell.FLEXIBLE -> R.string.plan_legend_flexible
    PlanCell.OFF, PlanCell.INACTIVE -> R.string.plan_cell_off
}
