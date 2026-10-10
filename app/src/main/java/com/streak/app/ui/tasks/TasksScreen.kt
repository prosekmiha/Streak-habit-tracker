package com.streak.app.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.streak.app.R
import com.streak.app.domain.model.Priority
import com.streak.app.domain.model.Task
import com.streak.app.ui.components.BadgeKind
import com.streak.app.ui.components.ScreenTitle
import com.streak.app.ui.components.StatusBadge
import com.streak.app.ui.components.StreakButton
import com.streak.app.ui.mascot.MascotMood
import com.streak.app.ui.mascot.MascotView
import com.streak.app.ui.theme.Bubblegum
import com.streak.app.ui.theme.InkOnPastel
import com.streak.app.ui.theme.Lime
import com.streak.app.ui.theme.Sky
import com.streak.app.ui.theme.StreakTheme
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun TasksScreen(
    onAddTask: () -> Unit,
    onEditTask: (String) -> Unit,
    viewModel: TasksViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val palette = StreakTheme.palette
    val open = state.groups.filter { it.first != TaskGroup.DONE }.sumOf { it.second.size }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            ScreenTitle(stringResource(R.string.nav_tasks), Modifier.padding(top = 22.dp, bottom = 6.dp)) {
                if (open > 0) {
                    Text(
                        pluralStringResource(R.plurals.tasks_open, open, open),
                        style = MaterialTheme.typography.labelLarge,
                        color = palette.inkSoft,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
            }
        }
        if (state.loaded && state.groups.isEmpty()) {
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(top = 28.dp)) {
                    MascotView(MascotMood.SLEEPING, Modifier.size(width = 160.dp, height = 173.dp))
                    Text(stringResource(R.string.tasks_empty), style = MaterialTheme.typography.bodyLarge, color = palette.ink, modifier = Modifier.padding(vertical = 16.dp))
                    StreakButton(stringResource(R.string.add_task), onAddTask)
                }
            }
        }
        state.groups.forEach { (group, tasks) ->
            item(key = group.name) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 14.dp)) {
                    Text(
                        stringResource(group.label()),
                        style = MaterialTheme.typography.titleSmall,
                        color = if (group == TaskGroup.OVERDUE) palette.danger else palette.ink,
                        modifier = Modifier.semantics { heading() }
                    )
                    Text(
                        tasks.size.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.inkSoft,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(palette.surfaceSoft)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            items(tasks, key = { it.id }) { task ->
                TaskRow(task, onToggle = { viewModel.toggle(task) }, onClick = { onEditTask(task.id) })
            }
        }
    }
}

private fun TaskGroup.label(): Int = when (this) {
    TaskGroup.OVERDUE -> R.string.tasks_overdue
    TaskGroup.TODAY -> R.string.tasks_today
    TaskGroup.UPCOMING -> R.string.tasks_upcoming
    TaskGroup.NO_DATE -> R.string.tasks_no_date
    TaskGroup.DONE -> R.string.tasks_done
}

fun priorityColor(priority: Priority): Color = when (priority) {
    Priority.URGENT -> Bubblegum
    Priority.NORMAL -> Sky
    Priority.LOW -> Lime
}

@Composable
fun priorityLabel(priority: Priority): String = stringResource(
    when (priority) {
        Priority.URGENT -> R.string.priority_urgent
        Priority.NORMAL -> R.string.priority_normal
        Priority.LOW -> R.string.priority_low
    }
)

@Composable
private fun TaskRow(task: Task, onToggle: () -> Unit, onClick: () -> Unit) {
    val palette = StreakTheme.palette
    val locale = LocalConfiguration.current.locales[0]
    val ink = if (task.isDone) palette.cardDoneInk else palette.ink
    val title = task.title
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(palette.surfaceSoft)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 6.dp, end = 14.dp, top = 6.dp, bottom = 6.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .toggleable(value = task.isDone, role = Role.Checkbox, onValueChange = { onToggle() })
                .semantics { contentDescription = title }
        ) { StatusBadge(if (task.isDone) BadgeKind.Done else BadgeKind.Ring, palette.ink) }
        Column(Modifier.weight(1f).padding(start = 8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = ink, textDecoration = if (task.isDone) TextDecoration.LineThrough else null)
            val due = listOfNotNull(
                task.dueDate?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)),
                task.dueTime?.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale))
            ).joinToString(" · ")
            if (due.isNotEmpty()) Text(due, style = MaterialTheme.typography.bodySmall, color = palette.inkSoft)
        }
        Text(
            priorityLabel(task.priority),
            style = MaterialTheme.typography.labelSmall,
            color = if (task.isDone) palette.cardDoneInk else InkOnPastel,
            modifier = Modifier
                .clip(RoundedCornerShape(9.dp))
                .background(if (task.isDone) palette.border else priorityColor(task.priority))
                .padding(horizontal = 9.dp, vertical = 5.dp)
        )
    }
}
