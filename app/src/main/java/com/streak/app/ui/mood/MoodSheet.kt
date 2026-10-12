package com.streak.app.ui.mood

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.streak.app.R
import com.streak.app.ui.components.ChoiceChip
import com.streak.app.ui.components.FieldLabel
import com.streak.app.ui.components.StreakButton
import com.streak.app.ui.components.StreakSheet
import com.streak.app.ui.components.StreakTextField
import com.streak.app.ui.components.TextAction
import com.streak.app.ui.theme.InkOnPastel
import com.streak.app.ui.theme.StreakTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

// Zahteva za odprtje lista: dan (null = danes) in izraz, izbran ze na kartici
data class MoodRequest(val date: LocalDate? = null, val preset: Int? = null)

// List za vpis razpolozenja (iz kartice na domacem zaslonu, menija "+" ali obvestila)
@Composable
fun MoodSheet(request: MoodRequest, onDismiss: () -> Unit, viewModel: MoodEntryViewModel = hiltViewModel()) {
    LaunchedEffect(request) { viewModel.open(request.date, request.preset) }
    val form by viewModel.form.collectAsStateWithLifecycle()
    StreakSheet(onDismiss = onDismiss, title = stringResource(R.string.mood_title), showClose = true) {
        MoodSheetContent(
            form = form,
            onMood = viewModel::selectMood,
            onEnergy = viewModel::selectEnergy,
            onNote = viewModel::setNote,
            onSave = { viewModel.save(onDismiss) },
            onDelete = { viewModel.delete(onDismiss) }
        )
    }
}

@Composable
fun MoodSheetContent(
    form: MoodForm,
    onMood: (Int) -> Unit,
    onEnergy: (Int) -> Unit,
    onNote: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit
) {
    val palette = StreakTheme.palette
    val locale = LocalConfiguration.current.locales[0]
    form.date?.let {
        Text(
            it.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale)),
            style = MaterialTheme.typography.labelMedium,
            color = palette.inkSoft
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth().padding(top = 14.dp).selectableGroup()) {
        MoodUi.levels.forEach { level ->
            val selected = form.mood == level
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (selected) MoodUi.color(level) else palette.surfaceSoft)
                    .selectable(selected = selected, role = Role.RadioButton) { onMood(level) }
                    .padding(vertical = 10.dp)
            ) {
                Icon(MoodUi.icon(level), contentDescription = null, tint = if (selected) InkOnPastel else palette.ink, modifier = Modifier.size(32.dp))
                Text(
                    stringResource(MoodUi.label(level)),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) InkOnPastel else palette.inkSoft,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp, start = 2.dp, end = 2.dp)
                )
            }
        }
    }

    FieldLabel(stringResource(R.string.mood_energy_label))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().selectableGroup()) {
        MoodUi.levels.forEach { level ->
            ChoiceChip(level.toString(), form.energy == level, { onEnergy(level) }, Modifier.weight(1f))
        }
    }
    Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
        Text(stringResource(R.string.mood_energy_low), style = MaterialTheme.typography.labelSmall, color = palette.inkSoft, modifier = Modifier.weight(1f))
        Text(stringResource(R.string.mood_energy_high), style = MaterialTheme.typography.labelSmall, color = palette.inkSoft)
    }

    StreakTextField(
        value = form.note,
        onValueChange = onNote,
        placeholder = stringResource(R.string.mood_note_placeholder),
        filled = true,
        singleLine = false,
        modifier = Modifier.padding(top = 16.dp)
    )
    StreakButton(stringResource(R.string.save), onSave, Modifier.padding(top = 16.dp), enabled = form.canSave)
    if (form.exists) {
        TextAction(stringResource(R.string.mood_delete), onDelete, Modifier.fillMaxWidth().padding(top = 6.dp), color = palette.danger)
    }
}

// Kartica na domacem zaslonu: izbira izraza odpre list z ze izbranim izrazom
@Composable
fun MoodCard(onPick: (Int) -> Unit, onLater: () -> Unit, modifier: Modifier = Modifier) {
    val palette = StreakTheme.palette
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(palette.surfaceSoft)
            .padding(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.mood_card_text), style = MaterialTheme.typography.titleSmall, color = palette.ink, modifier = Modifier.weight(1f))
            TextAction(stringResource(R.string.later), onLater, color = palette.inkSoft)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            MoodUi.levels.forEach { level ->
                val label = stringResource(MoodUi.label(level))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MoodUi.color(level))
                        .clickable(role = Role.Button, onClickLabel = label) { onPick(level) }
                ) {
                    Icon(MoodUi.icon(level), contentDescription = label, tint = InkOnPastel, modifier = Modifier.size(28.dp))
                }
            }
        }
    }
}
