package com.streak.app.ui.addtask

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.streak.app.R
import com.streak.app.domain.model.TaskReminder
import com.streak.app.ui.addhabit.ReminderPermissionHint
import com.streak.app.ui.components.ChoiceChips
import com.streak.app.ui.components.DashedAddButton
import com.streak.app.ui.components.FieldLabel
import com.streak.app.ui.components.InfoField
import com.streak.app.ui.components.RoundIconButton
import com.streak.app.ui.components.SegmentedControl
import com.streak.app.ui.components.Stepper
import com.streak.app.ui.components.StreakButton
import com.streak.app.ui.components.StreakSheet
import com.streak.app.ui.components.TimePickerSheet
import com.streak.app.ui.components.TimePreset
import com.streak.app.ui.theme.InkOnPastel
import com.streak.app.ui.theme.Lavender
import com.streak.app.ui.theme.StreakTheme
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private enum class ReminderKind { BEFORE, DAY, REPEAT }

private enum class LeadUnit(val minutes: Int) { MINUTES(1), HOURS(60), DAYS(24 * 60) }

// Opomniki naloge: cipi obstojecih in list za dodajanje (pred rokom, na dan ob uri, veckrat)
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TaskRemindersSection(form: TaskForm, viewModel: AddEditTaskViewModel) {
    var adding by remember { mutableStateOf(false) }
    val timeFormat = rememberTimeFormat()

    FieldLabel(stringResource(R.string.reminders_label))
    if (form.dueDate == null) {
        Hint(stringResource(R.string.task_reminders_need_date))
        return
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        form.reminders.forEach { reminder ->
            val label = taskReminderLabel(reminder, timeFormat)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Lavender.copy(alpha = .55f))
                    .padding(start = 16.dp)
            ) {
                Text(label, style = MaterialTheme.typography.titleSmall, color = InkOnPastel)
                RoundIconButton(
                    Icons.Rounded.Close,
                    stringResource(R.string.remove_task_reminder, label),
                    { viewModel.removeReminder(reminder) },
                    container = Color.Transparent,
                    tint = InkOnPastel
                )
            }
        }
        if (form.reminders.size < TaskReminder.MAX) {
            DashedAddButton(stringResource(R.string.add), { adding = true })
        }
    }
    if (!form.hasTime && form.reminders.any { it is TaskReminder.BeforeDue }) {
        Hint(stringResource(R.string.task_reminders_need_time))
    }
    if (form.reminders.isNotEmpty()) ReminderPermissionHint()

    if (adding) {
        AddTaskReminderSheet(
            form = form,
            onDismiss = { adding = false },
            onAdd = { viewModel.addReminder(it); adding = false }
        )
    }
}

@Composable
private fun AddTaskReminderSheet(form: TaskForm, onDismiss: () -> Unit, onAdd: (TaskReminder) -> Unit) {
    val timeFormat = rememberTimeFormat()
    var kind by remember { mutableStateOf(if (form.hasTime) ReminderKind.BEFORE else ReminderKind.DAY) }
    // Pred rokom
    var leadAmount by remember { mutableIntStateOf(0) }
    var leadUnit by remember { mutableStateOf(LeadUnit.MINUTES) }
    // Na dan ob uri
    var daysBefore by remember { mutableIntStateOf(0) }
    var dayTime by remember { mutableStateOf(form.morningTime) }
    // Veckrat na dan
    var every by remember { mutableIntStateOf(60) }
    var from by remember { mutableStateOf(LocalTime.of(9, 0)) }
    var until by remember { mutableStateOf(LocalTime.of(21, 0)) }
    var picking by remember { mutableStateOf<String?>(null) }

    StreakSheet(onDismiss = onDismiss, title = stringResource(R.string.add_reminder), showClose = true) {
        SegmentedControl(
            ReminderKind.entries,
            kind,
            {
                stringResource(
                    when (it) {
                        ReminderKind.BEFORE -> R.string.task_reminder_kind_before
                        ReminderKind.DAY -> R.string.task_reminder_kind_day
                        ReminderKind.REPEAT -> R.string.task_reminder_kind_repeat
                    }
                )
            },
            { kind = it }
        )

        val reminder: TaskReminder = when (kind) {
            ReminderKind.BEFORE -> TaskReminder.BeforeDue(leadAmount * leadUnit.minutes)
            ReminderKind.DAY -> TaskReminder.DayAt(daysBefore, dayTime)
            ReminderKind.REPEAT -> TaskReminder.Repeat(every, from, until)
        }

        when (kind) {
            ReminderKind.BEFORE -> {
                if (!form.hasTime) Hint(stringResource(R.string.task_reminders_need_time))
                FieldLabel(stringResource(R.string.task_reminder_quick))
                val presets = listOf(0, 15, 30, 60, 120, 24 * 60)
                ChoiceChips(
                    presets,
                    presets.firstOrNull { it == leadAmount * leadUnit.minutes && it % leadUnit.minutes == 0 } ?: -1,
                    { if (it == 0) stringResource(R.string.task_reminder_at_due) else durationText(it) },
                    { m ->
                        leadUnit = LeadUnit.entries.last { m % it.minutes == 0 }
                        leadAmount = m / leadUnit.minutes
                    }
                )
                FieldLabel(stringResource(R.string.task_reminder_custom))
                Stepper(
                    leadAmount.toString(),
                    { leadAmount = (leadAmount - 1).coerceAtLeast(0) },
                    { leadAmount = (leadAmount + 1).coerceAtMost(maxLead(leadUnit)) }
                )
                SegmentedControl(
                    LeadUnit.entries,
                    leadUnit,
                    {
                        stringResource(
                            when (it) {
                                LeadUnit.MINUTES -> R.string.task_reminder_unit_minutes
                                LeadUnit.HOURS -> R.string.task_reminder_unit_hours
                                LeadUnit.DAYS -> R.string.task_reminder_unit_days
                            }
                        )
                    },
                    { leadUnit = it; leadAmount = leadAmount.coerceAtMost(maxLead(it)) },
                    Modifier.padding(top = 8.dp)
                )
            }
            ReminderKind.DAY -> {
                FieldLabel(stringResource(R.string.task_reminder_days_before))
                Stepper(
                    daysText(daysBefore),
                    { daysBefore = (daysBefore - 1).coerceAtLeast(0) },
                    { daysBefore = (daysBefore + 1).coerceAtMost(MAX_DAYS) }
                )
                FieldLabel(stringResource(R.string.notif_settings_time))
                InfoField(Icons.Rounded.Schedule, stringResource(R.string.notif_settings_time), dayTime.format(timeFormat), { picking = "day" })
            }
            ReminderKind.REPEAT -> {
                FieldLabel(stringResource(R.string.task_reminder_interval))
                val presets = listOf(15, 30, 60, 120, 180)
                ChoiceChips(presets, every, { durationText(it) }, { every = it })
                Stepper(
                    durationText(every),
                    { every = (every - intervalStep(every - 1)).coerceAtLeast(TaskReminder.MIN_INTERVAL) },
                    { every = (every + intervalStep(every)).coerceAtMost(MAX_INTERVAL) },
                    Modifier.padding(top = 10.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 10.dp)) {
                    InfoField(Icons.Rounded.Schedule, stringResource(R.string.notif_settings_from), from.format(timeFormat), { picking = "from" }, Modifier.weight(1f))
                    InfoField(Icons.Rounded.Schedule, stringResource(R.string.notif_settings_to), until.format(timeFormat), { picking = "until" }, Modifier.weight(1f))
                }
                Hint(stringResource(R.string.task_reminder_until_done))
            }
        }

        val valid = when (kind) {
            ReminderKind.BEFORE -> form.hasTime
            ReminderKind.DAY -> true
            ReminderKind.REPEAT -> until.isAfter(from)
        } && reminder !in form.reminders
        StreakButton(stringResource(R.string.add), { onAdd(reminder) }, Modifier.padding(top = 20.dp), enabled = valid)
    }

    picking?.let { which ->
        TimePickerSheet(
            title = stringResource(R.string.notif_settings_time),
            initial = when (which) { "from" -> from; "until" -> until; else -> dayTime },
            presets = listOf(
                TimePreset(form.morningTime, stringResource(R.string.filter_morning)),
                TimePreset(form.eveningTime, stringResource(R.string.filter_evening))
            ),
            confirmText = stringResource(R.string.pick),
            onDismiss = { picking = null },
            onConfirm = { t ->
                when (which) { "from" -> from = t; "until" -> until = t; else -> dayTime = t }
                picking = null
            }
        )
    }
}

// "1 h prej", "Dan prej ob 20:00", "Vsakih 30 min, 9:00–21:00"
@Composable
internal fun taskReminderLabel(r: TaskReminder, timeFormat: DateTimeFormatter): String = when (r) {
    is TaskReminder.BeforeDue ->
        if (r.minutes == 0) stringResource(R.string.task_reminder_at_due)
        else stringResource(R.string.task_reminder_before, durationText(r.minutes))
    is TaskReminder.DayAt -> when (r.daysBefore) {
        0 -> stringResource(R.string.task_reminder_same_day_at, r.time.format(timeFormat))
        1 -> stringResource(R.string.task_reminder_day_before_at, r.time.format(timeFormat))
        else -> stringResource(R.string.task_reminder_days_before_at, daysText(r.daysBefore), r.time.format(timeFormat))
    }
    is TaskReminder.Repeat ->
        stringResource(R.string.task_reminder_every, durationText(r.everyMinutes), r.from.format(timeFormat), r.until.format(timeFormat))
}

// Najvecja enota, ki deli trajanje: "2 dni", "3 h", "90 min"
@Composable
private fun durationText(minutes: Int): String = when {
    minutes > 0 && minutes % (24 * 60) == 0 -> daysText(minutes / (24 * 60))
    minutes > 0 && minutes % 60 == 0 -> stringResource(R.string.task_reminder_hours, minutes / 60)
    else -> stringResource(R.string.task_reminder_minutes, minutes)
}

@Composable
private fun daysText(days: Int): String = pluralStringResource(R.plurals.task_reminder_days, days, days)

@Composable
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = StreakTheme.palette.inkSoft, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun rememberTimeFormat(): DateTimeFormatter {
    val locale = LocalConfiguration.current.locales[0]
    return remember(locale) { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale) }
}

// Pod uro po 5 min, nad uro po 15 min
private fun intervalStep(minutes: Int) = if (minutes < 60) 5 else 15

private fun maxLead(unit: LeadUnit) = when (unit) {
    LeadUnit.MINUTES -> 59 * 4
    LeadUnit.HOURS -> 72
    LeadUnit.DAYS -> MAX_DAYS
}

private const val MAX_DAYS = 30
private const val MAX_INTERVAL = 12 * 60
