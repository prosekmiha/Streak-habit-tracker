package com.streak.app.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.streak.app.R
import com.streak.app.domain.model.HabitWithStreak
import com.streak.app.ui.components.Heatmap
import com.streak.app.ui.components.IconMap
import com.streak.app.ui.components.WeeklyBars
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
import java.time.format.TextStyle

@Composable
fun StatsScreen() {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding()) {
        Text(
            stringResource(R.string.nav_stats),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(start = 20.dp, top = 24.dp, bottom = 8.dp).semantics { heading() }
        )
        SecondaryTabRow(selectedTabIndex = tab, containerColor = MaterialTheme.colorScheme.background) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(stringResource(R.string.stats_tab_overview), style = MaterialTheme.typography.labelLarge) })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(stringResource(R.string.stats_tab_habits), style = MaterialTheme.typography.labelLarge) })
        }
        Box(Modifier.weight(1f)) {
            if (tab == 0) CalendarTab() else HabitStatsTab()
        }
    }
}

@Composable
private fun HabitStatsTab(viewModel: StatsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val selected = state.selected
    val locale = LocalConfiguration.current.locales[0]

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp),
            modifier = Modifier.padding(vertical = 14.dp)
        ) {
            items(state.habits, key = { it.habit.id }) { item ->
                HabitChip(item, item.habit.id == selected?.habit?.id) { viewModel.select(item.habit.id) }
            }
        }

        if (selected == null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                MascotView(MascotMood.THINKING, Modifier.size(150.dp))
                Text(stringResource(R.string.stats_empty), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 12.dp))
            }
            return@Column
        }

        val unit = selected.habit.schedule.streakUnit
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Kpi(stringResource(R.string.stat_current), streakText(selected.streak.currentStreak, unit), Sun, Modifier.weight(1f))
                Kpi(stringResource(R.string.stat_best), streakText(selected.streak.bestStreak, unit), Bubblegum, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Kpi(stringResource(R.string.stat_total), selected.streak.totalDone.toString(), Sky, Modifier.weight(1f))
                Kpi(stringResource(R.string.stat_rate), "${state.monthRate} %", Lime, Modifier.weight(1f))
            }
            state.bestWeekday?.let { day ->
                Kpi(
                    stringResource(R.string.stat_best_weekday),
                    day.getDisplayName(TextStyle.FULL_STANDALONE, locale).replaceFirstChar { it.titlecase(locale) },
                    parseHexColor(selected.habit.colorHex),
                    Modifier.fillMaxWidth()
                )
            }
        }

        SectionCard(stringResource(R.string.weekly_overview)) {
            WeeklyBars(values = state.weeklyValues, modifier = Modifier.padding(top = 8.dp))
        }
        SectionCard(stringResource(R.string.yearly_overview)) {
            Heatmap(
                statusByDate = state.statusByDate,
                today = state.today,
                firstDayOfWeek = state.firstDayOfWeek,
                baseColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
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
            .background(if (selected) parseHexColor(item.habit.colorHex) else palette.surfaceSoft, shape)
            .then(if (selected) Modifier.border(2.dp, palette.ink, shape) else Modifier)
            .clickable(role = Role.Tab, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(horizontal = 14.dp)
    ) {
        Icon(IconMap.resolve(item.habit.icon), null, tint = if (selected) InkOnPastel else palette.ink, modifier = Modifier.size(18.dp))
        Text(
            item.habit.title,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) InkOnPastel else palette.ink,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun Kpi(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier
            .background(color, RoundedCornerShape(20.dp))
            .padding(16.dp)
            .semantics(mergeDescendants = true) {}
    ) {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = InkOnPastel)
        Text(label, style = MaterialTheme.typography.bodySmall, color = InkOnPastel, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 14.dp)
            .background(StreakTheme.palette.surfaceSoft, RoundedCornerShape(22.dp))
            .padding(18.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        content()
    }
}
