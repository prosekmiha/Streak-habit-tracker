package com.streak.app.ui.navigation

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.streak.app.ui.addhabit.AddEditHabitScreen
import com.streak.app.ui.addtask.AddEditTaskScreen
import com.streak.app.ui.calendar.CalendarScreen
import com.streak.app.ui.settings.SettingsScreen
import com.streak.app.ui.stats.StatsScreen
import com.streak.app.ui.theme.Accent
import com.streak.app.ui.today.TodayScreen

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun StreakApp() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route ?: Routes.TODAY

    // Spodnja vrstica skrita na modalnih ekranih
    val showBars = currentRoute in setOf(Routes.TODAY, Routes.CALENDAR, Routes.STATS, Routes.SETTINGS)

    var showAddSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBars) {
                StreakBottomBar(
                    current = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(Routes.TODAY) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onAdd = { showAddSheet = true }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.TODAY,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.TODAY) {
                TodayScreen(
                    onHabitClick = { id -> navController.navigate(Routes.editHabit(id)) },
                    onTaskClick = { id -> navController.navigate(Routes.editTask(id)) }
                )
            }
            composable(Routes.CALENDAR) { CalendarScreen() }
            composable(Routes.STATS) { StatsScreen() }
            composable(Routes.SETTINGS) {
                SettingsScreen(onEditHabit = { id -> navController.navigate(Routes.editHabit(id)) })
            }
            composable(Routes.ADD_HABIT) {
                AddEditHabitScreen(onClose = { navController.popBackStack() })
            }
            composable(
                Routes.EDIT_HABIT,
                arguments = listOf(navArgument("habitId") { type = NavType.StringType })
            ) {
                AddEditHabitScreen(onClose = { navController.popBackStack() })
            }
            composable(Routes.ADD_TASK) {
                AddEditTaskScreen(onClose = { navController.popBackStack() })
            }
            composable(
                Routes.EDIT_TASK,
                arguments = listOf(navArgument("taskId") { type = NavType.StringType })
            ) {
                AddEditTaskScreen(onClose = { navController.popBackStack() })
            }
        }
    }

    // Izbira: dodaj navado ali nalogo
    if (showAddSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp)) {
                AddOption("Nova navada", Icons.Filled.CheckCircle) {
                    showAddSheet = false
                    navController.navigate(Routes.ADD_HABIT)
                }
                AddOption("Nova naloga", Icons.Filled.TaskAlt) {
                    showAddSheet = false
                    navController.navigate(Routes.ADD_TASK)
                }
                Box(modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun AddOption(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier.size(40.dp).background(Accent.copy(alpha = 0.14f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) { Icon(icon, contentDescription = null, tint = Accent, modifier = Modifier.size(20.dp)) }
        Text(label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}
