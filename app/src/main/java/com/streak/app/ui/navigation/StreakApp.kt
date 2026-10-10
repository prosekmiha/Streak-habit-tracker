package com.streak.app.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.streak.app.ui.addhabit.AddEditHabitScreen
import com.streak.app.ui.addtask.AddEditTaskScreen
import com.streak.app.ui.detail.HabitDetailScreen
import com.streak.app.ui.home.HomeScreen
import com.streak.app.ui.session.SessionScreen
import com.streak.app.ui.settings.SettingsScreen
import com.streak.app.ui.stats.StatsScreen
import com.streak.app.ui.tasks.TasksScreen

@Composable
fun StreakApp(userName: String, pendingRoute: String? = null, onRouteHandled: () -> Unit = {}) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route ?: Routes.HOME
    val showBars = currentRoute in Routes.TopLevel
    var showAddSheet by rememberSaveable { mutableStateOf(false) }

    // Obvestilo seje ali bliznjica: odpri cilj (ce ni ze odprt)
    LaunchedEffect(pendingRoute) {
        val route = pendingRoute ?: return@LaunchedEffect
        val entry = navController.currentBackStackEntry
        val alreadyOpen = entry?.destination?.route == Routes.SESSION &&
            route == Routes.session(entry.arguments?.getString("habitId").orEmpty())
        if (!alreadyOpen) navController.navigate(route)
        onRouteHandled()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
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
                    onAddHabit = { navController.navigate(Routes.addHabit()) },
                    onEditHabit = { id -> navController.navigate(Routes.editHabit(id)) },
                    onOpenHabit = { id -> navController.navigate(Routes.habitDetail(id)) },
                    onOpenSession = { id -> navController.navigate(Routes.session(id)) },
                    onOpenStats = { navController.navigateTopLevel(Routes.STATS) }
                )
            }
            composable(Routes.TASKS) {
                TasksScreen(
                    onAddTask = { navController.navigate(Routes.ADD_TASK) },
                    onEditTask = { id -> navController.navigate(Routes.editTask(id)) }
                )
            }
            composable(Routes.STATS) {
                StatsScreen(onOpenHabit = { id -> navController.navigate(Routes.habitDetail(id)) })
            }
            composable(Routes.PROFILE) {
                SettingsScreen(onEditHabit = { id -> navController.navigate(Routes.editHabit(id)) })
            }
            composable(
                Routes.ADD_HABIT,
                arguments = listOf(navArgument("template") { type = NavType.StringType; nullable = true; defaultValue = null })
            ) {
                AddEditHabitScreen(onClose = { navController.popBackStack() })
            }
            composable(Routes.EDIT_HABIT, arguments = listOf(navArgument("habitId") { type = NavType.StringType })) {
                AddEditHabitScreen(onClose = { navController.popBackStack() })
            }
            composable(Routes.HABIT_DETAIL, arguments = listOf(navArgument("habitId") { type = NavType.StringType })) {
                HabitDetailScreen(
                    onBack = { navController.popBackStack() },
                    onEdit = { id -> navController.navigate(Routes.editHabit(id)) }
                )
            }
            composable(Routes.SESSION, arguments = listOf(navArgument("habitId") { type = NavType.StringType })) {
                SessionScreen(
                    onClose = { navController.popBackStack() },
                    onOpenSession = { id ->
                        navController.navigate(Routes.session(id)) { popUpTo(Routes.SESSION) { inclusive = true } }
                    }
                )
            }
            composable(Routes.ADD_TASK) {
                AddEditTaskScreen(onClose = { navController.popBackStack() })
            }
            composable(Routes.EDIT_TASK, arguments = listOf(navArgument("taskId") { type = NavType.StringType })) {
                AddEditTaskScreen(onClose = { navController.popBackStack() })
            }
        }
    }

    if (showAddSheet) {
        AddSheet(
            onDismiss = { showAddSheet = false },
            onNewHabit = { showAddSheet = false; navController.navigate(Routes.addHabit()) },
            onNewTask = { showAddSheet = false; navController.navigate(Routes.ADD_TASK) },
            onTemplate = { id -> showAddSheet = false; navController.navigate(Routes.addHabit(id)) }
        )
    }
}

private fun NavHostController.navigateTopLevel(route: String) {
    navigate(route) {
        popUpTo(Routes.HOME) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
