package com.streak.app.ui.addtask

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.streak.app.R
import com.streak.app.domain.model.Priority
import com.streak.app.ui.theme.Accent
import com.streak.app.ui.theme.Success
import com.streak.app.ui.theme.Warning
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun AddEditTaskScreen(
    onClose: () -> Unit,
    viewModel: AddEditTaskViewModel = hiltViewModel()
) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val sl = Locale.forLanguageTag("sl")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                stringRes(if (viewModel.isEdit) R.string.edit_task else R.string.add_task),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = "Zapri", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        }

        FieldLabel(stringRes(R.string.title_label))
        OutlinedTextField(
            value = form.title,
            onValueChange = { t -> viewModel.update { it.copy(title = t) } },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // Prioriteta
        FieldLabel(stringRes(R.string.priority_label))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PriorityChip(stringRes(R.string.priority_low), Success, form.priority == Priority.LOW) { viewModel.update { it.copy(priority = Priority.LOW) } }
            PriorityChip(stringRes(R.string.priority_normal), Accent, form.priority == Priority.NORMAL) { viewModel.update { it.copy(priority = Priority.NORMAL) } }
            PriorityChip(stringRes(R.string.priority_urgent), Warning, form.priority == Priority.URGENT) { viewModel.update { it.copy(priority = Priority.URGENT) } }
        }

        // Ura
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(stringRes(R.string.due_time), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Switch(checked = form.hasTime, onCheckedChange = { on -> viewModel.update { it.copy(hasTime = on) } })
        }
        if (form.hasTime) {
            val timeState = rememberTimePickerState(initialHour = form.dueHour, initialMinute = form.dueMinute, is24Hour = true)
            androidx.compose.runtime.LaunchedEffect(timeState.hour, timeState.minute) {
                viewModel.update { it.copy(dueHour = timeState.hour, dueMinute = timeState.minute) }
            }
            Box(modifier = Modifier.fillMaxWidth().padding(top = 10.dp), contentAlignment = Alignment.Center) {
                TimePicker(state = timeState)
            }
        }

        // Ponavljanje
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(stringRes(R.string.recurring_label), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Switch(checked = form.isRecurring, onCheckedChange = { on -> viewModel.update { it.copy(isRecurring = on) } })
        }
        if (form.isRecurring) {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY).forEach { day ->
                    val sel = form.recurringDays.contains(day)
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(if (sel) Accent.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), CircleShape)
                            .clickable { viewModel.toggleDay(day) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            day.getDisplayName(TextStyle.NARROW, sl).uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            textAlign = TextAlign.Center,
                            color = if (sel) Accent else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Button(
            onClick = { viewModel.save(onClose) },
            enabled = form.isValid,
            modifier = Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 28.dp)
        ) { Text(stringRes(R.string.save)) }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text = text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 18.dp, bottom = 8.dp))
}

@Composable
private fun PriorityChip(label: String, color: androidx.compose.ui.graphics.Color, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(if (selected) color.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(label, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, color = if (selected) color else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable private fun stringRes(id: Int) = androidx.compose.ui.res.stringResource(id)
