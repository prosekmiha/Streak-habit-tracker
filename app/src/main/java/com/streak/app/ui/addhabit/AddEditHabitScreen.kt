package com.streak.app.ui.addhabit

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.streak.app.R
import com.streak.app.domain.model.HabitCategory
import com.streak.app.domain.model.TimeOfDay
import com.streak.app.ui.components.ButtonVariant
import com.streak.app.ui.components.ChoiceChips
import com.streak.app.ui.components.DialogAction
import com.streak.app.ui.components.FieldLabel
import com.streak.app.ui.components.ScreenTopBar
import com.streak.app.ui.components.SegmentedControl
import com.streak.app.ui.components.StreakButton
import com.streak.app.ui.components.StreakDialog
import com.streak.app.ui.components.StreakTextField
import com.streak.app.ui.components.TextAction
import com.streak.app.ui.mascot.MascotMood
import com.streak.app.ui.theme.StreakTheme

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
