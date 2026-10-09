package com.streak.app.ui.addhabit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.streak.app.R
import com.streak.app.domain.model.ScheduleType
import com.streak.app.ui.components.ColorPicker
import com.streak.app.ui.components.IconPicker
import com.streak.app.ui.components.parseHexColor
import com.streak.app.ui.theme.HabitColors

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun AddEditHabitScreen(
    onClose: () -> Unit,
    viewModel: AddEditHabitViewModel = hiltViewModel()
) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val accent = parseHexColor(form.colorHex)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
    ) {
        // Glava
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                stringRes(if (viewModel.isEdit) R.string.edit_habit else R.string.add_habit),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = "Zapri", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        }

        // Naslov
        FieldLabel(stringRes(R.string.title_label))
        OutlinedTextField(
            value = form.title,
            onValueChange = { t -> viewModel.update { it.copy(title = t) } },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // Ikona
        FieldLabel(stringRes(R.string.icon_label))
        IconPicker(selected = form.icon, accent = accent, onSelect = { name -> viewModel.update { it.copy(icon = name) } })

        // Barva
        FieldLabel(stringRes(R.string.color_label))
        ColorPicker(colors = HabitColors, selected = form.colorHex, onSelect = { hex -> viewModel.update { it.copy(colorHex = hex) } })

        // Frekvenca
        FieldLabel(stringRes(R.string.frequency_label))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FreqChip(stringRes(R.string.freq_daily), form.scheduleType == ScheduleType.DAILY) { viewModel.update { it.copy(scheduleType = ScheduleType.DAILY) } }
            FreqChip(stringRes(R.string.freq_weekly), form.scheduleType == ScheduleType.TIMES_PER_WEEK) { viewModel.update { it.copy(scheduleType = ScheduleType.TIMES_PER_WEEK, timesPerPeriod = it.timesPerPeriod.coerceAtMost(7)) } }
            FreqChip(stringRes(R.string.freq_monthly), form.scheduleType == ScheduleType.TIMES_PER_MONTH) { viewModel.update { it.copy(scheduleType = ScheduleType.TIMES_PER_MONTH) } }
        }

        // Cilj na obdobje (le ce ni dnevno)
        if (form.scheduleType != ScheduleType.DAILY) {
            val monthly = form.scheduleType == ScheduleType.TIMES_PER_MONTH
            val label = stringRes(if (monthly) R.string.target_label_month else R.string.target_label)
            val options = if (monthly) AddEditHabitViewModel.QuickMonthlyTargets else (1..7).toList()
            FieldLabel("$label: ${form.timesPerPeriod}x")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                options.forEach { n ->
                    Box(
                        modifier = Modifier
                            .background(
                                if (form.timesPerPeriod == n) accent.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { viewModel.update { it.copy(timesPerPeriod = n) } }
                            .padding(horizontal = 14.dp, vertical = 9.dp)
                    ) { Text("$n", color = if (form.timesPerPeriod == n) accent else MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        }

        // Opomnik
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(stringRes(R.string.reminder_label), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Switch(checked = form.reminderEnabled, onCheckedChange = { on -> viewModel.update { it.copy(reminderEnabled = on) } })
        }

        if (form.reminderEnabled) {
            val timeState = rememberTimePickerState(initialHour = form.reminderHour, initialMinute = form.reminderMinute, is24Hour = true)
            // Sinhroniziraj izbiro ure nazaj v form
            androidx.compose.runtime.LaunchedEffect(timeState.hour, timeState.minute) {
                viewModel.update { it.copy(reminderHour = timeState.hour, reminderMinute = timeState.minute) }
            }
            Box(modifier = Modifier.fillMaxWidth().padding(top = 10.dp), contentAlignment = Alignment.Center) {
                TimePicker(state = timeState)
            }
        }

        Button(
            onClick = { viewModel.save(onClose) },
            enabled = form.isValid,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 28.dp)
        ) { Text(stringRes(R.string.save)) }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 18.dp, bottom = 8.dp)
    )
}

@Composable
private fun FreqChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(label, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable private fun stringRes(id: Int) = androidx.compose.ui.res.stringResource(id)
