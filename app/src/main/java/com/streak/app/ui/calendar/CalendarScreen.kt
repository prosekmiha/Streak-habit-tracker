package com.streak.app.ui.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.streak.app.domain.model.HabitWithStreak
import com.streak.app.domain.model.LogStatus
import com.streak.app.domain.model.Priority
import com.streak.app.domain.model.Task
import com.streak.app.ui.components.parseHexColor
import com.streak.app.ui.components.IconMap
import com.streak.app.ui.theme.Accent
import com.streak.app.ui.theme.Success
import com.streak.app.ui.theme.Warning
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarScreen(viewModel: CalendarViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sl = Locale.forLanguageTag("sl")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
    ) {
        // Glava z mesecem
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { viewModel.prevMonth() }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Prejšnji", tint = MaterialTheme.colorScheme.onSurface)
            }
            Text(
                text = "${state.month.month.getDisplayName(TextStyle.FULL, sl).replaceFirstChar { it.uppercase() }} ${state.month.year}",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(onClick = { viewModel.nextMonth() }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Naslednji", tint = MaterialTheme.colorScheme.onSurface)
            }
        }

        // Glave dni v tednu (pon..ned)
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY).forEach { d ->
                Text(
                    text = d.getDisplayName(TextStyle.SHORT, sl).replaceFirstChar { it.uppercase() },
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Mreza dni
        val first = state.month.atDay(1)
        val leading = (first.dayOfWeek.value + 6) % 7
        val daysInMonth = state.month.lengthOfMonth()
        val today = LocalDate.now()
        val allCells = List(leading) { null } + (1..daysInMonth).map { it }
        val weeks = allCells.chunked(7).map { week ->
            if (week.size < 7) week + List(7 - week.size) { null } else week
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            weeks.forEach { week ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    week.forEach { dayNum ->
                        Box(modifier = Modifier.weight(1f)) {
                            if (dayNum != null) {
                                val date = state.month.atDay(dayNum)
                                DayCell(
                                    day = dayNum,
                                    completion = state.completionByDate[date] ?: 0f,
                                    isToday = date == today,
                                    isSelected = date == state.selectedDate,
                                    onClick = { viewModel.selectDate(date) }
                                )
                            }
                        }
                    }
                }
            }
        }

        Text(
            text = "Barva označuje delež opravljenih navad",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
        )

        // Panel za izbran dan
        state.selectedDate?.let { date ->
            val dow = date.dayOfWeek.getDisplayName(TextStyle.FULL, sl).replaceFirstChar { it.uppercase() }
            val dateStr = date.format(DateTimeFormatter.ofPattern("d. MMMM", sl))

            Text(
                text = "$dow, $dateStr",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp, start = 4.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(22.dp))
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Text(
                        text = "Navade",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                    )
                    if (state.habitsForDate.isEmpty()) {
                        Text(
                            text = "Ni aktivnih navad",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        state.habitsForDate.forEachIndexed { i, habit ->
                            if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            HabitSummaryRow(
                                item = habit,
                                date = date,
                                onToggle = {
                                    viewModel.toggleHabitOnDate(
                                        habit.habit.id,
                                        date,
                                        habit.statusOn(date) == LogStatus.DONE
                                    )
                                }
                            )
                        }
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    Text(
                        text = "Naloge",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    if (state.tasksForDate.isEmpty()) {
                        Text(
                            text = "Ni nalog za ta dan",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        state.tasksForDate.forEachIndexed { i, task ->
                            if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            TaskSummaryRow(task = task)
                        }
                    }
                }
            }
        }

        Box(modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun DayCell(day: Int, completion: Float, isToday: Boolean, isSelected: Boolean, onClick: () -> Unit) {
    val fill = when {
        isSelected -> Accent.copy(alpha = 0.75f)
        completion > 0f -> Accent.copy(alpha = 0.18f + 0.6f * completion)
        else -> MaterialTheme.colorScheme.surface
    }
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(3.dp)
            .background(fill, RoundedCornerShape(10.dp))
            .then(
                if (isToday && !isSelected)
                    Modifier.border(1.5.dp, Accent, RoundedCornerShape(10.dp))
                else Modifier
            )
            .clickable(
                interactionSource = MutableInteractionSource(),
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
            color = when {
                isSelected -> Color.White
                isToday -> Accent
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

@Composable
private fun HabitSummaryRow(item: HabitWithStreak, date: LocalDate, onToggle: () -> Unit) {
    val color = parseHexColor(item.habit.colorHex)
    val status = item.statusOn(date)
    val isFuture = date.isAfter(LocalDate.now())
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isFuture) Modifier
                else Modifier.clickable(
                    interactionSource = MutableInteractionSource(),
                    indication = null,
                    onClick = onToggle
                )
            )
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(color.copy(alpha = 0.14f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = IconMap.resolve(item.habit.icon),
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(17.dp)
            )
        }
        Text(
            text = item.habit.title,
            style = MaterialTheme.typography.bodyMedium,
            color = if (status == LogStatus.DONE) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            textDecoration = if (status == LogStatus.DONE) TextDecoration.LineThrough else null,
            modifier = Modifier.weight(1f)
        )
        when (status) {
            LogStatus.DONE -> Icon(Icons.Filled.Check, contentDescription = null, tint = Success, modifier = Modifier.size(16.dp))
            LogStatus.SKIPPED -> Text("Preskočeno", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            LogStatus.MISSED -> Text("Zamujeno", style = MaterialTheme.typography.labelSmall, color = Warning)
            null -> {}
        }
    }
}

@Composable
private fun TaskSummaryRow(task: Task) {
    val dotColor = when (task.priority) {
        Priority.URGENT -> Warning
        Priority.NORMAL -> Accent
        Priority.LOW -> Success
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(if (task.isDone) Success else dotColor, RoundedCornerShape(50))
        )
        Text(
            text = task.title,
            style = MaterialTheme.typography.bodyMedium,
            color = if (task.isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
            modifier = Modifier.weight(1f)
        )
        if (task.isDone) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = Success, modifier = Modifier.size(16.dp))
        }
    }
}
