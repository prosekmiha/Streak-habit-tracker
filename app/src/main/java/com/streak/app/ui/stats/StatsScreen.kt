package com.streak.app.ui.stats

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.streak.app.R
import com.streak.app.domain.model.HabitWithStreak
import com.streak.app.ui.components.Heatmap
import com.streak.app.ui.components.IconMap
import com.streak.app.ui.components.WeeklyBars
import com.streak.app.ui.components.parseHexColor

@Composable
fun StatsScreen(viewModel: StatsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val selected = state.selected

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
    ) {
        Text(
            text = stringRes(R.string.nav_stats),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(vertical = 14.dp)
        )

        // Izbira navade (vrstica cipov)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 16.dp)) {
            items(state.habits, key = { it.habit.id }) { item ->
                HabitChip(item = item, selected = item.habit.id == selected?.habit?.id) { viewModel.select(item.habit.id) }
            }
        }

        if (selected == null) {
            Text("Ni navad za prikaz", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@Column
        }

        val color = parseHexColor(selected.habit.colorHex)

        // Trojni stat
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatBox(stringRes(R.string.stat_current), "${selected.streak.currentStreak}", stringRes(R.string.stat_days), Modifier.weight(1f))
            StatBox(stringRes(R.string.stat_best), "${selected.streak.bestStreak}", stringRes(R.string.stat_days), Modifier.weight(1f))
            StatBox(stringRes(R.string.stat_month), "${state.monthRate}", "%", Modifier.weight(1f))
        }

        // Tedenski graf
        SectionCard(title = stringRes(R.string.weekly_overview)) {
            WeeklyBars(values = state.weeklyValues, modifier = Modifier.padding(top = 8.dp))
        }

        // Letni heatmap
        SectionCard(title = stringRes(R.string.yearly_overview)) {
            Heatmap(statusByDate = state.statusByDate, baseColor = color, modifier = Modifier.padding(top = 8.dp))
        }

        Box(modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun HabitChip(item: HabitWithStreak, selected: Boolean, onClick: () -> Unit) {
    val color = parseHexColor(item.habit.colorHex)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        modifier = Modifier
            .background(
                if (selected) color.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surface,
                RoundedCornerShape(50)
            )
            .then(if (selected) Modifier.border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(50)) else Modifier)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Icon(IconMap.resolve(item.habit.icon), contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Text(item.habit.title, style = MaterialTheme.typography.labelLarge, color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StatBox(label: String, value: String, unit: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
            .padding(vertical = 18.dp, horizontal = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(" $unit", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 6.dp))
        }
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(22.dp))
            .padding(18.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        content()
    }
}

@Composable private fun stringRes(id: Int) = androidx.compose.ui.res.stringResource(id)
