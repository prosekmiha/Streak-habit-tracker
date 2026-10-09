package com.streak.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.streak.app.R
import com.streak.app.data.local.DatabaseHealth
import com.streak.app.domain.model.TimeOfDay
import com.streak.app.domain.time.ClockStatus
import com.streak.app.ui.components.HabitGridCard
import com.streak.app.ui.components.PillButton
import com.streak.app.ui.components.WeekStrip
import com.streak.app.ui.mascot.MascotMood
import com.streak.app.ui.mascot.MascotView
import com.streak.app.ui.theme.InkOnPastel
import com.streak.app.ui.theme.Lime
import com.streak.app.ui.theme.StreakTheme
import com.streak.app.ui.theme.Sun
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun HomeScreen(
    onAddHabit: () -> Unit,
    onEditHabit: (String) -> Unit,
    onOpenStats: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pendingClock by viewModel.hasPendingClockAction.collectAsStateWithLifecycle()
    var sheetFor by rememberSaveable { mutableStateOf<String?>(null) }
    var notificationsOpen by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val futureMsg = stringResource(R.string.future_blocked)

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                HomeEvent.FutureBlocked -> snackbar.showSnackbar(futureMsg)
            }
        }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 156.dp),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize().statusBarsPadding()
        ) {
            fullSpan { Header(state, onOpenStats = onOpenStats, onOpenNotifications = { notificationsOpen = true }) }
            fullSpan {
                WeekStrip(
                    days = state.weekDays,
                    selected = state.selectedDate,
                    onSelect = viewModel::selectDate,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
            banners(state, onConfirmClock = viewModel::confirmClock)

            if (!state.isToday) fullSpan { PastDayBar(state, onBack = viewModel::backToToday) }

            if (state.loaded && !state.hasHabits) {
                fullSpan { EmptyState(onAddHabit) }
            } else {
                if (state.allDone && state.isToday) fullSpan { AllDoneBanner() }
                if (state.dueCount > 0) {
                    fullSpan {
                        Text(
                            pluralStringResource(R.plurals.home_progress, state.dueCount, state.doneCount, state.dueCount),
                            style = MaterialTheme.typography.bodyMedium,
                            color = StreakTheme.palette.inkSoft
                        )
                    }
                }
                state.sections.forEach { section ->
                    section.timeOfDay?.let { tod -> fullSpan { SectionTitle(tod) } }
                    items(section.items, key = { it.habit.id }) { item ->
                        HabitGridCard(
                            item = item,
                            onTap = { viewModel.onTap(item) },
                            onLongPress = { sheetFor = item.habit.id }
                        )
                    }
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(16.dp))
    }

    sheetFor?.let { id ->
        val item = state.item(id)
        if (item == null) {
            LaunchedEffect(id) { sheetFor = null }
        } else {
            HabitActionsSheet(
                item = item,
                date = state.selectedDate,
                isToday = state.isToday,
                onDismiss = { sheetFor = null },
                onToggle = { viewModel.onTap(item) },
                onSetStatus = { viewModel.setStatus(item.habit, it) },
                onAddProgress = { viewModel.addProgress(item.habit, it) },
                onSaveNote = { viewModel.saveNote(item.habit, it) },
                onEdit = { sheetFor = null; onEditHabit(id) },
                onArchive = { sheetFor = null; viewModel.archive(item.habit) }
            )
        }
    }

    if (pendingClock) {
        val status = state.clockStatus
        AlertDialog(
            onDismissRequest = viewModel::dismissClockDialog,
            title = { Text(stringResource(R.string.clock_dialog_title)) },
            text = { Text(clockMessage(status)) },
            confirmButton = { TextButton(onClick = viewModel::confirmClock) { Text(stringResource(R.string.clock_dialog_confirm)) } },
            dismissButton = { TextButton(onClick = viewModel::dismissClockDialog) { Text(stringResource(R.string.cancel)) } }
        )
    }

    if (notificationsOpen) {
        AlertDialog(
            onDismissRequest = { notificationsOpen = false },
            title = { Text(stringResource(R.string.inbox_title)) },
            text = { Text(stringResource(R.string.inbox_empty)) },
            confirmButton = { TextButton(onClick = { notificationsOpen = false }) { Text(stringResource(R.string.close)) } }
        )
    }
}

private fun LazyGridScope.fullSpan(content: @Composable () -> Unit) {
    item(span = { GridItemSpan(maxLineSpan) }) { content() }
}

@Composable
private fun Header(state: HomeUiState, onOpenStats: () -> Unit, onOpenNotifications: () -> Unit) {
    val greeting = stringResource(
        when (state.greeting) {
            Greeting.MORNING -> R.string.greeting_morning
            Greeting.DAY -> R.string.greeting_day
            Greeting.EVENING -> R.string.greeting_evening
        }
    )
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 8.dp)
    ) {
        // Pozdrav in ime v locenih vrsticah - ime se nikoli ne sklanja
        Column(Modifier.weight(1f).semantics(mergeDescendants = true) { heading() }) {
            Text(
                if (state.userName.isBlank()) greeting else stringResource(R.string.greeting_with_comma, greeting),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (state.userName.isNotBlank()) {
                Text(state.userName, style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.onBackground)
            }
        }
        IconButton(onClick = onOpenStats) {
            Icon(Icons.Outlined.Insights, contentDescription = stringResource(R.string.nav_stats))
        }
        IconButton(onClick = onOpenNotifications) {
            Icon(Icons.Outlined.NotificationsNone, contentDescription = stringResource(R.string.inbox_title))
        }
    }
}

private fun LazyGridScope.banners(state: HomeUiState, onConfirmClock: () -> Unit) {
    if (state.dbHealth != DatabaseHealth.Ok) {
        fullSpan {
            Banner(
                text = stringResource(
                    if (state.dbHealth is DatabaseHealth.MigrationFailed) R.string.db_migration_failed else R.string.db_corrupted
                ),
                color = MaterialTheme.colorScheme.errorContainer
            )
        }
    }
    if (state.clockStatus != ClockStatus.Ok) {
        fullSpan {
            Banner(
                text = clockMessage(state.clockStatus),
                color = Sun,
                action = stringResource(R.string.clock_dialog_confirm),
                onAction = onConfirmClock
            )
        }
    }
}

@Composable
private fun clockMessage(status: ClockStatus): String = when (status) {
    is ClockStatus.JumpedForward -> pluralStringResource(R.plurals.clock_jump_forward, status.days.toInt(), status.days.toInt())
    is ClockStatus.JumpedBackward -> pluralStringResource(R.plurals.clock_jump_backward, status.days.toInt(), status.days.toInt())
    ClockStatus.Ok -> ""
}

@Composable
private fun Banner(text: String, color: Color, action: String? = null, onAction: () -> Unit = {}) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(color, RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = InkOnPastel)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = InkOnPastel, modifier = Modifier.weight(1f).padding(horizontal = 10.dp))
        if (action != null) {
            TextButton(onClick = onAction) { Text(action, color = InkOnPastel, style = MaterialTheme.typography.labelLarge) }
        }
    }
}

@Composable
private fun PastDayBar(state: HomeUiState, onBack: () -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    val date = state.selectedDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(StreakTheme.palette.surfaceSoft, RoundedCornerShape(18.dp))
            .padding(start = 14.dp)
    ) {
        Text(
            stringResource(if (state.selectedDate.isAfter(state.today)) R.string.home_viewing_future else R.string.home_editing_past, date),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onBack) { Text(stringResource(R.string.home_back_to_today), style = MaterialTheme.typography.labelLarge) }
    }
}

@Composable
private fun SectionTitle(timeOfDay: TimeOfDay) {
    Text(
        stringResource(
            when (timeOfDay) {
                TimeOfDay.MORNING -> R.string.filter_morning
                TimeOfDay.AFTERNOON -> R.string.filter_afternoon
                TimeOfDay.EVENING -> R.string.filter_evening
                TimeOfDay.ANYTIME -> R.string.time_of_day_anytime
            }
        ),
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(top = 8.dp).semantics { heading() }
    )
}

@Composable
private fun AllDoneBanner() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(Lime, RoundedCornerShape(22.dp))
            .padding(12.dp)
    ) {
        MascotView(MascotMood.CHEER, Modifier.size(72.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(stringResource(R.string.all_done_title), style = MaterialTheme.typography.titleLarge, color = InkOnPastel)
            Text(stringResource(R.string.all_done_subtitle), style = MaterialTheme.typography.bodyMedium, color = InkOnPastel)
        }
    }
}

@Composable
private fun EmptyState(onAddHabit: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
    ) {
        MascotView(MascotMood.IDLE, Modifier.size(180.dp))
        Text(
            stringResource(R.string.empty_habits_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            stringResource(R.string.empty_habits),
            style = MaterialTheme.typography.bodyMedium,
            color = StreakTheme.palette.inkSoft,
            modifier = Modifier.padding(top = 6.dp, bottom = 20.dp)
        )
        PillButton(stringResource(R.string.add_habit), onAddHabit)
    }
}
