package com.streak.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.streak.app.R
import com.streak.app.ui.theme.Accent
import com.streak.app.ui.theme.Accent2

// Spodnja vrstica s 4 cilji in osrednjim FAB
@Composable
fun StreakBottomBar(
    current: String,
    onNavigate: (String) -> Unit,
    onAdd: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surface,
                RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
            )
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        NavItem(Icons.Filled.Home, stringRes(R.string.nav_today), current == Routes.TODAY) { onNavigate(Routes.TODAY) }
        NavItem(Icons.Filled.CalendarMonth, stringRes(R.string.nav_calendar), current == Routes.CALENDAR) { onNavigate(Routes.CALENDAR) }

        // FAB
        Box(
            modifier = Modifier
                .size(54.dp)
                .background(Brush.linearGradient(listOf(Accent, Accent2)), RoundedCornerShape(50))
                .clickable { onAdd() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Dodaj", tint = Color.White, modifier = Modifier.size(26.dp))
        }

        NavItem(Icons.Filled.BarChart, stringRes(R.string.nav_stats), current == Routes.STATS) { onNavigate(Routes.STATS) }
        NavItem(Icons.Filled.MoreHoriz, stringRes(R.string.nav_more), current == Routes.SETTINGS) { onNavigate(Routes.SETTINGS) }
    }
}

@Composable
private fun stringRes(id: Int): String = androidx.compose.ui.res.stringResource(id)

@Composable
private fun NavItem(icon: ImageVector, label: String, active: Boolean, onClick: () -> Unit) {
    val color = if (active) Accent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier.clickable { onClick() }.padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(22.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = color)
    }
}
