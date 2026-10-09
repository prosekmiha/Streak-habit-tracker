package com.streak.app.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.streak.app.R
import com.streak.app.domain.model.Priority
import com.streak.app.domain.model.Task
import com.streak.app.ui.components.PillButton
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
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                stringResource(R.string.nav_tasks),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp).semantics { heading() }
            )
        }
        if (state.loaded && state.groups.isEmpty()) {
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
                    MascotView(MascotMood.SLEEPING, Modifier.size(160.dp))
                    Text(stringResource(R.string.tasks_empty), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(vertical = 16.dp))
                    PillButton(stringResource(R.string.add_task), onAddTask)
                }
            }
        }
        state.groups.forEach { (group, tasks) ->
            item(key = group.name) {
                Text(
                    stringResource(group.label()),
                    style = MaterialTheme.typography.titleLarge,
                    color = if (group == TaskGroup.OVERDUE) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 12.dp).semantics { heading() }
                )
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
private fun TaskRow(task: Task, onToggle: () -> Unit, onClick: () -> Unit) {
    val palette = StreakTheme.palette
    val locale = LocalConfiguration.current.locales[0]
    val doneText = stringResource(if (task.isDone) R.string.state_done else R.string.state_not_done)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .background(if (task.isDone) palette.cardDone else priorityColor(task.priority), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        val ink = if (task.isDone) palette.cardDoneInk else InkOnPastel
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .clickable(role = Role.Checkbox, onClick = onToggle)
                .semantics { stateDescription = doneText }
        ) {
            if (task.isDone) {
                Box(Modifier.size(26.dp).background(palette.ink, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.surface, modifier = Modifier.size(17.dp))
                }
            } else {
                Box(Modifier.size(26.dp).border(2.dp, ink, CircleShape))
            }
        }
        Column(Modifier.weight(1f).padding(start = 8.dp)) {
            Text(
                task.title,
                style = MaterialTheme.typography.titleMedium,
                color = ink,
                textDecoration = if (task.isDone) TextDecoration.LineThrough else null
            )
            val due = listOfNotNull(
                task.dueDate?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)),
                task.dueTime?.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale))
            ).joinToString(" · ")
            if (due.isNotEmpty()) Text(due, style = MaterialTheme.typography.bodySmall, color = ink.copy(alpha = 0.8f))
        }
    }
}
