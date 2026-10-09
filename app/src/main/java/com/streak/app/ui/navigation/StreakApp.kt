package com.streak.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.streak.app.R
import com.streak.app.ui.addhabit.AddEditHabitScreen
import com.streak.app.ui.addtask.AddEditTaskScreen
import com.streak.app.ui.home.HomeScreen
import com.streak.app.ui.settings.SettingsScreen
import com.streak.app.ui.stats.StatsScreen
import com.streak.app.ui.tasks.TasksScreen
import com.streak.app.ui.theme.InkOnPastel
import com.streak.app.ui.theme.Lime
import com.streak.app.ui.theme.Sky

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreakApp(userName: String) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route ?: Routes.HOME
    val showBars = currentRoute in Routes.TopLevel

    var showAddSheet by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
        bottomBar = {
            if (showBars) {
                StreakBottomBar(
                    current = currentRoute,
                    userName = userName,
                    onNavigate = { route -> navController.navigateTopLevel(route) },
                    onAdd = { showAddSheet = true }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onAddHabit = { navController.navigate(Routes.ADD_HABIT) },
                    onEditHabit = { id -> navController.navigate(Routes.editHabit(id)) },
                    onOpenStats = { navController.navigateTopLevel(Routes.STATS) }
                )
            }
            composable(Routes.TASKS) {
                TasksScreen(
                    onAddTask = { navController.navigate(Routes.ADD_TASK) },
                    onEditTask = { id -> navController.navigate(Routes.editTask(id)) }
                )
            }
            composable(Routes.STATS) { StatsScreen() }
            composable(Routes.PROFILE) {
                SettingsScreen(onEditHabit = { id -> navController.navigate(Routes.editHabit(id)) })
            }
            composable(Routes.ADD_HABIT) {
                AddEditHabitScreen(onClose = { navController.popBackStack() })
            }
            composable(Routes.EDIT_HABIT, arguments = listOf(navArgument("habitId") { type = NavType.StringType })) {
                AddEditHabitScreen(onClose = { navController.popBackStack() })
            }
            composable(Routes.ADD_TASK) {
                AddEditTaskScreen(onClose = { navController.popBackStack() })
            }
            composable(Routes.EDIT_TASK, arguments = listOf(navArgument("taskId") { type = NavType.StringType })) {
                AddEditTaskScreen(onClose = { navController.popBackStack() })
            }
        }
    }

    // Izbira: nova navada ali naloga
    if (showAddSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
                AddOption(stringResource(R.string.new_habit), stringResource(R.string.new_habit_desc), Icons.Outlined.Repeat, Lime) {
                    showAddSheet = false
                    navController.navigate(Routes.ADD_HABIT)
                }
                AddOption(stringResource(R.string.new_task), stringResource(R.string.new_task_desc), Icons.Outlined.Checklist, Sky) {
                    showAddSheet = false
                    navController.navigate(Routes.ADD_TASK)
                }
            }
        }
    }
}

private fun NavHostController.navigateTopLevel(route: String) {
    navigate(route) {
        popUpTo(Routes.HOME) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun AddOption(title: String, description: String, icon: ImageVector, color: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .heightIn(min = 72.dp)
            .background(color, RoundedCornerShape(20.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(16.dp)
    ) {
        Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = InkOnPastel, modifier = Modifier.size(28.dp))
        }
        Column(Modifier.padding(start = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = InkOnPastel)
            Text(description, style = MaterialTheme.typography.bodySmall, color = InkOnPastel)
        }
    }
}
