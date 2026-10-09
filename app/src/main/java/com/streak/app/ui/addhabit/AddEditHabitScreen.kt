package com.streak.app.ui.addhabit

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.streak.app.R
import com.streak.app.domain.model.GoalType
import com.streak.app.domain.model.HabitCategory
import com.streak.app.domain.model.ScheduleType
import com.streak.app.domain.model.TimeOfDay
import com.streak.app.ui.components.FieldLabel
import com.streak.app.ui.components.FieldShape
import com.streak.app.ui.components.IconMap
import com.streak.app.ui.components.PillButton
import com.streak.app.ui.components.StreakTextField
import com.streak.app.ui.components.parseHexColor
import com.streak.app.ui.theme.HabitColors
import com.streak.app.ui.theme.InkOnPastel
import com.streak.app.ui.theme.StreakTheme
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle

@Composable
fun AddEditHabitScreen(onClose: () -> Unit, viewModel: AddEditHabitViewModel = hiltViewModel()) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val showErrors by viewModel.showErrors.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
            IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.close)) }
            Text(
                stringResource(if (viewModel.isEdit) R.string.edit_habit else R.string.new_habit_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(start = 4.dp).semantics { heading() }
            )
        }
        if (!form.loaded) return@Column

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            StreakTextField(
                value = form.title,
                onValueChange = { v -> viewModel.update { it.copy(title = v) } },
                label = stringResource(R.string.habit_name_label),
                placeholder = stringResource(R.string.habit_name_placeholder),
                isError = showErrors && form.titleError,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )
            StreakTextField(
                value = form.description,
                onValueChange = { v -> viewModel.update { it.copy(description = v) } },
                label = stringResource(R.string.habit_description_label),
                placeholder = stringResource(R.string.habit_description_placeholder),
                filled = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )

            IntervalSection(form, showErrors, viewModel)
            GoalSection(form, showErrors, viewModel)

            FieldLabel(stringResource(R.string.icon_label))
            IconGrid(selected = form.icon, onSelect = viewModel::selectIcon)

            FieldLabel(stringResource(R.string.color_label))
            ColorRow(selected = form.colorHex, onSelect = viewModel::selectColor)

            FieldLabel(stringResource(R.string.category_label))
            ChoiceChips(
                options = HabitCategory.entries,
                selected = form.category,
                label = { categoryLabel(it) },
                onSelect = { c -> viewModel.update { it.copy(category = c) } }
            )

            FieldLabel(stringResource(R.string.time_of_day_label))
            ChoiceChips(
                options = TimeOfDay.entries,
                selected = form.timeOfDay,
                label = { timeOfDayLabel(it) },
                onSelect = { t -> viewModel.update { it.copy(timeOfDay = t) } }
            )

            RemindersSection(form, viewModel)
            DatesSection(form, showErrors, viewModel)

            if (viewModel.isEdit) {
                Row(Modifier.fillMaxWidth().padding(top = 20.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { viewModel.setArchived(!form.isArchived, onClose) }) {
                        Text(stringResource(if (form.isArchived) R.string.action_unarchive else R.string.action_archive))
                    }
                    TextButton(onClick = { confirmDelete = true }) {
                        Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            Spacer(Modifier.size(16.dp))
        }

        PillButton(
            text = stringResource(R.string.save),
            onClick = { viewModel.save(onClose) },
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_habit_title)) },
            text = { Text(stringResource(R.string.delete_habit_text, form.title)) },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; viewModel.delete(onClose) }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun IntervalSection(form: HabitForm, showErrors: Boolean, viewModel: AddEditHabitViewModel) {
    val palette = StreakTheme.palette
    var expanded by remember { mutableStateOf(false) }
    FieldLabel(stringResource(R.string.interval_label))
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = scheduleLabel(form.scheduleType),
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            shape = FieldShape,
            textStyle = MaterialTheme.typography.bodyLarge,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = palette.surfaceSoft,
                unfocusedContainerColor = palette.surfaceSoft,
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ScheduleType.entries.forEach { type ->
                DropdownMenuItem(
                    text = { Text(scheduleLabel(type)) },
                    onClick = {
                        expanded = false
                        viewModel.update {
                            it.copy(
                                scheduleType = type,
                                timesPerPeriod = it.timesPerPeriod.coerceAtMost(if (type == ScheduleType.TIMES_PER_WEEK) 7 else 31)
                            )
                        }
                    }
                )
            }
        }
    }

    when (form.scheduleType) {
        ScheduleType.WEEKDAYS -> {
            val locale = LocalConfiguration.current.locales[0]
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 10.dp)
            ) {
                form.orderedWeekdays.forEach { day ->
                    val on = day in form.weekdays
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(44.dp)
                            .background(if (on) palette.daySelected else palette.surfaceSoft, CircleShape)
                            .clickable(role = Role.Checkbox) { viewModel.toggleWeekday(day) }
                            .semantics {
                                selected = on
                                contentDescription = day.getDisplayName(TextStyle.FULL, locale)
                            }
                    ) {
                        Text(
                            day.getDisplayName(TextStyle.SHORT, locale).take(3),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (on) palette.daySelectedInk else palette.ink
                        )
                    }
                }
            }
            if (showErrors && form.weekdaysError) ErrorText(stringResource(R.string.error_pick_weekday))
        }
        ScheduleType.TIMES_PER_WEEK, ScheduleType.TIMES_PER_MONTH -> {
            val max = if (form.scheduleType == ScheduleType.TIMES_PER_WEEK) 7 else 31
            Stepper(
                value = form.timesPerPeriod,
                label = stringResource(
                    if (form.scheduleType == ScheduleType.TIMES_PER_WEEK) R.string.times_per_week_value else R.string.times_per_month_value,
                    form.timesPerPeriod
                ),
                onChange = { n -> viewModel.update { it.copy(timesPerPeriod = n.coerceIn(1, max)) } }
            )
        }
        ScheduleType.DAILY -> Unit
    }
}

@Composable
private fun GoalSection(form: HabitForm, showErrors: Boolean, viewModel: AddEditHabitViewModel) {
    FieldLabel(stringResource(R.string.goal_label))
    ChoiceChips(
        options = GoalType.entries,
        selected = form.goalType,
        label = { goalLabel(it) },
        onSelect = { g -> viewModel.update { it.copy(goalType = g) } }
    )
    when (form.goalType) {
        GoalType.QUANTITY -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 4.dp)) {
            StreakTextField(
                value = form.targetText,
                onValueChange = { v -> viewModel.update { it.copy(targetText = v.filter { c -> c.isDigit() || c == ',' || c == '.' }.take(7)) } },
                placeholder = stringResource(R.string.goal_amount_placeholder),
                filled = true,
                isError = showErrors && form.targetError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
            StreakTextField(
                value = form.unit,
                onValueChange = { v -> viewModel.update { it.copy(unit = v.take(20)) } },
                placeholder = stringResource(R.string.goal_unit_placeholder),
                filled = true,
                modifier = Modifier.weight(1.4f)
            )
        }
        GoalType.DURATION -> Stepper(
            value = form.durationMinutes,
            label = stringResource(R.string.duration_minutes_value, form.durationMinutes),
            step = 5,
            onChange = { n -> viewModel.update { it.copy(durationMinutes = n.coerceIn(1, 600)) } }
        )
        GoalType.YES_NO -> Unit
    }
}

@Composable
private fun IconGrid(selected: String, onSelect: (String) -> Unit) {
    // Mreza 4 stolpcev brez lastnega drsenja (je znotraj drsnega obrazca)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        IconMap.all.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { icon ->
                    val isSel = icon.name == selected
                    val shape = RoundedCornerShape(18.dp)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .background(parseHexColor(icon.tileHex), shape)
                            .then(if (isSel) Modifier.border(3.dp, StreakTheme.palette.ink, shape) else Modifier)
                            .clickable(role = Role.RadioButton) { onSelect(icon.name) }
                            .semantics { this.selected = isSel }
                    ) {
                        Icon(icon.vector, contentDescription = icon.name, tint = InkOnPastel, modifier = Modifier.size(26.dp))
                    }
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorRow(selected: String, onSelect: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        HabitColors.forEach { hex ->
            val isSel = hex.equals(selected, ignoreCase = true)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .background(parseHexColor(hex), CircleShape)
                    .then(if (isSel) Modifier.border(3.dp, StreakTheme.palette.ink, CircleShape) else Modifier)
                    .clickable(role = Role.RadioButton) { onSelect(hex) }
                    .semantics { this.selected = isSel; contentDescription = hex }
            ) {
                if (isSel) Icon(Icons.Filled.Check, null, tint = InkOnPastel, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> ChoiceChips(options: List<T>, selected: T, label: @Composable (T) -> String, onSelect: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(label(option), style = MaterialTheme.typography.labelLarge) },
                shape = RoundedCornerShape(14.dp),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = StreakTheme.palette.surfaceSoft,
                    selectedContainerColor = StreakTheme.palette.daySelected,
                    selectedLabelColor = StreakTheme.palette.daySelectedInk
                ),
                border = null,
                modifier = Modifier.heightIn(min = 44.dp)
            )
        }
    }
}

@Composable
private fun Stepper(value: Int, label: String, onChange: (Int) -> Unit, step: Int = 1) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .background(StreakTheme.palette.surfaceSoft, FieldShape)
            .padding(6.dp)
    ) {
        FilledTonalIconButton(onClick = { onChange(value - step) }) { Icon(Icons.Filled.Remove, stringResource(R.string.decrease)) }
        Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
        FilledTonalIconButton(onClick = { onChange(value + step) }) { Icon(Icons.Filled.Add, stringResource(R.string.increase)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RemindersSection(form: HabitForm, viewModel: AddEditHabitViewModel) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val formatter = remember(locale) { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale) }
    var picking by remember { mutableStateOf(false) }

    FieldLabel(stringResource(R.string.reminders_label))
    form.reminders.forEach { time ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .background(StreakTheme.palette.surfaceSoft, FieldShape)
                .padding(start = 16.dp)
        ) {
            Icon(Icons.Outlined.Alarm, null)
            Text(time.format(formatter), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f).padding(start = 12.dp))
            IconButton(onClick = { viewModel.removeReminder(time) }) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.remove_reminder, time.format(formatter)))
            }
        }
    }
    if (form.reminders.size < com.streak.app.notifications.AlarmScheduler.MAX_REMINDERS) {
        TextButton(onClick = { picking = true }) {
            Icon(Icons.Filled.Add, null)
            Text(stringResource(R.string.add_reminder), modifier = Modifier.padding(start = 6.dp), style = MaterialTheme.typography.labelLarge)
        }
    }

    if (picking) {
        val timeState = rememberTimePickerState(initialHour = 8, initialMinute = 0, is24Hour = DateFormat.is24HourFormat(context))
        AlertDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.addReminder(LocalTime.of(timeState.hour, timeState.minute))
                    picking = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { picking = false }) { Text(stringResource(R.string.cancel)) } },
            text = { TimePicker(state = timeState) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatesSection(form: HabitForm, showErrors: Boolean, viewModel: AddEditHabitViewModel) {
    val locale = LocalConfiguration.current.locales[0]
    val formatter = remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale) }
    var picking by remember { mutableStateOf<String?>(null) } // "start" ali "end"

    FieldLabel(stringResource(R.string.dates_label))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        DateBox(
            label = stringResource(R.string.start_date),
            value = form.startDate.format(formatter),
            modifier = Modifier.weight(1f)
        ) { picking = "start" }
        DateBox(
            label = stringResource(R.string.end_date),
            value = form.endDate?.format(formatter) ?: stringResource(R.string.end_date_none),
            modifier = Modifier.weight(1f),
            onClear = if (form.endDate != null) ({ viewModel.update { it.copy(endDate = null) } }) else null
        ) { picking = "end" }
    }
    if (showErrors && form.endDateError) ErrorText(stringResource(R.string.error_end_before_start))

    picking?.let { which ->
        val initial = if (which == "start") form.startDate else form.endDate ?: form.startDate
        val state = rememberDatePickerState(initialSelectedDateMillis = initial.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { picking = null },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        viewModel.update { if (which == "start") it.copy(startDate = date) else it.copy(endDate = date) }
                    }
                    picking = null
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { picking = null }) { Text(stringResource(R.string.cancel)) } }
        ) { DatePicker(state = state) }
    }
}

@Composable
private fun DateBox(label: String, value: String, modifier: Modifier, onClear: (() -> Unit)? = null, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(StreakTheme.palette.surfaceSoft, FieldShape)
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 56.dp)
            .padding(horizontal = 12.dp)
    ) {
        Icon(Icons.Outlined.CalendarMonth, null, modifier = Modifier.size(20.dp))
        Column(Modifier.weight(1f).padding(start = 8.dp, top = 6.dp, bottom = 6.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = StreakTheme.palette.inkSoft)
            Text(value, style = MaterialTheme.typography.bodyMedium)
        }
        if (onClear != null) {
            IconButton(onClick = onClear) { Icon(Icons.Filled.Close, stringResource(R.string.clear), modifier = Modifier.size(18.dp)) }
        }
    }
}

@Composable
private fun ErrorText(text: String) {
    Text(text, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
}

@Composable
private fun scheduleLabel(type: ScheduleType) = stringResource(
    when (type) {
        ScheduleType.DAILY -> R.string.interval_daily
        ScheduleType.WEEKDAYS -> R.string.interval_weekdays
        ScheduleType.TIMES_PER_WEEK -> R.string.interval_times_week
        ScheduleType.TIMES_PER_MONTH -> R.string.interval_times_month
    }
)

@Composable
private fun goalLabel(type: GoalType) = stringResource(
    when (type) {
        GoalType.YES_NO -> R.string.goal_yes_no
        GoalType.QUANTITY -> R.string.goal_quantity
        GoalType.DURATION -> R.string.goal_duration
    }
)

@Composable
fun categoryLabel(c: HabitCategory) = stringResource(
    when (c) {
        HabitCategory.NONE -> R.string.category_none
        HabitCategory.HEALTH -> R.string.category_health
        HabitCategory.FINANCE -> R.string.category_finance
        HabitCategory.LEARNING -> R.string.category_learning
        HabitCategory.HOME -> R.string.category_home
        HabitCategory.MINDFULNESS -> R.string.category_mindfulness
        HabitCategory.WORK -> R.string.category_work
    }
)

@Composable
fun timeOfDayLabel(t: TimeOfDay) = stringResource(
    when (t) {
        TimeOfDay.ANYTIME -> R.string.time_of_day_anytime
        TimeOfDay.MORNING -> R.string.filter_morning
        TimeOfDay.AFTERNOON -> R.string.filter_afternoon
        TimeOfDay.EVENING -> R.string.filter_evening
    }
)
