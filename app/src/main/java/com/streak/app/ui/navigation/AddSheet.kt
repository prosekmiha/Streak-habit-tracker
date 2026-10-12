package com.streak.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.SentimentSatisfied
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.streak.app.R
import com.streak.app.ui.components.IconMap
import com.streak.app.ui.components.StreakSheet
import com.streak.app.ui.components.parseHexColor
import com.streak.app.ui.onboarding.HabitTemplates
import com.streak.app.ui.theme.InkOnPastel
import com.streak.app.ui.theme.Lime
import com.streak.app.ui.theme.Sky
import com.streak.app.ui.theme.StreakTheme
import com.streak.app.ui.theme.Sun

// List ob gumbu "+": navada, naloga ali hitro iz predloge
@Composable
fun AddSheet(
    onDismiss: () -> Unit,
    onNewHabit: () -> Unit,
    onNewTask: () -> Unit,
    onTemplate: (String) -> Unit,
    onLogMood: () -> Unit
) {
    StreakSheet(onDismiss = onDismiss, title = stringResource(R.string.add_sheet_title), showClose = true) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BigTile(stringResource(R.string.add_habit_tile), stringResource(R.string.new_habit_desc), Icons.Rounded.Repeat, Lime, Modifier.weight(1f), onNewHabit)
            BigTile(stringResource(R.string.add_task_tile), stringResource(R.string.new_task_desc), Icons.Rounded.Checklist, Sky, Modifier.weight(1f), onNewTask)
        }
        WideTile(stringResource(R.string.mood_add_tile), stringResource(R.string.mood_add_tile_desc), Icons.Rounded.SentimentSatisfied, Sun, onLogMood)
        Text(
            stringResource(R.string.quick_from_template),
            style = MaterialTheme.typography.titleSmall,
            color = StreakTheme.palette.inkSoft,
            modifier = Modifier.padding(top = 22.dp, bottom = 10.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            HabitTemplates.all.forEach { t ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .heightIn(min = 44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(parseHexColor(t.colorHex))
                        .clickable(role = Role.Button) { onTemplate(t.id) }
                        .padding(horizontal = 14.dp)
                ) {
                    Icon(IconMap.resolve(t.icon), null, tint = InkOnPastel, modifier = Modifier.size(18.dp))
                    Text(stringResource(t.title), style = MaterialTheme.typography.labelLarge, color = InkOnPastel, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun WideTile(title: String, subtitle: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(top = 12.dp)
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(color)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(InkOnPastel)
        ) { Icon(icon, null, tint = color, modifier = Modifier.size(22.dp)) }
        Column(Modifier.padding(start = 14.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = InkOnPastel)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = InkOnPastel)
        }
    }
}

@Composable
private fun BigTile(title: String, subtitle: String, icon: ImageVector, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .height(170.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(color)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(18.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(InkOnPastel)
        ) { Icon(icon, null, tint = color, modifier = Modifier.size(24.dp)) }
        Spacer(Modifier.weight(1f))
        Text(title, style = MaterialTheme.typography.titleLarge, color = InkOnPastel)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = InkOnPastel)
    }
}
