package com.streak.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.streak.app.R
import com.streak.app.ui.theme.StreakTheme

// Spodnja vrstica: Domov, Naloge, velik gumb "+", Statistika, Profil (kot v dizajnu le ikone)
@Composable
fun StreakBottomBar(
    current: String,
    userName: String,
    onNavigate: (String) -> Unit,
    onAdd: () -> Unit
) {
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)) {
        HorizontalDivider(color = StreakTheme.palette.border)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(72.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            NavItem(Icons.Outlined.Home, Icons.Filled.Home, stringResource(R.string.nav_today), current == Routes.HOME) { onNavigate(Routes.HOME) }
            NavItem(Icons.Outlined.Checklist, Icons.Filled.Checklist, stringResource(R.string.nav_tasks), current == Routes.TASKS) { onNavigate(Routes.TASKS) }

            val addLabel = stringResource(R.string.add)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .selectable(selected = false, role = Role.Button, onClick = onAdd)
                    .semantics { contentDescription = addLabel }
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(28.dp))
            }

            NavItem(Icons.Outlined.BarChart, Icons.Filled.BarChart, stringResource(R.string.nav_stats), current == Routes.STATS) { onNavigate(Routes.STATS) }
            ProfileItem(userName, current == Routes.PROFILE) { onNavigate(Routes.PROFILE) }
        }
    }
}

@Composable
private fun NavItem(icon: ImageVector, activeIcon: ImageVector, label: String, active: Boolean, onClick: () -> Unit) {
    val palette = StreakTheme.palette
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .selectable(selected = active, role = Role.Tab, onClick = onClick)
            .semantics { contentDescription = label }
    ) {
        Icon(if (active) activeIcon else icon, contentDescription = null, tint = if (active) palette.ink else palette.inkSoft, modifier = Modifier.size(26.dp))
    }
}

// Profil kot avatar z zacetnico (kot v dizajnu)
@Composable
private fun ProfileItem(userName: String, active: Boolean, onClick: () -> Unit) {
    val palette = StreakTheme.palette
    val label = stringResource(R.string.nav_profile)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .selectable(selected = active, role = Role.Tab, onClick = onClick)
            .semantics { contentDescription = label }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(34.dp)
                .background(if (active) palette.ink else MaterialTheme.colorScheme.primaryContainer, CircleShape)
        ) {
            Text(
                userName.ifBlank { "?" }.take(1).uppercase(),
                style = MaterialTheme.typography.titleSmall,
                color = if (active) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}
