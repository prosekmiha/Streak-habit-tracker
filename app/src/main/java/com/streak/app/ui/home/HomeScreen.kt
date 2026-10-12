package com.streak.app.ui.home

import com.streak.app.ui.theme.Sky
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.foundation.background
import com.streak.app.session.SessionNotifier
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.activity.compose.ReportDrawnWhen
import com.streak.app.ui.components.ScreenReadyTrace
import com.streak.app.ui.achievements.titleRes
import com.streak.app.ui.achievements.GameCelebrationOverlay
import com.streak.app.ui.theme.Lavender
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.material.icons.rounded.Star
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.platform.LocalResources
import com.streak.app.data.local.DatabaseHealth
import com.streak.app.domain.model.TimeOfDay
import com.streak.app.domain.time.ClockStatus
import com.streak.app.ui.components.ButtonVariant
import com.streak.app.ui.components.DialogAction
import com.streak.app.ui.components.HabitGridCard
import com.streak.app.ui.components.ProgressTrack
import com.streak.app.ui.components.RoundIconButton
import com.streak.app.ui.components.StreakButton
import com.streak.app.ui.components.StreakDialog
import com.streak.app.ui.components.TextAction
import com.streak.app.ui.components.WeekStrip
import com.streak.app.ui.components.bleed
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
    onOpenHabit: (String) -> Unit,
    onOpenSession: (String) -> Unit,
    onOpenStats: () -> Unit,
    onOpenData: () -> Unit,
    onOpenAchievements: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val gameCelebration by viewModel.gameCelebration.collectAsStateWithLifecycle()
    val inbox by viewModel.inbox.collectAsStateWithLifecycle()
    val level by viewModel.level.collectAsStateWithLifecycle()
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

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                HomeEvent.FutureBlocked -> snackbar.showSnackbar(futureMsg)
                is HomeEvent.OpenSession -> onOpenSession(event.habitId)
                is HomeEvent.ChallengeDone -> snackbar.showSnackbar(
                    resources.getString(R.string.challenge_done_snackbar, resources.getString(event.challenge.titleRes()))
                )
            }
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
                onToggle = { viewModel.toggleDone(item) },
                onSetStatus = { viewModel.setStatus(item.habit, it) },
                onAddProgress = { viewModel.addProgress(item.habit, it) },
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
            onOpenAchievements = { inboxOpen = false; viewModel.markInboxRead(); onOpenAchievements() },
            onDismiss = { inboxOpen = false; viewModel.markInboxRead() }
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
        gameCelebration?.let { GameCelebrationOverlay(it, onDismiss = viewModel::dismissGameCelebration) }
    }
}

// Seja tece v ozadju: hiter povratek na zaslon seje
@Composable
private fun ActiveSessionBanner(bar: ActiveSessionBar, onOpen: () -> Unit) {
    val text = stringResource(
        if (bar.running) R.string.home_session_running else R.string.home_session_paused,
        bar.title
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Lime)
            .clickable(role = Role.Button, onClick = onOpen)
            .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
    ) {
        Icon(
            if (bar.running) Icons.Rounded.SelfImprovement else Icons.Rounded.Pause,
            contentDescription = null,
            tint = InkOnPastel,
            modifier = Modifier.size(22.dp)
        )
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(text, style = MaterialTheme.typography.titleSmall, color = InkOnPastel, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(SessionNotifier.formatClock(bar.elapsedSeconds), style = MaterialTheme.typography.labelLarge, color = InkOnPastel.copy(alpha = .75f))
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(InkOnPastel)
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(stringResource(R.string.home_session_open), style = MaterialTheme.typography.labelLarge, color = Lime)
        }
    }
}

// Opomnik za varnostno kopijo (vsakih 30 dni)
@Composable
private fun BackupBanner(onExport: () -> Unit, onLater: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Sky)
            .padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 10.dp)
    ) {
        Icon(Icons.Rounded.CloudUpload, contentDescription = null, tint = InkOnPastel)
        Text(stringResource(R.string.backup_banner), style = MaterialTheme.typography.bodyMedium, color = InkOnPastel, modifier = Modifier.weight(1f).padding(horizontal = 10.dp))
        TextAction(stringResource(R.string.later), onLater, color = InkOnPastel.copy(alpha = .7f))
        TextAction(stringResource(R.string.backup_banner_action), onExport, color = InkOnPastel)
    }
}

private fun LazyGridScope.fullSpan(content: @Composable () -> Unit) {
    item(span = { GridItemSpan(maxLineSpan) }) { content() }
}

@Composable
private fun Header(state: HomeUiState, unread: Int, onOpenStats: () -> Unit, onOpenInbox: () -> Unit) {
    val greeting = stringResource(
        when (state.greeting) {
            Greeting.MORNING -> R.string.greeting_morning
            Greeting.DAY -> R.string.greeting_day
            Greeting.EVENING -> R.string.greeting_evening
        }
    )
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth().padding(top = 20.dp)) {
        // Pozdrav in ime v locenih vrsticah - ime se nikoli ne sklanja
        Column(Modifier.weight(1f).semantics(mergeDescendants = true) { heading() }) {
            Text(
                if (state.userName.isBlank()) greeting else stringResource(R.string.greeting_with_comma, greeting),
                style = MaterialTheme.typography.headlineSmall,
                color = StreakTheme.palette.ink
            )
            if (state.userName.isNotBlank()) {
                Text(state.userName, style = MaterialTheme.typography.displayMedium, color = StreakTheme.palette.ink)
            }
        }
        RoundIconButton(Icons.Rounded.AutoGraph, stringResource(R.string.nav_stats), onOpenStats, container = Color.Transparent)
        Box {
            RoundIconButton(
                Icons.Rounded.NotificationsNone,
                if (unread > 0) pluralStringResource(R.plurals.inbox_unread_a11y, unread, unread) else stringResource(R.string.inbox_title),
                onOpenInbox,
                container = Color.Transparent
            )
            if (unread > 0) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 8.dp)
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

@Composable
private fun ProgressRow(state: HomeUiState, level: Int?, onOpenAchievements: () -> Unit) {
    val palette = StreakTheme.palette
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
        ProgressTrack(
            progress = if (state.dueCount == 0) 0f else state.doneCount.toFloat() / state.dueCount,
            modifier = Modifier.weight(1f)
        )
        Text(
            stringResource(R.string.home_progress_short, state.doneCount, state.dueCount),
            style = MaterialTheme.typography.labelLarge,
            color = palette.ink,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 10.dp)
        )
        if (state.topStreak > 0) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFFFE7D6))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Rounded.LocalFireDepartment, null, tint = Color(0xFFF4874B), modifier = Modifier.size(16.dp))
                Text(
                    pluralStringResource(R.plurals.days_short, state.topStreak, state.topStreak),
                    style = MaterialTheme.typography.labelLarge,
                    color = InkOnPastel,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
        if (level != null) {
            val description = stringResource(R.string.level_chip_a11y, level)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(start = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Lavender)
                    .clickable(role = Role.Button, onClick = onOpenAchievements)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .clearAndSetSemantics { contentDescription = description }
            ) {
                Icon(Icons.Rounded.Star, null, tint = InkOnPastel, modifier = Modifier.size(16.dp))
                Text(
                    stringResource(R.string.level_short, level),
                    style = MaterialTheme.typography.labelLarge,
                    color = InkOnPastel,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}

private fun LazyGridScope.banners(state: HomeUiState, onConfirmClock: () -> Unit) {
    if (state.dbHealth != DatabaseHealth.Ok) {
        fullSpan {
            Banner(
                text = stringResource(if (state.dbHealth is DatabaseHealth.MigrationFailed) R.string.db_migration_failed else R.string.db_corrupted),
                color = Color(0xFFFFE3EC)
            )
        }
    }
    if (state.clockStatus != ClockStatus.Ok) {
        fullSpan { Banner(clockMessage(state.clockStatus), Sun, stringResource(R.string.clock_dialog_confirm), onConfirmClock) }
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
            .clip(RoundedCornerShape(20.dp))
            .background(color)
            .padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 10.dp)
    ) {
        Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = InkOnPastel)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = InkOnPastel, modifier = Modifier.weight(1f).padding(horizontal = 10.dp))
        if (action != null) TextAction(action, onAction, color = InkOnPastel)
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
            .clip(RoundedCornerShape(20.dp))
            .background(StreakTheme.palette.surfaceSoft)
            .padding(start = 14.dp)
    ) {
        Text(
            stringResource(if (state.selectedDate.isAfter(state.today)) R.string.home_viewing_future else R.string.home_editing_past, date),
            style = MaterialTheme.typography.titleSmall,
            color = StreakTheme.palette.ink,
            modifier = Modifier.weight(1f)
        )
        TextAction(stringResource(R.string.home_back_to_today), onBack)
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
        color = StreakTheme.palette.ink,
        modifier = Modifier.padding(top = 8.dp).semantics { heading() }
    )
}

// Zelena pasica z maskoto, ki proslavlja
@Composable
private fun AllDoneBanner(state: HomeUiState) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Lime)
    ) {
        Column(Modifier.align(Alignment.CenterStart).padding(start = 20.dp).fillMaxWidth(0.58f)) {
            Text(stringResource(R.string.all_done_title), style = MaterialTheme.typography.headlineSmall, color = InkOnPastel)
            Text(
                // Vsako stevilo ima svojo mnozinsko obliko (1 dan, 2 dneva, 5 dni ...)
                pluralStringResource(R.plurals.all_done_detail, state.doneCount, state.doneCount) + " · " +
                    state.topStreak + " " + pluralStringResource(R.plurals.streak_label_days, state.topStreak),
                style = MaterialTheme.typography.titleSmall,
                color = InkOnPastel,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
        MascotView(
            MascotMood.CHEER,
            Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 6.dp, y = 26.dp)
                .size(width = 150.dp, height = 162.dp)
        )
    }
}

@Composable
private fun EmptyState(onAddHabit: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(top = 28.dp)) {
        MascotView(MascotMood.IDLE, Modifier.size(width = 170.dp, height = 184.dp))
        Text(stringResource(R.string.empty_habits_title), style = MaterialTheme.typography.headlineSmall, color = StreakTheme.palette.ink, modifier = Modifier.padding(top = 16.dp))
        Text(
            stringResource(R.string.empty_habits),
            style = MaterialTheme.typography.bodyLarge,
            color = StreakTheme.palette.inkSoft,
            modifier = Modifier.padding(top = 6.dp, bottom = 20.dp)
        )
        StreakButton(stringResource(R.string.add_habit), onAddHabit)
        Spacer(Modifier.height(8.dp))
    }
}
