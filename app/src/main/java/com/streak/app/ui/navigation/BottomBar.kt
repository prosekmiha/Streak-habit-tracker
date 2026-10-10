package com.streak.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.streak.app.R
import com.streak.app.ui.theme.StreakTheme

// Spodnja vrstica po dizajnu: ikone s piko pod aktivno, velik moder gumb "+", avatar za profil
@Composable
fun StreakBottomBar(
    current: String,
    userName: String,
    onNavigate: (String) -> Unit,
    onAdd: () -> Unit
) {
    val palette = StreakTheme.palette
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)) {
        HorizontalDivider(color = palette.border)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(76.dp)
                .padding(horizontal = 14.dp)
                .selectableGroup(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            NavItem(if (current == Routes.HOME) Icons.Rounded.Home else Icons.Outlined.Home, stringResource(R.string.nav_today), current == Routes.HOME) { onNavigate(Routes.HOME) }
            NavItem(Icons.Rounded.Checklist, stringResource(R.string.nav_tasks), current == Routes.TASKS) { onNavigate(Routes.TASKS) }

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
                Icon(Icons.Rounded.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
            }

            NavItem(Icons.Rounded.BarChart, stringResource(R.string.nav_stats), current == Routes.STATS) { onNavigate(Routes.STATS) }
            ProfileItem(userName, current == Routes.PROFILE) { onNavigate(Routes.PROFILE) }
        }
    }
}

@Composable
private fun NavItem(icon: ImageVector, label: String, active: Boolean, onClick: () -> Unit) {
    val palette = StreakTheme.palette
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(18.dp))
            .selectable(selected = active, role = Role.Tab, onClick = onClick)
            .semantics { contentDescription = label }
            .padding(top = 10.dp)
    ) {
        Icon(icon, contentDescription = null, tint = if (active) palette.ink else palette.inkSoft, modifier = Modifier.size(27.dp))
        Spacer(Modifier.height(5.dp))
        Box(Modifier.size(5.dp).clip(CircleShape).background(if (active) palette.ink else Color.Transparent))
    }
}

@Composable
private fun ProfileItem(userName: String, active: Boolean, onClick: () -> Unit) {
    val palette = StreakTheme.palette
    val label = stringResource(R.string.nav_profile)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(18.dp))
            .selectable(selected = active, role = Role.Tab, onClick = onClick)
            .semantics { contentDescription = label }
            .padding(top = 8.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(31.dp)
                .clip(CircleShape)
                .background(if (active) palette.ink else MaterialTheme.colorScheme.primaryContainer)
        ) {
            Text(
                userName.ifBlank { "?" }.take(1).uppercase(),
                style = MaterialTheme.typography.titleSmall,
                color = if (active) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Spacer(Modifier.height(4.dp))
        Box(Modifier.size(5.dp).clip(CircleShape).background(if (active) palette.ink else Color.Transparent))
    }
}
