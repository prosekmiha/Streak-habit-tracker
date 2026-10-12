package com.streak.app.ui.settings

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.streak.app.R
import com.streak.app.domain.model.Habit
import com.streak.app.tiles.QuickTiles
import com.streak.app.ui.components.ButtonVariant
import com.streak.app.ui.components.FieldLabel
import com.streak.app.ui.components.IconMap
import com.streak.app.ui.components.StreakButton
import com.streak.app.ui.components.StreakSheet
import com.streak.app.ui.components.parseHexColor
import com.streak.app.ui.theme.InkOnPastel
import com.streak.app.ui.theme.StreakTheme

// Izbira navad za ploscici v hitrih nastavitvah (navada in seja)
@Composable
internal fun TilesSheet(
    habits: List<Habit>,
    habitTile: String?,
    sessionTile: String?,
    onHabitTile: (String?) -> Unit,
    onSessionTile: (String?) -> Unit,
    onAddTile: (session: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = StreakTheme.palette
    val canAdd = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    StreakSheet(onDismiss = onDismiss, title = stringResource(R.string.tiles_title), showClose = true) {
        Text(stringResource(R.string.tiles_desc), style = MaterialTheme.typography.bodyMedium, color = palette.inkSoft)

        FieldLabel(stringResource(R.string.tile_habit_label))
        HabitChoices(habits.filter(QuickTiles::supportsHabitTile), habitTile, onHabitTile)
        if (canAdd && habitTile != null) {
            StreakButton(stringResource(R.string.tiles_add), { onAddTile(false) }, Modifier.padding(top = 4.dp), ButtonVariant.Secondary, height = 48.dp)
        }

        FieldLabel(stringResource(R.string.tile_session_label))
        val durations = habits.filter(QuickTiles::supportsSessionTile)
        if (durations.isEmpty()) {
            Text(stringResource(R.string.tiles_no_session_habits), style = MaterialTheme.typography.bodySmall, color = palette.inkSoft)
        } else {
            HabitChoices(durations, sessionTile, onSessionTile)
        }
        if (canAdd && sessionTile != null) {
            StreakButton(stringResource(R.string.tiles_add), { onAddTile(true) }, Modifier.padding(top = 4.dp), ButtonVariant.Secondary, height = 48.dp)
        }
    }
}

@Composable
private fun HabitChoices(habits: List<Habit>, selected: String?, onSelect: (String?) -> Unit) {
    Column(Modifier.selectableGroup()) {
        ChoiceRow(stringResource(R.string.tiles_none), Icons.Rounded.Block, null, selected == null) { onSelect(null) }
        habits.forEach { h ->
            ChoiceRow(h.title, IconMap.resolve(h.icon), h.colorHex, selected == h.id) { onSelect(h.id) }
        }
    }
}

@Composable
private fun ChoiceRow(title: String, icon: ImageVector, colorHex: String?, selected: Boolean, onClick: () -> Unit) {
    val palette = StreakTheme.palette
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(palette.surfaceSoft)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 10.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(colorHex?.let(::parseHexColor) ?: palette.border)
        ) { Icon(icon, null, tint = InkOnPastel, modifier = Modifier.size(18.dp)) }
        Text(title, style = MaterialTheme.typography.titleSmall, color = palette.ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(horizontal = 10.dp))
        if (selected) Icon(Icons.Rounded.Check, null, tint = palette.ink, modifier = Modifier.size(22.dp))
    }
}
