package com.streak.app.ui.addhabit

import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.core.net.toUri
import androidx.core.app.NotificationManagerCompat
import android.provider.Settings
import android.os.Build
import android.content.Intent
import android.app.AlarmManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.streak.app.R
import com.streak.app.domain.model.GoalType
import com.streak.app.domain.model.HabitCategory
import com.streak.app.domain.model.ScheduleType
import com.streak.app.domain.model.TimeOfDay
import com.streak.app.ui.components.ButtonVariant
import com.streak.app.ui.components.ChoiceChip
import com.streak.app.ui.components.ChoiceChips
import com.streak.app.ui.components.DashedAddButton
import com.streak.app.ui.components.DatePickerSheet
import com.streak.app.ui.components.DatePreset
import com.streak.app.ui.components.DialogAction
import com.streak.app.ui.components.FieldLabel
import com.streak.app.ui.components.IconMap
import com.streak.app.ui.components.InfoField
import com.streak.app.ui.components.OptionSheet
import com.streak.app.ui.components.RoundIconButton
import com.streak.app.ui.components.ScreenTopBar
import com.streak.app.ui.components.SegmentedControl
import com.streak.app.ui.components.SelectField
import com.streak.app.ui.components.Stepper
import com.streak.app.ui.components.StreakButton
import com.streak.app.ui.components.StreakDialog
import com.streak.app.ui.components.StreakTextField
import com.streak.app.ui.components.TextAction
import com.streak.app.ui.components.TimePickerSheet
import com.streak.app.ui.components.TimePreset
import com.streak.app.ui.components.nextWeekStart
import com.streak.app.ui.components.parseHexColor
import com.streak.app.ui.components.shortDayName
import com.streak.app.ui.mascot.MascotMood
import com.streak.app.ui.theme.HabitColors
import com.streak.app.ui.theme.InkOnPastel
import com.streak.app.ui.theme.Lavender
import com.streak.app.ui.theme.StreakTheme
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle

@Composable
fun AddEditHabitScreen(onClose: () -> Unit, viewModel: AddEditHabitViewModel = hiltViewModel()) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val showErrors by viewModel.showErrors.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }
    val palette = StreakTheme.palette

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).imePadding()) {
        Column(Modifier.fillMaxSize()) {
            ScreenTopBar(
                title = stringResource(if (viewModel.isEdit) R.string.edit_habit else R.string.new_habit_title),
                navIcon = Icons.Rounded.Close,
                navDescription = stringResource(R.string.close),
                onNav = onClose
            )
            if (!form.loaded) return@Column

            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 110.dp)
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

                FieldLabel(stringResource(R.string.card_color_label))
                ColorRow(selected = form.colorHex, onSelect = viewModel::selectColor)

                FieldLabel(stringResource(R.string.category_label))
                ChoiceChips(HabitCategory.entries, form.category, { categoryLabel(it) }, { c -> viewModel.update { it.copy(category = c) } })

                FieldLabel(stringResource(R.string.time_of_day_label))
                SegmentedControl(TimeOfDay.entries, form.timeOfDay, { timeOfDayLabel(it) }, { t -> viewModel.update { it.copy(timeOfDay = t) } })

                RemindersSection(form, viewModel)
                DatesSection(form, showErrors, viewModel)

                if (viewModel.isEdit) {
                    Row(Modifier.fillMaxWidth().padding(top = 24.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextAction(
                            stringResource(if (form.isArchived) R.string.action_unarchive else R.string.action_archive),
                            { viewModel.setArchived(!form.isArchived, onClose) },
                            color = palette.ink
                        )
                        TextAction(stringResource(R.string.delete), { confirmDelete = true }, color = palette.danger)
                    }
                }
            }
        }

        // Pripet gumb "Shrani" z mehkim prehodom
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(0f to Color.Transparent, 0.3f to MaterialTheme.colorScheme.background))
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 16.dp)
        ) {
            StreakButton(stringResource(R.string.save), { viewModel.save(onClose) }, height = 58.dp)
        }
    }

    if (confirmDelete) {
        StreakDialog(
            onDismiss = { confirmDelete = false },
            title = stringResource(R.string.delete_habit_title_named, form.title),
            text = stringResource(R.string.delete_habit_text_short),
            mascot = MascotMood.SAD
        ) {
            if (!form.isArchived) DialogAction(stringResource(R.string.archive_instead), ButtonVariant.Dark) { confirmDelete = false; viewModel.setArchived(true, onClose) }
            DialogAction(stringResource(R.string.delete_forever), ButtonVariant.Danger) { confirmDelete = false; viewModel.delete(onClose) }
            DialogAction(stringResource(R.string.cancel), ButtonVariant.Secondary) { confirmDelete = false }
        }
    }
}

@Composable
private fun IntervalSection(form: HabitForm, showErrors: Boolean, viewModel: AddEditHabitViewModel) {
    val palette = StreakTheme.palette
    val locale = LocalConfiguration.current.locales[0]
    var picking by remember { mutableStateOf(false) }

    FieldLabel(stringResource(R.string.interval_label))
    SelectField(scheduleLabel(form.scheduleType), { picking = true })

    when (form.scheduleType) {
        ScheduleType.WEEKDAYS -> {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
                form.orderedWeekdays.forEach { day ->
                    val on = day in form.weekdays
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (on) palette.daySelected else palette.surfaceSoft)
                            .toggleable(value = on, role = Role.Checkbox) { viewModel.toggleWeekday(day) }
                            .semantics { contentDescription = day.getDisplayName(TextStyle.FULL, locale) }
                    ) {
                        Text(shortDayName(day, locale).take(3), style = MaterialTheme.typography.labelMedium, color = if (on) palette.daySelectedInk else palette.ink)
                    }
                }
            }
            if (showErrors && form.weekdaysError) ErrorText(stringResource(R.string.error_pick_weekday))
        }
        ScheduleType.TIMES_PER_WEEK, ScheduleType.TIMES_PER_MONTH -> {
            val weekly = form.scheduleType == ScheduleType.TIMES_PER_WEEK
            val max = if (weekly) 7 else 31
            Stepper(
                value = stringResource(if (weekly) R.string.times_per_week_value else R.string.times_per_month_value, form.timesPerPeriod),
                onMinus = { viewModel.update { it.copy(timesPerPeriod = (it.timesPerPeriod - 1).coerceIn(1, max)) } },
                onPlus = { viewModel.update { it.copy(timesPerPeriod = (it.timesPerPeriod + 1).coerceIn(1, max)) } },
                modifier = Modifier.padding(top = 10.dp)
            )
        }
        ScheduleType.DAILY -> Unit
    }

    if (picking) {
        OptionSheet(
            title = stringResource(R.string.interval_label),
            options = ScheduleType.entries,
            selected = form.scheduleType,
            label = { scheduleLabel(it) },
            description = { scheduleDescription(it) },
            onSelect = { type ->
                viewModel.update {
                    it.copy(scheduleType = type, timesPerPeriod = it.timesPerPeriod.coerceAtMost(if (type == ScheduleType.TIMES_PER_WEEK) 7 else 31))
                }
            },
            onDismiss = { picking = false }
        )
    }
}

@Composable
private fun GoalSection(form: HabitForm, showErrors: Boolean, viewModel: AddEditHabitViewModel) {
    FieldLabel(stringResource(R.string.goal_label))
    SegmentedControl(GoalType.entries, form.goalType, { goalLabel(it) }, { g -> viewModel.update { it.copy(goalType = g) } })
    when (form.goalType) {
        GoalType.QUANTITY -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 10.dp)) {
            AmountStepper(
                value = form.targetText,
                onValueChange = { v -> viewModel.update { it.copy(targetText = v.filter { c -> c.isDigit() || c == ',' || c == '.' }.take(6)) } },
                onStep = { delta -> viewModel.update { f -> f.copy(targetText = ((f.target ?: 0.0) + delta).coerceAtLeast(1.0).toLong().toString()) } },
                isError = showErrors && form.targetError,
                modifier = Modifier.weight(1.3f)
            )
            StreakTextField(
                value = form.unit,
                onValueChange = { v -> viewModel.update { it.copy(unit = v.take(20)) } },
                placeholder = stringResource(R.string.goal_unit_placeholder_short),
                filled = true,
                modifier = Modifier.weight(1f)
            )
        }
        GoalType.DURATION -> {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 10.dp)) {
                listOf(5, 10, 20, 30).forEach { m ->
                    ChoiceChip(stringResource(R.string.duration_minutes_value, m), form.durationMinutes == m, { viewModel.update { it.copy(durationMinutes = m) } }, Modifier.weight(1f))
                }
            }
            Stepper(
                value = stringResource(R.string.duration_minutes_value, form.durationMinutes),
                onMinus = { viewModel.update { it.copy(durationMinutes = (it.durationMinutes - 5).coerceIn(1, 600)) } },
                onPlus = { viewModel.update { it.copy(durationMinutes = (it.durationMinutes + 5).coerceIn(1, 600)) } },
                modifier = Modifier.padding(top = 10.dp)
            )
        }
        GoalType.YES_NO -> Unit
    }
}

// Stevec z vnosnim poljem v sredini (npr. 20 strani)
@Composable
private fun AmountStepper(value: String, onValueChange: (String) -> Unit, onStep: (Double) -> Unit, isError: Boolean, modifier: Modifier) {
    val palette = StreakTheme.palette
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFFFFF3D1))
            .then(if (isError) Modifier.border(2.dp, palette.danger, RoundedCornerShape(18.dp)) else Modifier)
            .padding(horizontal = 6.dp)
    ) {
        RoundIconButton(Icons.Rounded.Remove, stringResource(R.string.decrease), { onStep(-1.0) }, container = InkOnPastel, tint = Color.White, size = 40.dp)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.headlineSmall.copy(color = InkOnPastel, textAlign = TextAlign.Center),
            cursorBrush = SolidColor(InkOnPastel),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.Center) {
                    if (value.isEmpty()) Text("0", style = MaterialTheme.typography.headlineSmall, color = InkOnPastel.copy(alpha = .35f))
                    inner()
                }
            }
        )
        RoundIconButton(Icons.Rounded.Add, stringResource(R.string.increase), { onStep(1.0) }, container = InkOnPastel, tint = Color.White, size = 40.dp)
    }
}

// Mreza ikon 4 stolpcev; vsaka ploscica ima svojo pastelno barvo, izbrana je obrobljena
@Composable
private fun IconGrid(selected: String, onSelect: (String) -> Unit) {
    val palette = StreakTheme.palette
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        IconMap.all.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { icon ->
                    val isSel = icon.name == selected
                    val shape = RoundedCornerShape(20.dp)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(shape)
                            .background(parseHexColor(icon.tileHex))
                            .border(3.dp, if (isSel) palette.ink else Color.Transparent, shape)
                            .selectable(selected = isSel, role = Role.RadioButton) { onSelect(icon.name) }
                            .semantics { contentDescription = icon.name }
                    ) { Icon(icon.vector, contentDescription = null, tint = InkOnPastel, modifier = Modifier.size(26.dp)) }
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorRow(selected: String, onSelect: (String) -> Unit) {
    val palette = StreakTheme.palette
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        HabitColors.forEach { hex ->
            val isSel = hex.equals(selected, ignoreCase = true)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .border(3.dp, if (isSel) palette.ink else Color.Transparent, CircleShape)
                    .padding(5.dp)
                    .clip(CircleShape)
                    .background(parseHexColor(hex))
                    .selectable(selected = isSel, role = Role.RadioButton) { onSelect(hex) }
                    .semantics { contentDescription = hex }
            ) { if (isSel) Icon(Icons.Rounded.Check, null, tint = InkOnPastel, modifier = Modifier.size(18.dp)) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RemindersSection(form: HabitForm, viewModel: AddEditHabitViewModel) {
    val locale = LocalConfiguration.current.locales[0]
    val formatter = remember(locale) { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale) }
    var picking by remember { mutableStateOf(false) }

    FieldLabel(stringResource(R.string.reminders_label))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        form.reminders.forEach { time ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Lavender.copy(alpha = .55f))
                    .padding(start = 16.dp)
            ) {
                Text(time.format(formatter), style = MaterialTheme.typography.titleMedium, color = InkOnPastel)
                RoundIconButton(
                    Icons.Rounded.Close,
                    stringResource(R.string.remove_reminder, time.format(formatter)),
                    { viewModel.removeReminder(time) },
                    container = Color.Transparent,
                    tint = InkOnPastel
                )
            }
        }
        if (form.reminders.size < com.streak.app.notifications.AlarmScheduler.MAX_REMINDERS) {
            DashedAddButton(stringResource(R.string.add), { picking = true })
        }
    }
    if (form.reminders.isNotEmpty()) ReminderPermissionHint()

    if (picking) {
        TimePickerSheet(
            title = stringResource(R.string.add_reminder),
            initial = form.reminders.lastOrNull()?.plusHours(1) ?: defaultReminder(form.timeOfDay),
            presets = listOf(
                TimePreset(LocalTime.of(7, 0), stringResource(R.string.filter_morning)),
                TimePreset(LocalTime.of(12, 30), stringResource(R.string.preset_lunch)),
                TimePreset(LocalTime.of(20, 0), stringResource(R.string.filter_evening))
            ),
            confirmText = stringResource(R.string.add),
            onDismiss = { picking = false },
            onConfirm = { viewModel.addReminder(it); picking = false }
        )
    }
}

private fun defaultReminder(timeOfDay: TimeOfDay): LocalTime = when (timeOfDay) {
    TimeOfDay.MORNING -> LocalTime.of(7, 0)
    TimeOfDay.AFTERNOON -> LocalTime.of(13, 0)
    TimeOfDay.EVENING, TimeOfDay.ANYTIME -> LocalTime.of(20, 0)
}

@Composable
private fun DatesSection(form: HabitForm, showErrors: Boolean, viewModel: AddEditHabitViewModel) {
    val locale = LocalConfiguration.current.locales[0]
    val formatter = remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale) }
    val today = LocalDate.now()
    var picking by remember { mutableStateOf<String?>(null) }

    FieldLabel(stringResource(R.string.dates_label))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        InfoField(
            Icons.Rounded.CalendarMonth,
            stringResource(R.string.start_date),
            if (form.startDate == today) stringResource(R.string.today) else form.startDate.format(formatter),
            { picking = "start" },
            Modifier.weight(1f)
        )
        InfoField(
            Icons.Rounded.CalendarMonth,
            stringResource(R.string.end_date),
            form.endDate?.format(formatter) ?: stringResource(R.string.end_date_none),
            { picking = "end" },
            Modifier.weight(1f)
        )
    }
    if (showErrors && form.endDateError) ErrorText(stringResource(R.string.error_end_before_start))

    picking?.let { which ->
        val start = which == "start"
        DatePickerSheet(
            title = stringResource(if (start) R.string.start_date else R.string.end_date),
            initial = if (start) form.startDate else form.endDate ?: form.startDate.plusMonths(1),
            today = today,
            firstDayOfWeek = form.firstDayOfWeek,
            presets = if (start) listOf(
                DatePreset(today, stringResource(R.string.today)),
                DatePreset(today.plusDays(1), stringResource(R.string.tomorrow)),
                DatePreset(nextWeekStart(today, form.firstDayOfWeek), form.firstDayOfWeek.getDisplayName(TextStyle.FULL_STANDALONE, locale).replaceFirstChar { it.titlecase(locale) })
            ) else emptyList(),
            minDate = if (start) null else form.startDate,
            extraAction = if (!start) stringResource(R.string.end_date_none) to { viewModel.update { it.copy(endDate = null) } } else null,
            onDismiss = { picking = null },
            onConfirm = { date ->
                viewModel.update { if (start) it.copy(startDate = date) else it.copy(endDate = date) }
                picking = null
            }
        )
    }
}

@Composable
private fun ErrorText(text: String) {
    Text(text, color = StreakTheme.palette.danger, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
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
private fun scheduleDescription(type: ScheduleType) = stringResource(
    when (type) {
        ScheduleType.DAILY -> R.string.interval_daily_desc
        ScheduleType.WEEKDAYS -> R.string.interval_weekdays_desc
        ScheduleType.TIMES_PER_WEEK -> R.string.interval_times_week_desc
        ScheduleType.TIMES_PER_MONTH -> R.string.interval_times_month_desc
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

// Opomniki brez obvestil ali natancnih alarmov ne delujejo (dobro): povej in ponudi nastavitev
@Composable
private fun ReminderPermissionHint() {
    val context = LocalContext.current
    var notificationsOn by remember { mutableStateOf(true) }
    var exactOn by remember { mutableStateOf(true) }
    LifecycleResumeEffect(Unit) {
        notificationsOn = NotificationManagerCompat.from(context).areNotificationsEnabled()
        exactOn = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
        onPauseOrDispose { }
    }
    val (text, intent) = when {
        !notificationsOn -> stringResource(R.string.reminders_hint_notifications) to
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        !exactOn && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> stringResource(R.string.reminders_hint_exact) to
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, "package:${context.packageName}".toUri())
        else -> return
    }
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .padding(top = 10.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(role = Role.Button) { runCatching { context.startActivity(intent) } }
            .padding(vertical = 6.dp)
    )
}
