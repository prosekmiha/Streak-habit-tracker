package com.streak.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.streak.app.R
import com.streak.app.domain.model.CompletionStatus
import com.streak.app.domain.model.DayState
import com.streak.app.domain.model.GoalType
import com.streak.app.domain.model.StreakUnit
import com.streak.app.ui.components.ButtonVariant
import com.streak.app.ui.components.ChoiceChip
import com.streak.app.ui.components.HabitCardUi
import com.streak.app.ui.components.IconMap
import com.streak.app.ui.components.IconTile
import com.streak.app.ui.components.SheetActionRow
import com.streak.app.ui.components.Stepper
import com.streak.app.ui.components.StreakButton
import com.streak.app.ui.components.StreakSheet
import com.streak.app.ui.components.StreakTextField
import com.streak.app.ui.components.formatAmount
import com.streak.app.ui.components.parseHexColor
import com.streak.app.ui.theme.Bubblegum
import com.streak.app.ui.theme.InkOnPastel
import com.streak.app.ui.theme.Lavender
import com.streak.app.ui.theme.Lime
import com.streak.app.ui.theme.Sky
import com.streak.app.ui.theme.StreakTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

// Dolg pritisk na kartico: stevec/oznacitev, hitra dejanja, urejanje in arhiv
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
    onStartSession: () -> Unit,
    onDetails: () -> Unit,
    onEdit: () -> Unit,
    onArchive: () -> Unit
) {
    val habit = item.habit
    val palette = StreakTheme.palette
    val locale = LocalConfiguration.current.locales[0]
    val pastel = parseHexColor(habit.colorHex)
    var noteOpen by remember(habit.id, date) { mutableStateOf(false) }
    var note by remember(habit.id, date) { mutableStateOf(item.note.orEmpty()) }
    val hasRecord = item.state in setOf(DayState.DONE, DayState.PARTIAL, DayState.SKIPPED, DayState.FROZEN)

    StreakSheet(onDismiss = onDismiss) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(IconMap.resolve(habit.icon), pastel)
            Column(Modifier.padding(start = 14.dp).weight(1f)) {
                Text(habit.title, style = MaterialTheme.typography.headlineSmall, color = palette.ink)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                    Icon(Icons.Rounded.LocalFireDepartment, null, tint = Color(0xFFF4874B), modifier = Modifier.size(15.dp))
                    Text(
                        streakLine(item.currentStreak, item.bestStreak, item.unit) +
                            if (!isToday) " · " + date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)) else "",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.inkSoft,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }

        // Glavni del: stevec (kolicina/cas) ali oznacitev (da/ne)
        val soft = pastel.copy(alpha = .35f).compositeOver(MaterialTheme.colorScheme.surface)
        when (habit.goalType) {
            GoalType.QUANTITY -> {
                Stepper(
                    value = "${formatAmount(item.value)} / ${formatAmount(habit.targetValue)}",
                    caption = habit.unit?.let { stringResource(R.string.unit_today, it) },
                    onMinus = { onAddProgress(-1.0) },
                    onPlus = { onAddProgress(1.0) },
                    container = soft,
                    ink = InkOnPastel,
                    modifier = Modifier.padding(top = 18.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 10.dp)) {
                    ChoiceChip("+5", false, { onAddProgress(5.0) })
                    ChoiceChip("+10", false, { onAddProgress(10.0) })
                    StreakButton(
                        stringResource(R.string.mark_all, formatAmount(habit.targetValue)),
                        { onAddProgress((habit.targetValue - item.value).coerceAtLeast(0.0)) },
                        Modifier.weight(1f),
                        ButtonVariant.Dark,
                        height = 44.dp
                    )
                }
            }
            GoalType.DURATION -> {
                Stepper(
                    value = stringResource(R.string.habit_progress_minutes, (item.value / 60).toInt(), (habit.targetValue / 60).toInt()),
                    onMinus = { onAddProgress(-5 * 60.0) },
                    onPlus = { onAddProgress(5 * 60.0) },
                    container = soft,
                    ink = InkOnPastel,
                    modifier = Modifier.padding(top = 18.dp)
                )
                if (isToday) {
                    StreakButton(stringResource(R.string.start_session), onStartSession, Modifier.padding(top = 10.dp), ButtonVariant.Dark, icon = Icons.Rounded.PlayArrow, height = 48.dp)
                }
            }
            GoalType.YES_NO -> StreakButton(
                stringResource(if (item.isDone) R.string.action_mark_not_done else R.string.action_mark_done),
                { onToggle(); onDismiss() },
                Modifier.padding(top = 18.dp),
                if (item.isDone) ButtonVariant.Secondary else ButtonVariant.Dark
            )
        }

        // Hitra dejanja
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 20.dp)) {
            QuickAction(
                Icons.Rounded.SkipNext, Sky,
                stringResource(if (item.state == DayState.SKIPPED) R.string.action_unskip_short else R.string.action_skip_short),
                Modifier.weight(1f)
            ) { onSetStatus(if (item.state == DayState.SKIPPED) null else CompletionStatus.SKIPPED); onDismiss() }
            QuickAction(
                Icons.Rounded.AcUnit, Lavender,
                stringResource(if (item.state == DayState.FROZEN) R.string.action_unfreeze else R.string.action_freeze_short),
                Modifier.weight(1f)
            ) { onSetStatus(if (item.state == DayState.FROZEN) null else CompletionStatus.FROZEN); onDismiss() }
            QuickAction(Icons.Rounded.EditNote, Bubblegum, stringResource(R.string.note_label), Modifier.weight(1f), enabled = hasRecord) { noteOpen = !noteOpen }
            QuickAction(Icons.Rounded.BarChart, Lime, stringResource(R.string.details), Modifier.weight(1f), onClick = onDetails)
        }
        if (!hasRecord) {
            Text(stringResource(R.string.note_needs_record), style = MaterialTheme.typography.bodySmall, color = palette.inkSoft, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        }
        if (noteOpen && hasRecord) {
            StreakTextField(
                value = note,
                onValueChange = { note = it },
                placeholder = stringResource(R.string.note_placeholder),
                filled = true,
                singleLine = false,
                modifier = Modifier.padding(top = 12.dp)
            )
            StreakButton(stringResource(R.string.save), { onSaveNote(note); onDismiss() }, Modifier.padding(top = 8.dp), height = 48.dp)
        }

        HorizontalDivider(color = palette.border, modifier = Modifier.padding(top = 16.dp))
        SheetActionRow(Icons.Rounded.Edit, stringResource(R.string.action_edit), onEdit)
        SheetActionRow(Icons.Rounded.Archive, stringResource(R.string.action_archive), onArchive)
    }
}

@Composable
private fun QuickAction(icon: ImageVector, color: Color, label: String, modifier: Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(vertical = 4.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(58.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(if (enabled) color else StreakTheme.palette.surfaceSoft)
        ) { Icon(icon, null, tint = if (enabled) InkOnPastel else StreakTheme.palette.inkSoft, modifier = Modifier.size(24.dp)) }
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (enabled) StreakTheme.palette.ink else StreakTheme.palette.inkSoft,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Composable
private fun streakLine(current: Int, best: Int, unit: StreakUnit): String =
    stringResource(R.string.streak_and_best, streakText(current, unit), best)

@Composable
fun streakText(count: Int, unit: StreakUnit): String = pluralStringResource(
    when (unit) {
        StreakUnit.DAY -> R.plurals.streak_days
        StreakUnit.WEEK -> R.plurals.streak_weeks
        StreakUnit.MONTH -> R.plurals.streak_months
    },
    count, count
)
