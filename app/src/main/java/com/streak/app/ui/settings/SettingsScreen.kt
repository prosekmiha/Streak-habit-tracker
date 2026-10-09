package com.streak.app.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.streak.app.R
import com.streak.app.domain.model.Habit
import com.streak.app.domain.model.ThemeMode
import com.streak.app.ui.components.IconMap
import com.streak.app.ui.components.parseHexColor

@Composable
fun SettingsScreen(
    onEditHabit: (String) -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<Habit?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
    ) {
        Text(
            text = stringRes(R.string.nav_more),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(vertical = 14.dp)
        )

        // Ime uporabnika
        SectionCard {
            Text(stringRes(R.string.settings_name), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            OutlinedTextField(
                value = state.userName,
                onValueChange = { viewModel.setName(it) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
        }

        // Tema
        SectionCard {
            Text(stringRes(R.string.settings_theme), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Row(modifier = Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeOption(stringRes(R.string.theme_system), state.themeMode == ThemeMode.SYSTEM) { viewModel.setTheme(ThemeMode.SYSTEM) }
                ThemeOption(stringRes(R.string.theme_light), state.themeMode == ThemeMode.LIGHT) { viewModel.setTheme(ThemeMode.LIGHT) }
                ThemeOption(stringRes(R.string.theme_dark), state.themeMode == ThemeMode.DARK) { viewModel.setTheme(ThemeMode.DARK) }
            }
        }

        // Upravljanje navad
        SectionCard {
            Text(stringRes(R.string.settings_manage), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Column(modifier = Modifier.padding(top = 6.dp)) {
                state.habits.forEach { habit ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val color = parseHexColor(habit.colorHex)
                        Box(
                            modifier = Modifier.size(32.dp).background(color.copy(alpha = 0.14f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) { Icon(IconMap.resolve(habit.icon), contentDescription = null, tint = color, modifier = Modifier.size(17.dp)) }
                        Text(habit.title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                        IconButton(onClick = { onEditHabit(habit.id) }) { Icon(Icons.Filled.Edit, contentDescription = "Uredi", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(19.dp)) }
                        IconButton(onClick = { pendingDelete = habit }) { Icon(Icons.Filled.DeleteOutline, contentDescription = "Izbriši", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(19.dp)) }
                    }
                }
                if (state.habits.isEmpty()) Text("Ni navad", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
            }
        }

        // Izvoz JSON
        SectionCard {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { viewModel.export() }.padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringRes(R.string.settings_export), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            }
        }

        // Oblacna sinhronizacija (placeholder)
        SectionCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringRes(R.string.settings_cloud), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text(stringRes(R.string.settings_cloud_desc), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = state.cloudSync, onCheckedChange = { viewModel.setCloud(it) })
            }
        }

        Box(modifier = Modifier.size(24.dp))
    }

    // Dialog izvoza
    state.exportJson?.let { json ->
        AlertDialog(
            onDismissRequest = { viewModel.clearExport() },
            confirmButton = { TextButton(onClick = { viewModel.clearExport() }) { Text("Zapri") } },
            title = { Text("Izvoz (JSON)") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(json, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    // Potrditev brisanja
    pendingDelete?.let { habit ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteHabit(habit); pendingDelete = null }) {
                    Text(stringRes(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text(stringRes(R.string.cancel)) } },
            title = { Text("Izbrišem navado?") },
            text = { Text("\"${habit.title}\" bo trajno odstranjena.") }
        )
    }
}

@Composable
private fun ThemeOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(22.dp))
            .padding(18.dp)
    ) { content() }
}

@Composable private fun stringRes(id: Int) = androidx.compose.ui.res.stringResource(id)
