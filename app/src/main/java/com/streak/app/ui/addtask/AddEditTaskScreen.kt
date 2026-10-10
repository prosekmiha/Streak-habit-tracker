package com.streak.app.ui.addtask

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.streak.app.R
import com.streak.app.domain.model.Priority
import com.streak.app.ui.components.ChoiceChip
import com.streak.app.ui.components.DatePickerSheet
import com.streak.app.ui.components.DatePreset
import com.streak.app.ui.components.FieldLabel
import com.streak.app.ui.components.InfoField
import com.streak.app.ui.components.RoundIconButton
import com.streak.app.ui.components.ScreenTopBar
import com.streak.app.ui.components.SegmentedControl
import com.streak.app.ui.components.StreakButton
import com.streak.app.ui.components.StreakTextField
import com.streak.app.ui.components.TextAction
import com.streak.app.ui.components.TimePickerSheet
import com.streak.app.ui.components.TimePreset
import com.streak.app.ui.components.nextWeekStart
import com.streak.app.ui.components.shortDayName
import com.streak.app.ui.tasks.priorityColor
import com.streak.app.ui.tasks.priorityLabel
import com.streak.app.ui.theme.StreakTheme
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.time.temporal.WeekFields

@Composable
fun AddEditTaskScreen(onClose: () -> Unit, viewModel: AddEditTaskViewModel = hiltViewModel()) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val palette = StreakTheme.palette
    val locale = LocalConfiguration.current.locales[0]
    val firstDay = remember(locale) { WeekFields.of(locale).firstDayOfWeek }
    val today = LocalDate.now()
    var pickDate by remember { mutableStateOf(false) }
    var pickTime by remember { mutableStateOf(false) }
    val dateFormat = remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale) }
    val timeFormat = remember(locale) { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale) }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).imePadding()) {
        ScreenTopBar(
            title = stringResource(if (viewModel.isEdit) R.string.edit_task else R.string.new_task),
            navIcon = Icons.Rounded.Close,
            navDescription = stringResource(R.string.close),
            onNav = onClose
        )
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            StreakTextField(
                value = form.title,
                onValueChange = { t -> viewModel.update { it.copy(title = t) } },
                label = stringResource(R.string.title_label),
                placeholder = stringResource(R.string.task_title_placeholder),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )

            FieldLabel(stringResource(R.string.due_label))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                InfoField(
                    Icons.Rounded.CalendarMonth,
                    stringResource(R.string.due_date_label),
                    when (val d = form.dueDate) {
                        null -> stringResource(R.string.no_date)
                        today -> stringResource(R.string.today)
                        today.plusDays(1) -> stringResource(R.string.tomorrow)
                        else -> d.format(dateFormat)
                    },
                    { pickDate = true },
                    Modifier.weight(1f)
                )
                InfoField(
                    Icons.Rounded.Schedule,
                    stringResource(R.string.due_time),
                    if (form.hasTime) LocalTime.of(form.dueHour, form.dueMinute).format(timeFormat) else stringResource(R.string.no_time),
                    { pickTime = true },
                    Modifier.weight(1f),
                    trailing = if (form.hasTime) ({
                        RoundIconButton(Icons.Rounded.Close, stringResource(R.string.clear), { viewModel.update { it.copy(hasTime = false) } }, container = Color.Transparent)
                    }) else null
                )
            }

            FieldLabel(stringResource(R.string.priority_label))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(Priority.LOW, Priority.NORMAL, Priority.URGENT).forEach { p ->
                    ChoiceChip(priorityLabel(p), form.priority == p, { viewModel.update { it.copy(priority = p) } }, Modifier.weight(1f), color = priorityColor(p))
                }
            }

            FieldLabel(stringResource(R.string.repeat_label))
            SegmentedControl(
                TaskRepeat.entries,
                form.repeat,
                {
                    stringResource(
                        when (it) {
                            TaskRepeat.NONE -> R.string.repeat_none
                            TaskRepeat.DAILY -> R.string.repeat_daily
                            TaskRepeat.WEEKLY -> R.string.repeat_weekly
                        }
                    )
                },
                viewModel::setRepeat
            )
            if (form.repeat == TaskRepeat.WEEKLY) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
                    (0L..6L).map { firstDay.plus(it) }.forEach { day ->
                        val on = day in form.recurringDays
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (on) palette.daySelected else palette.surfaceSoft)
                                .toggleable(value = on, role = Role.Checkbox) { viewModel.toggleDay(day) }
                                .semantics { contentDescription = day.getDisplayName(TextStyle.FULL, locale) }
                        ) { Text(shortDayName(day, locale).take(3), style = MaterialTheme.typography.labelMedium, color = if (on) palette.daySelectedInk else palette.ink) }
                    }
                }
            }

            if (viewModel.isEdit) {
                TextAction(stringResource(R.string.delete), { viewModel.delete(onClose) }, Modifier.padding(top = 20.dp), color = palette.danger)
            }
        }
        Box(Modifier.navigationBarsPadding().padding(horizontal = 20.dp, vertical = 14.dp)) {
            StreakButton(stringResource(R.string.save), { viewModel.save(onClose) }, enabled = form.isValid, height = 58.dp)
        }
    }

    if (pickDate) {
        DatePickerSheet(
            title = stringResource(R.string.due_date_label),
            initial = form.dueDate ?: today,
            today = today,
            firstDayOfWeek = firstDay,
            presets = listOf(
                DatePreset(today, stringResource(R.string.today)),
                DatePreset(today.plusDays(1), stringResource(R.string.tomorrow)),
                DatePreset(nextWeekStart(today, firstDay), firstDay.getDisplayName(TextStyle.FULL_STANDALONE, locale).replaceFirstChar { it.titlecase(locale) })
            ),
            extraAction = stringResource(R.string.no_date) to { viewModel.update { it.copy(dueDate = null) } },
            onDismiss = { pickDate = false },
            onConfirm = { d -> viewModel.update { it.copy(dueDate = d) }; pickDate = false }
        )
    }
    if (pickTime) {
        TimePickerSheet(
            title = stringResource(R.string.due_time),
            initial = LocalTime.of(form.dueHour, form.dueMinute),
            presets = listOf(
                TimePreset(LocalTime.of(9, 0), stringResource(R.string.filter_morning)),
                TimePreset(LocalTime.of(14, 0), stringResource(R.string.filter_afternoon)),
                TimePreset(LocalTime.of(19, 0), stringResource(R.string.filter_evening))
            ),
            confirmText = stringResource(R.string.pick),
            onDismiss = { pickTime = false },
            onConfirm = { t -> viewModel.update { it.copy(hasTime = true, dueHour = t.hour, dueMinute = t.minute) }; pickTime = false }
        )
    }
}
