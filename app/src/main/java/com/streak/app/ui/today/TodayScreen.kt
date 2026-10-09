package com.streak.app.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.streak.app.R
import com.streak.app.domain.model.HabitWithStreak
import com.streak.app.domain.model.Priority
import com.streak.app.domain.model.Task
import com.streak.app.domain.model.TimeOfDay
import com.streak.app.ui.components.FilterChipsRow
import com.streak.app.ui.components.HabitCard
import com.streak.app.ui.components.ProgressRing
import com.streak.app.ui.theme.Primary
import com.streak.app.ui.theme.Success
import com.streak.app.ui.theme.Warning
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun TodayScreen(
    onHabitClick: (String) -> Unit,
    onTaskClick: (String) -> Unit,
    viewModel: TodayViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var filter by remember { mutableIntStateOf(0) }

    val filterLabels = listOf(
        stringRes(R.string.filter_all),
        stringRes(R.string.filter_morning),
        stringRes(R.string.filter_afternoon),
        stringRes(R.string.filter_evening)
    )
    val visibleHabits = state.habits.filter { partOfDayIndex(it) == 0 || filter == 0 || partOfDayIndex(it) == filter }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { GreetingHeader(state.userName) }
        item { HeroCard(progress = state.progress, done = state.doneCount, total = state.totalCount, streak = state.bestStreak) }

        item {
            FilterChipsRow(
                options = filterLabels,
                selectedIndex = filter,
                onSelect = { filter = it },
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        item {
            SectionLabel(stringRes(R.string.today_habits))
        }

        if (visibleHabits.isEmpty()) {
            item { EmptyHint(stringRes(R.string.empty_habits)) }
        } else {
            items(visibleHabits, key = { it.habit.id }) { item ->
                HabitCard(
                    item = item,
                    onToggle = { viewModel.toggle(item) },
                    onClick = { onHabitClick(item.habit.id) }
                )
            }
        }

        item {
            SectionLabel(stringRes(R.string.today_tasks), top = 14.dp)
        }

        if (state.tasks.isEmpty()) {
            item { EmptyHint(stringRes(R.string.empty_tasks)) }
        } else {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    state.tasks.forEach { task ->
                        TaskRow(task = task, onToggle = { viewModel.toggleTask(task) }, onClick = { onTaskClick(task.id) })
                    }
                }
            }
        }

        item { Box(modifier = Modifier.size(24.dp)) }
    }
}

// 0 = celodnevno, 1 = jutro, 2 = popoldne, 3 = vecer
private fun partOfDayIndex(item: HabitWithStreak): Int = when (item.habit.timeOfDay) {
    TimeOfDay.ANYTIME -> 0
    TimeOfDay.MORNING -> 1
    TimeOfDay.AFTERNOON -> 2
    TimeOfDay.EVENING -> 3
}

@Composable
private fun SectionLabel(text: String, top: androidx.compose.ui.unit.Dp = 6.dp) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = top, bottom = 2.dp)
    )
}

@Composable
private fun GreetingHeader(name: String) {
    val today = LocalDate.now()
    val dow = today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.forLanguageTag("sl"))
        .replaceFirstChar { it.uppercase() }
    val dateStr = today.format(DateTimeFormatter.ofPattern("d. MMMM", Locale.forLanguageTag("sl")))
    val hour = java.time.LocalTime.now().hour
    val greet = when {
        hour < 11 -> stringRes(R.string.greeting_morning)
        hour < 18 -> stringRes(R.string.greeting_day)
        else -> stringRes(R.string.greeting_evening)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = 14.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("$dow, $dateStr", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row {
                Text("$greet, ", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    name.ifBlank { "prijatelj" },
                    style = MaterialTheme.typography.headlineMedium,
                    color = Primary
                )
            }
        }
        // Avatar
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(Brush.linearGradient(listOf(Primary, MaterialTheme.colorScheme.secondary)), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.ifBlank { "P" }.take(1).uppercase(),
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
        }
    }
}

@Composable
private fun HeroCard(progress: Float, done: Int, total: Int, streak: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(listOf(Primary, MaterialTheme.colorScheme.secondary)),
                RoundedCornerShape(24.dp)
            )
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        ProgressRing(progress = progress, sizeDp = 78, onColor = Color.White, trackColor = Color.White.copy(alpha = 0.28f))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringRes(R.string.today_progress), style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.8f))
            Text(
                stringResFmt(R.string.habits_done, done, total),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .padding(top = 4.dp)
                    .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 11.dp, vertical = 5.dp)
            ) {
                Icon(Icons.Filled.LocalFireDepartment, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                Text(stringResFmt(R.string.streak_days, streak), style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun TaskRow(task: Task, onToggle: () -> Unit, onClick: () -> Unit) {
    val dotColor = when (task.priority) {
        Priority.URGENT -> Warning
        Priority.NORMAL -> Primary
        Priority.LOW -> Success
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickableNoRipple { onClick() }
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(if (task.isDone) Success else dotColor, CircleShape)
        )
        Text(
            text = task.title,
            style = MaterialTheme.typography.bodyMedium,
            color = if (task.isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
            modifier = Modifier.weight(1f)
        )
        task.dueTime?.let {
            Text(it.format(DateTimeFormatter.ofPattern("HH:mm")), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box(
            modifier = Modifier
                .size(22.dp)
                .then(
                    if (task.isDone) Modifier.background(Success, CircleShape)
                    else Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f), CircleShape)
                )
                .clickableNoRipple { onToggle() },
            contentAlignment = Alignment.Center
        ) {
            if (task.isDone) Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun EmptyHint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
            .padding(16.dp)
    )
}

private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier =
    this.clickable(
        interactionSource = null,
        indication = null,
        onClick = onClick
    )

@Composable private fun stringRes(id: Int) = androidx.compose.ui.res.stringResource(id)
@Composable private fun stringResFmt(id: Int, vararg args: Any) = androidx.compose.ui.res.stringResource(id, *args)
