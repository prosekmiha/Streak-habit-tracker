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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.streak.app.R
import com.streak.app.ui.components.KpiTile
import com.streak.app.ui.components.MonthGrid
import com.streak.app.ui.components.RoundIconButton
import com.streak.app.ui.mood.MoodRequest
import com.streak.app.ui.mood.MoodSheet
import com.streak.app.ui.mood.MoodUi
import com.streak.app.ui.theme.InkOnPastel
import com.streak.app.ui.theme.StreakTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun MoodTab(viewModel: MoodStatsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<LocalDate?>(null) }
    MoodStatsContent(state, onPrev = viewModel::prevMonth, onNext = viewModel::nextMonth, onSelectDate = { editing = it })
    editing?.let { date ->
        MoodSheet(request = MoodRequest(date = date), onDismiss = { editing = null })
    }
}

// Mesec z barvo razpolozenja, povprecje in povezava z navadami (zadnjih 90 dni)
@Composable
fun MoodStatsContent(state: MoodStatsUiState, onPrev: () -> Unit, onNext: () -> Unit, onSelectDate: (LocalDate) -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    val palette = StreakTheme.palette
    val stats = state.stats

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (stats.entries == 0) {
            Text(stringResource(R.string.mood_stats_empty), style = MaterialTheme.typography.bodyLarge, color = palette.ink)
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KpiTile(score(stats.average, locale), stringResource(R.string.mood_stats_average), MoodUi.color(level(stats.average)), Modifier.weight(1f), big = true)
                KpiTile(stats.entries.toString(), stringResource(R.string.mood_stats_entries), Color(0xFFE4E5FB), Modifier.weight(1f), big = true)
            }
            if (stats.hasCorrelation) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KpiTile(score(stats.goodDaysAverage, locale), stringResource(R.string.mood_stats_good_days), MoodUi.color(level(stats.goodDaysAverage)), Modifier.weight(1f))
                    KpiTile(score(stats.otherDaysAverage, locale), stringResource(R.string.mood_stats_other_days), MoodUi.color(level(stats.otherDaysAverage)), Modifier.weight(1f))
                }
            } else {
                Text(stringResource(R.string.mood_stats_need_more), style = MaterialTheme.typography.bodySmall, color = palette.inkSoft)
            }
            if (state.topHabitTitle != null && stats.topHabitAverage != null) {
                KpiTile(
                    state.topHabitTitle,
                    stringResource(R.string.mood_stats_top_habit, score(stats.topHabitAverage, locale)),
                    MoodUi.color(level(stats.topHabitAverage)),
                    Modifier.fillMaxWidth()
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 10.dp)) {
            RoundIconButton(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, stringResource(R.string.previous_month), onPrev)
            Text(
                "${state.month.month.getDisplayName(TextStyle.FULL_STANDALONE, locale).replaceFirstChar { it.titlecase(locale) }} ${state.month.year}",
                style = MaterialTheme.typography.titleLarge,
                color = palette.ink,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            RoundIconButton(Icons.AutoMirrored.Rounded.KeyboardArrowRight, stringResource(R.string.next_month), onNext)
        }
        MonthGrid(state.month, state.firstDayOfWeek, cellSpacing = 3.dp) { date ->
            MoodDayCell(date, state.moodByDate[date], isToday = date == state.today, isFuture = date.isAfter(state.today)) { onSelectDate(date) }
        }
        Text(stringResource(R.string.mood_calendar_legend), style = MaterialTheme.typography.bodySmall, color = palette.inkSoft)
    }
}

@Composable
private fun MoodDayCell(date: LocalDate, mood: Int?, isToday: Boolean, isFuture: Boolean, onClick: () -> Unit) {
    val palette = StreakTheme.palette
    val locale = LocalConfiguration.current.locales[0]
    val shape = RoundedCornerShape(11.dp)
    val description = listOfNotNull(
        date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)),
        mood?.let { stringResource(MoodUi.label(it)) }
    ).joinToString(", ")
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .padding(horizontal = 2.5.dp)
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(shape)
            .background(if (mood != null) MoodUi.color(mood) else palette.surfaceSoft)
            .then(if (isToday) Modifier.border(2.5.dp, palette.ink, shape) else Modifier)
            .clickable(enabled = !isFuture, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description }
    ) {
        Text(
            date.dayOfMonth.toString(),
            style = MaterialTheme.typography.titleSmall,
            color = if (isFuture) palette.inkSoft else if (mood != null) InkOnPastel else palette.ink
        )
    }
}

// "3,6 / 5" v zapisu jezika
private fun score(value: Double?, locale: Locale): String =
    if (value == null) "–" else String.format(locale, "%.1f / 5", value)

private fun level(value: Double?): Int = (value ?: 3.0).roundToInt().coerceIn(MoodUi.levels)
