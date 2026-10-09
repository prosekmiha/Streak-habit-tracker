package com.streak.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.streak.app.R
import com.streak.app.domain.model.CompletionStatus
import com.streak.app.domain.model.DayState
import com.streak.app.domain.model.GoalType
import com.streak.app.domain.model.StreakUnit
import com.streak.app.ui.components.HabitCardUi
import com.streak.app.ui.components.IconMap
import com.streak.app.ui.components.StreakTextField
import com.streak.app.ui.components.formatAmount
import com.streak.app.ui.components.parseHexColor
import com.streak.app.ui.theme.InkOnPastel
import com.streak.app.ui.theme.StreakTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

// Dejanja ob dolgem pritisku na kartico: opravi/odznaci, kolicina, preskok,
// zamrznitev, opomba, urejanje, arhiv.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitActionsSheet(
    item: HabitCardUi,
    date: LocalDate,
    isToday: Boolean,
    onDismiss: () -> Unit,
    onToggle: () -> Unit,
    onSetStatus: (CompletionStatus?) -> Unit,
    onAddProgress: (Double) -> Unit,
    onSaveNote: (String) -> Unit,
    onEdit: () -> Unit,
    onArchive: () -> Unit
) {
    val habit = item.habit
    val palette = StreakTheme.palette
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val locale = LocalConfiguration.current.locales[0]
    var note by remember(habit.id, date) { mutableStateOf("") }
    val hasRecord = item.state in setOf(DayState.DONE, DayState.PARTIAL, DayState.SKIPPED, DayState.FROZEN)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(48.dp).background(parseHexColor(habit.colorHex), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) { Icon(IconMap.resolve(habit.icon), null, tint = InkOnPastel) }
                Column(Modifier.padding(start = 14.dp).weight(1f)) {
                    Text(habit.title, style = MaterialTheme.typography.headlineSmall)
                    if (!isToday) {
                        Text(
                            date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale)),
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.inkSoft
                        )
                    }
                }
            }

            if (item.currentStreak > 0) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp)) {
                    Icon(Icons.Outlined.LocalFireDepartment, null, tint = MaterialTheme.colorScheme.primary)
                    Text(streakText(item.currentStreak, habit.schedule.streakUnit), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 6.dp))
                }
            }

            if (habit.goalType != GoalType.YES_NO) {
                val step = if (habit.goalType == GoalType.DURATION) 5 * 60.0 else 1.0
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .background(palette.surfaceSoft, RoundedCornerShape(18.dp))
                        .padding(8.dp)
                ) {
                    FilledTonalIconButton(onClick = { onAddProgress(-step) }) {
                        Icon(Icons.Filled.Remove, stringResource(R.string.decrease))
                    }
                    Text(progressText(item), style = MaterialTheme.typography.titleLarge)
                    FilledTonalIconButton(onClick = { onAddProgress(step) }) {
                        Icon(Icons.Filled.Add, stringResource(R.string.increase))
                    }
                }
            }

            Column(Modifier.padding(top = 12.dp)) {
                ActionRow(
                    if (item.isDone) Icons.Outlined.RadioButtonUnchecked else Icons.Outlined.CheckCircle,
                    stringResource(if (item.isDone) R.string.action_mark_not_done else R.string.action_mark_done)
                ) { onToggle(); onDismiss() }
                if (item.state == DayState.SKIPPED) {
                    ActionRow(Icons.Outlined.SkipNext, stringResource(R.string.action_unskip)) { onSetStatus(null); onDismiss() }
                } else {
                    ActionRow(Icons.Outlined.SkipNext, stringResource(R.string.action_skip)) { onSetStatus(CompletionStatus.SKIPPED); onDismiss() }
                }
                if (item.state == DayState.FROZEN) {
                    ActionRow(Icons.Outlined.AcUnit, stringResource(R.string.action_unfreeze)) { onSetStatus(null); onDismiss() }
                } else {
                    ActionRow(Icons.Outlined.AcUnit, stringResource(R.string.action_freeze)) { onSetStatus(CompletionStatus.FROZEN); onDismiss() }
                }
                ActionRow(Icons.Outlined.Edit, stringResource(R.string.action_edit), onEdit)
                ActionRow(Icons.Outlined.Archive, stringResource(R.string.action_archive), onArchive)
            }

            if (hasRecord) {
                StreakTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = stringResource(R.string.note_label),
                    placeholder = stringResource(R.string.note_placeholder),
                    filled = true,
                    singleLine = false
                )
                TextButton(
                    onClick = { onSaveNote(note); onDismiss() },
                    enabled = note.isNotBlank(),
                    modifier = Modifier.align(Alignment.End)
                ) { Text(stringResource(R.string.save), style = MaterialTheme.typography.labelLarge) }
            }
        }
    }
}

@Composable
private fun ActionRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 4.dp)
    ) {
        Icon(icon, contentDescription = null)
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 16.dp))
    }
}

@Composable
private fun progressText(item: HabitCardUi): String {
    val habit = item.habit
    return if (habit.goalType == GoalType.DURATION) {
        stringResource(R.string.habit_progress_minutes, (item.value / 60).toInt(), (habit.targetValue / 60).toInt())
    } else {
        stringResource(R.string.habit_progress_quantity, formatAmount(item.value), formatAmount(habit.targetValue), habit.unit.orEmpty()).trim()
    }
}

@Composable
fun streakText(count: Int, unit: StreakUnit): String = pluralStringResource(
    when (unit) {
        StreakUnit.DAY -> R.plurals.streak_days
        StreakUnit.WEEK -> R.plurals.streak_weeks
        StreakUnit.MONTH -> R.plurals.streak_months
    },
    count, count
)
