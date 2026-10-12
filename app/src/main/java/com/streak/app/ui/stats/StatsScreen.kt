package com.streak.app.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.streak.app.R
import com.streak.app.domain.model.HabitWithStreak
import com.streak.app.domain.model.StreakUnit
import com.streak.app.ui.components.ButtonVariant
import com.streak.app.ui.components.IconMap
import com.streak.app.ui.components.KpiTile
import com.streak.app.ui.components.ScreenReadyTrace
import com.streak.app.ui.components.ScreenTitle
import com.streak.app.ui.components.SegmentedControl
import com.streak.app.ui.components.StreakButton
import com.streak.app.ui.components.parseHexColor
import com.streak.app.ui.home.streakText
import com.streak.app.ui.mascot.MascotMood
import com.streak.app.ui.mascot.MascotView
import com.streak.app.ui.theme.Bubblegum
import com.streak.app.ui.theme.InkOnPastel
import com.streak.app.ui.theme.Lime
import com.streak.app.ui.theme.Sky
import com.streak.app.ui.theme.StreakTheme
import com.streak.app.ui.theme.Sun
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle

@Composable
fun StatsScreen(onOpenHabit: (String) -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding()) {
        ScreenTitle(stringResource(R.string.nav_stats), Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp))
        SegmentedControl(
            options = listOf(0, 1),
            selected = tab,
            label = { stringResource(if (it == 0) R.string.stats_tab_overview else R.string.stats_tab_habits) },
            onSelect = { tab = it },
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 14.dp)
        )
        Box(Modifier.weight(1f)) {
            if (tab == 0) CalendarTab() else HabitStatsTab(onOpenHabit)
        }
    }
}

@Composable
private fun HabitStatsTab(onOpenHabit: (String) -> Unit, viewModel: StatsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ScreenReadyTrace("StatsHabits.ready", state.loaded)
    val selected = state.selected
    val locale = LocalConfiguration.current.locales[0]
    val palette = StreakTheme.palette

    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 28.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 20.dp)) {
            items(state.habits, key = { it.habit.id }) { item ->
                HabitChip(item, item.habit.id == selected?.habit?.id) { viewModel.select(item.habit.id) }
            }
        }

        if (selected == null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                MascotView(MascotMood.THINKING, Modifier.size(width = 150.dp, height = 162.dp))
                Text(stringResource(R.string.stats_empty), style = MaterialTheme.typography.bodyLarge, color = palette.ink, modifier = Modifier.padding(top = 12.dp))
            }
            return@Column
        }

        val unit = selected.streak.unit
        Column(Modifier.padding(horizontal = 20.dp).padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KpiTile(streakShort(selected.streak.currentStreak, unit), stringResource(R.string.current_streak), Sun, Modifier.weight(1f), big = true)
                KpiTile(streakShort(selected.streak.bestStreak, unit), stringResource(R.string.stat_best).lowercase(locale), Bubblegum, Modifier.weight(1f), big = true)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KpiTile(selected.streak.totalDone.toString(), stringResource(R.string.stat_total).lowercase(locale), Sky, Modifier.weight(1f), big = true)
                KpiTile("${state.monthRate} %", stringResource(R.string.stat_rate).lowercase(locale), Lime, Modifier.weight(1f), big = true)
            }

            // Zadnjih 8 tednov
            Column(
                Modifier
                    .padding(top = 4.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(palette.surfaceSoft)
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(stringResource(R.string.weekly_overview), style = MaterialTheme.typography.titleSmall, color = palette.ink, modifier = Modifier.weight(1f).semantics { heading() })
                    Text(stringResource(R.string.share_of_goal), style = MaterialTheme.typography.labelMedium, color = palette.inkSoft)
                }
                WeekBars(state.weeklyValues)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoTile(
                    stringResource(R.string.stat_best_weekday),
                    state.bestWeekday?.getDisplayName(TextStyle.FULL_STANDALONE, locale)?.replaceFirstChar { it.titlecase(locale) } ?: "–",
                    Color(0xFFE4E5FB),
                    Modifier.weight(1f)
                )
                InfoTile(
                    stringResource(R.string.typical_time),
                    state.typicalTime?.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)) ?: "–",
                    Color(0xFFFFE7D6),
                    Modifier.weight(1f)
                )
            }
            StreakButton(
                stringResource(R.string.open_details),
                { onOpenHabit(selected.habit.id) },
                Modifier.padding(top = 6.dp),
                ButtonVariant.Secondary,
                icon = Icons.AutoMirrored.Rounded.KeyboardArrowRight
            )
        }
    }
}

@Composable
private fun streakShort(count: Int, unit: StreakUnit): String = streakText(count, unit).substringBeforeLast(' ')

@Composable
private fun WeekBars(values: List<Float>) {
    val palette = StreakTheme.palette
    val percents = values.map { (it * 100).toInt() }
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .padding(top = 12.dp)
            .fillMaxWidth()
            .height(110.dp)
            .semantics(mergeDescendants = true) { contentDescription = percents.joinToString(", ") { "$it %" } }
    ) {
        values.forEachIndexed { i, v ->
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight(v.coerceIn(0.04f, 1f))
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (i == values.lastIndex) palette.ink else Lime)
            )
        }
    }
}

@Composable
private fun InfoTile(label: String, value: String, color: Color, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(20.dp)).background(color).padding(horizontal = 16.dp, vertical = 14.dp).semantics(mergeDescendants = true) {}) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = InkOnPastel)
        Text(value, style = MaterialTheme.typography.titleLarge, color = InkOnPastel, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun HabitChip(item: HabitWithStreak, selected: Boolean, onClick: () -> Unit) {
    val palette = StreakTheme.palette
    val shape = RoundedCornerShape(14.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .heightIn(min = 44.dp)
            .clip(shape)
            .background(if (selected) parseHexColor(item.habit.colorHex) else palette.surfaceSoft)
            .border(2.5.dp, if (selected) palette.ink else Color.Transparent, shape)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 14.dp)
    ) {
        Icon(IconMap.resolve(item.habit.icon), null, tint = if (selected) InkOnPastel else palette.ink, modifier = Modifier.size(18.dp))
        Text(item.habit.title, style = MaterialTheme.typography.labelLarge, color = if (selected) InkOnPastel else palette.ink, modifier = Modifier.padding(start = 8.dp))
    }
}
