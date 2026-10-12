package com.streak.app.ui.home

import androidx.activity.compose.ReportDrawnWhen
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.streak.app.R
import com.streak.app.ui.achievements.GameCelebrationOverlay
import com.streak.app.ui.achievements.titleRes
import com.streak.app.ui.components.ButtonVariant
import com.streak.app.ui.components.DialogAction
import com.streak.app.ui.components.HabitGridCard
import com.streak.app.ui.components.ScreenReadyTrace
import com.streak.app.ui.components.StreakDialog
import com.streak.app.ui.components.WeekStrip
import com.streak.app.ui.components.bleed
import com.streak.app.ui.mascot.MascotMood
import com.streak.app.ui.mood.MoodCard
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun HomeScreen(
    onAddHabit: () -> Unit,
    onEditHabit: (String) -> Unit,
    onOpenHabit: (String) -> Unit,
    onOpenSession: (String) -> Unit,
    onOpenStats: () -> Unit,
    onOpenData: () -> Unit,
    onOpenAchievements: () -> Unit,
    onLogMood: (Int) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
    gameViewModel: HomeGameViewModel = hiltViewModel()
) {
    val gameCelebration by gameViewModel.celebration.collectAsStateWithLifecycle()
    val inbox by gameViewModel.inbox.collectAsStateWithLifecycle()
    val level by gameViewModel.level.collectAsStateWithLifecycle()
    val resources = LocalResources.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    ScreenReadyTrace("Home.ready", state.loaded)
    // Zagon je "v celoti izrisan", ko so navade nalozene (TTFD v meritvah zagona)
    ReportDrawnWhen { state.loaded }
    val pendingClock by viewModel.hasPendingClockAction.collectAsStateWithLifecycle()
    val activeSession by viewModel.activeSession.collectAsStateWithLifecycle()
    val restoreOffer by viewModel.restoreOffer.collectAsStateWithLifecycle()
    var sheetFor by rememberSaveable { mutableStateOf<String?>(null) }
    var inboxOpen by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val futureMsg = stringResource(R.string.future_blocked)
    val longPressMsg = stringResource(R.string.long_press_to_uncheck)

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                HomeEvent.FutureBlocked -> snackbar.showSnackbar(futureMsg)
                HomeEvent.NeedsLongPress -> snackbar.showSnackbar(longPressMsg)
                is HomeEvent.OpenSession -> onOpenSession(event.habitId)
            }
        }
    }
    LaunchedEffect(Unit) {
        gameViewModel.challengeDone.collect { challenge ->
            snackbar.showSnackbar(resources.getString(R.string.challenge_done_snackbar, resources.getString(challenge.titleRes())))
        }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 156.dp),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize().statusBarsPadding()
        ) {
            fullSpan { Header(state, unread = inbox?.unread ?: 0, onOpenStats = onOpenStats, onOpenInbox = { inboxOpen = true }) }
            fullSpan {
                // Trak sega cez rob (odmik je v traku samem)
                WeekStrip(
                    days = state.weekDays,
                    selected = state.selectedDate,
                    onSelect = viewModel::selectDate,
                    modifier = Modifier.padding(top = 6.dp).bleed(20.dp)
                )
            }
            activeSession?.let { bar -> fullSpan { ActiveSessionBanner(bar) { onOpenSession(bar.habitId) } } }
            banners(state, onConfirmClock = viewModel::confirmClock)
            if (state.backupDue && state.isToday) fullSpan { BackupBanner(onExport = onOpenData, onLater = viewModel::snoozeBackupReminder) }
            if (!state.isToday) fullSpan { PastDayBar(state, onBack = viewModel::backToToday) }

            if (state.loaded && !state.hasHabits) {
                fullSpan { EmptyState(onAddHabit) }
            } else {
                if (state.dueCount > 0) fullSpan { ProgressRow(state, level, onOpenAchievements) }
                if (state.allDone && state.isToday) fullSpan { AllDoneBanner(state) }
                if (state.moodPrompt && state.isToday) fullSpan { MoodCard(onPick = { onLogMood(it) }, onLater = viewModel::dismissMoodPrompt) }
                state.sections.forEach { section ->
                    section.timeOfDay?.let { tod -> fullSpan { SectionTitle(tod) } }
                    items(section.items, key = { it.habit.id }) { item ->
                        HabitGridCard(
                            item = item,
                            // Pri opuscanju tap odpre dejanja, da zdrs ni vpisan po nesreci
                            onTap = { if (item.habit.isQuit) sheetFor = item.habit.id else viewModel.onTap(item) },
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
                onToggle = { viewModel.toggleDone(item) },
                onSetStatus = { viewModel.setStatus(item.habit, it) },
                onAddProgress = { viewModel.addProgress(item.habit, it) },
                onAddRelapse = { viewModel.addRelapse(item.habit, it) },
                onSaveNote = { viewModel.saveNote(item.habit, it) },
                onStartSession = { sheetFor = null; onOpenSession(id) },
                onDetails = { sheetFor = null; onOpenHabit(id) },
                onEdit = { sheetFor = null; onEditHabit(id) },
                onArchive = { sheetFor = null; viewModel.archive(item.habit) }
            )
        }
    }

    if (pendingClock) {
        StreakDialog(
            onDismiss = viewModel::dismissClockDialog,
            title = stringResource(R.string.clock_dialog_title),
            text = clockMessage(state.clockStatus),
            mascot = MascotMood.THINKING
        ) {
            DialogAction(stringResource(R.string.clock_dialog_confirm), ButtonVariant.Dark, viewModel::confirmClock)
            DialogAction(stringResource(R.string.cancel), ButtonVariant.Secondary, viewModel::dismissClockDialog)
        }
    }

    if (inboxOpen) {
        InboxSheet(
            memory = inbox,
            onOpenAchievements = { inboxOpen = false; gameViewModel.markInboxRead(); onOpenAchievements() },
            onDismiss = { inboxOpen = false; gameViewModel.markInboxRead() }
        )
    }

    restoreOffer?.let { offer ->
        val locale = LocalConfiguration.current.locales[0]
        val date = offer.createdAt?.atZone(java.time.ZoneId.systemDefault())?.toLocalDate()
            ?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)).orEmpty()
        StreakDialog(
            onDismiss = {},
            title = stringResource(R.string.restore_offer_title),
            text = pluralStringResource(R.plurals.restore_offer_text, offer.habits, date, offer.habits),
            mascot = MascotMood.WAVE
        ) {
            DialogAction(stringResource(R.string.restore_offer_accept), ButtonVariant.Dark, viewModel::acceptRestore)
            DialogAction(stringResource(R.string.restore_offer_decline), ButtonVariant.Secondary, viewModel::declineRestore)
        }
    }

    // Najprej mejnik verige, nato znacke in nivo (nikoli dve praznovanji hkrati)
    val milestone = state.celebration
    if (milestone != null) {
        CelebrationOverlay(celebration = milestone, onDismiss = viewModel::dismissCelebration)
    } else {
        gameCelebration?.let { GameCelebrationOverlay(it, onDismiss = gameViewModel::dismissCelebration) }
    }
}
