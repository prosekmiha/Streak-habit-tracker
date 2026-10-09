package com.streak.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.streak.app.BuildConfig
import com.streak.app.R
import com.streak.app.domain.model.AnimationLevel
import com.streak.app.domain.model.Habit
import com.streak.app.domain.model.ThemeMode
import com.streak.app.ui.components.IconMap
import com.streak.app.ui.components.StreakTextField
import com.streak.app.ui.components.parseHexColor
import com.streak.app.ui.theme.InkOnPastel
import com.streak.app.ui.theme.StreakTheme
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle

@Composable
fun SettingsScreen(
    onEditHabit: (String) -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val s = state.settings
    val locale = LocalConfiguration.current.locales[0]
    var editingName by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Habit?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp)
    ) {
        // Profil
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)) {
            Box(
                Modifier.size(64.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    s.userName.ifBlank { "?" }.take(1).uppercase(locale),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
            Column(Modifier.weight(1f).padding(start = 16.dp)) {
                Text(
                    s.userName.ifBlank { stringResource(R.string.profile_no_name) },
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.semantics { heading() }
                )
                Text(stringResource(R.string.privacy_short), style = MaterialTheme.typography.bodySmall, color = StreakTheme.palette.inkSoft)
            }
            IconButton(onClick = { editingName = true }) {
                Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.settings_name))
            }
        }

        Section(stringResource(R.string.settings_appearance)) {
            Label(stringResource(R.string.settings_theme))
            Chips(
                options = ThemeMode.entries,
                selected = s.themeMode,
                label = {
                    stringResource(
                        when (it) {
                            ThemeMode.SYSTEM -> R.string.theme_system
                            ThemeMode.LIGHT -> R.string.theme_light
                            ThemeMode.DARK -> R.string.theme_dark
                        }
                    )
                },
                onSelect = viewModel::setTheme
            )
            Label(stringResource(R.string.settings_animations))
            Chips(
                options = AnimationLevel.entries,
                selected = s.animationLevel,
                label = {
                    stringResource(
                        when (it) {
                            AnimationLevel.FULL -> R.string.animations_full
                            AnimationLevel.REDUCED -> R.string.animations_reduced
                            AnimationLevel.OFF -> R.string.animations_off
                        }
                    )
                },
                onSelect = viewModel::setAnimations
            )
        }

        Section(stringResource(R.string.settings_days)) {
            Label(stringResource(R.string.settings_first_day))
            Chips(
                options = listOf(DayOfWeek.MONDAY, DayOfWeek.SUNDAY, DayOfWeek.SATURDAY),
                selected = s.firstDayOfWeek,
                label = { it.getDisplayName(TextStyle.FULL_STANDALONE, locale).replaceFirstChar { c -> c.titlecase(locale) } },
                onSelect = viewModel::setFirstDayOfWeek
            )
            Label(stringResource(R.string.settings_day_ends_at))
            Text(stringResource(R.string.settings_day_ends_at_desc), style = MaterialTheme.typography.bodySmall, color = StreakTheme.palette.inkSoft)
            val timeFormat = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)
            Chips(
                options = (0..6).map { LocalTime.of(it, 0) },
                selected = s.dayEndsAt,
                label = { if (it == LocalTime.MIDNIGHT) stringResource(R.string.settings_midnight) else it.format(timeFormat) },
                onSelect = viewModel::setDayEndsAt
            )
        }

        Section(stringResource(R.string.settings_manage)) {
            if (state.habits.isEmpty()) {
                Text(stringResource(R.string.no_habits), style = MaterialTheme.typography.bodyMedium, color = StreakTheme.palette.inkSoft)
            }
            state.habits.forEach { habit -> HabitRow(habit, onClick = { onEditHabit(habit.id) }) }
            if (state.archived.isNotEmpty()) {
                Label(stringResource(R.string.settings_archived))
                state.archived.forEach { habit ->
                    HabitRow(habit, onClick = { onEditHabit(habit.id) }) {
                        TextButton(onClick = { viewModel.unarchive(habit) }) { Text(stringResource(R.string.action_unarchive)) }
                        TextButton(onClick = { pendingDelete = habit }) {
                            Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        Section(stringResource(R.string.settings_data)) {
            ActionRow(stringResource(R.string.settings_export), onClick = viewModel::export)
            ActionRow(
                stringResource(if (state.recalculating) R.string.settings_recalculating else R.string.settings_recalculate),
                onClick = viewModel::recalculateAll
            )
        }

        Section(stringResource(R.string.settings_about)) {
            Text(stringResource(R.string.privacy_long), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.health_disclaimer), style = MaterialTheme.typography.bodySmall, color = StreakTheme.palette.inkSoft, modifier = Modifier.padding(top = 10.dp))
            Text(stringResource(R.string.licenses_fonts), style = MaterialTheme.typography.bodySmall, color = StreakTheme.palette.inkSoft, modifier = Modifier.padding(top = 10.dp))
            Text(stringResource(R.string.app_version, BuildConfig.VERSION_NAME), style = MaterialTheme.typography.bodySmall, color = StreakTheme.palette.inkSoft, modifier = Modifier.padding(top = 10.dp))
        }
    }

    if (editingName) {
        var name by remember { mutableStateOf(s.userName) }
        AlertDialog(
            onDismissRequest = { editingName = false },
            title = { Text(stringResource(R.string.settings_name)) },
            text = { StreakTextField(value = name, onValueChange = { name = it }) },
            confirmButton = { TextButton(onClick = { viewModel.setName(name); editingName = false }) { Text(stringResource(R.string.save)) } },
            dismissButton = { TextButton(onClick = { editingName = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }

    state.exportJson?.let { json ->
        AlertDialog(
            onDismissRequest = viewModel::clearExport,
            confirmButton = { TextButton(onClick = viewModel::clearExport) { Text(stringResource(R.string.close)) } },
            title = { Text(stringResource(R.string.export_title)) },
            text = {
                SelectionContainer {
                    Text(json, style = MaterialTheme.typography.bodySmall, modifier = Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()))
                }
            }
        )
    }

    pendingDelete?.let { habit ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.delete_habit_title)) },
            text = { Text(stringResource(R.string.delete_habit_text, habit.title)) },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteHabit(habit); pendingDelete = null }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Text(
        title,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp).semantics { heading() }
    )
    Column(
        Modifier
            .fillMaxWidth()
            .background(StreakTheme.palette.surfaceSoft, RoundedCornerShape(22.dp))
            .padding(16.dp)
    ) { content() }
}

@Composable
private fun Label(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp, bottom = 6.dp))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> Chips(options: List<T>, selected: T, label: @Composable (T) -> String, onSelect: (T) -> Unit) {
    val palette = StreakTheme.palette
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(label(option), style = MaterialTheme.typography.labelLarge) },
                shape = RoundedCornerShape(14.dp),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    selectedContainerColor = palette.daySelected,
                    selectedLabelColor = palette.daySelectedInk
                ),
                border = null,
                modifier = Modifier.heightIn(min = 44.dp)
            )
        }
    }
}

@Composable
private fun HabitRow(habit: Habit, onClick: () -> Unit, trailing: @Composable () -> Unit = {
    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
}) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clickable(role = Role.Button, onClick = onClick)
    ) {
        Box(
            Modifier.size(36.dp).background(parseHexColor(habit.colorHex), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) { Icon(IconMap.resolve(habit.icon), null, tint = InkOnPastel, modifier = Modifier.size(20.dp)) }
        Text(
            habit.title,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
        )
        trailing()
    }
}

@Composable
private fun ActionRow(text: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clickable(role = Role.Button, onClick = onClick)
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = StreakTheme.palette.inkSoft)
    }
}
