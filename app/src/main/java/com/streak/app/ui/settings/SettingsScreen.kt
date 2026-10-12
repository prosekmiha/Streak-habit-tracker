package com.streak.app.ui.settings

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.streak.app.BuildConfig
import com.streak.app.R
import com.streak.app.domain.model.AnimationLevel
import com.streak.app.domain.model.Habit
import com.streak.app.domain.model.ThemeMode
import com.streak.app.locale.AppLocale
import com.streak.app.ui.components.ButtonVariant
import com.streak.app.ui.components.DialogAction
import com.streak.app.ui.components.IconMap
import com.streak.app.ui.components.KpiTile
import com.streak.app.ui.components.OptionSheet
import com.streak.app.ui.components.RoundIconButton
import com.streak.app.ui.components.SegmentedControl
import com.streak.app.ui.components.SettingsGroup
import com.streak.app.ui.components.SettingsRow
import com.streak.app.ui.components.StreakButton
import com.streak.app.ui.components.StreakDialog
import com.streak.app.ui.components.StreakSheet
import com.streak.app.ui.components.StreakSwitch
import com.streak.app.ui.components.StreakTextField
import com.streak.app.ui.components.TextAction
import com.streak.app.ui.components.parseHexColor
import com.streak.app.ui.mascot.MascotMood
import com.streak.app.ui.theme.Bubblegum
import com.streak.app.ui.theme.InkOnPastel
import com.streak.app.ui.theme.Lavender
import com.streak.app.ui.theme.Lime
import com.streak.app.ui.theme.Mint
import com.streak.app.ui.theme.Peach
import com.streak.app.ui.theme.Sky
import com.streak.app.ui.theme.StreakTheme
import com.streak.app.ui.theme.Sun
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle

private enum class ProfileSheet { NAME, ANIMATIONS, LANGUAGE, FIRST_DAY, DAY_END, ARCHIVED, ABOUT }

@Composable
fun SettingsScreen(
    onEditHabit: (String) -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenData: () -> Unit,
    onOpenAchievements: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val s = state.settings
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val activity = LocalActivity.current
    val currentLanguage = remember(locale) { AppLocale.current(context) }
    val palette = StreakTheme.palette
    var sheet by remember { mutableStateOf<ProfileSheet?>(null) }
    var pendingDelete by remember { mutableStateOf<Habit?>(null) }
    val timeFormat = remember(locale) { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale) }
    var notificationsOn by remember { mutableStateOf(true) }
    LifecycleResumeEffect(Unit) {
        notificationsOn = NotificationManagerCompat.from(context).areNotificationsEnabled()
        onPauseOrDispose { }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 22.dp)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(68.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary)
            ) { Text(s.userName.ifBlank { "?" }.take(1).uppercase(locale), style = MaterialTheme.typography.headlineMedium, color = Color.White) }
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                Text(
                    s.userName.ifBlank { stringResource(R.string.profile_no_name) },
                    style = MaterialTheme.typography.headlineMedium,
                    color = palette.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() }
                )
                Text(stringResource(R.string.privacy_short), style = MaterialTheme.typography.labelMedium, color = palette.inkSoft)
            }
            RoundIconButton(Icons.Rounded.Edit, stringResource(R.string.settings_name), { sheet = ProfileSheet.NAME })
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 14.dp)) {
            KpiTile(state.topStreak.toString(), pluralStringResource(R.plurals.streak_label_days, state.topStreak), Color(0xFFFFE7D6), Modifier.weight(1f))
            KpiTile(state.totalDone.toString(), pluralStringResource(R.plurals.profile_done, state.totalDone), Color(0xFFDCF5B6), Modifier.weight(1f))
            KpiTile(state.habits.size.toString(), pluralStringResource(R.plurals.profile_habits, state.habits.size), Color(0xFFE4E5FB), Modifier.weight(1f))
        }

        SettingsGroup {
            Text(stringResource(R.string.settings_theme), style = MaterialTheme.typography.titleSmall, color = palette.ink, modifier = Modifier.padding(top = 12.dp, bottom = 8.dp))
            SegmentedControl(
                ThemeMode.entries,
                s.themeMode,
                { stringResource(themeLabel(it)) },
                viewModel::setTheme,
                container = MaterialTheme.colorScheme.background,
                height = 38.dp
            )
            Box(Modifier.heightIn(min = 10.dp))
            SettingsRow(stringResource(R.string.settings_animations), Icons.Rounded.Animation, Lavender, { sheet = ProfileSheet.ANIMATIONS }, stringResource(animationLabel(s.animationLevel)), showDivider = false)
        }

        SettingsGroup {
            SettingsRow(
                stringResource(R.string.language_title), Icons.Rounded.Translate, Mint, { sheet = ProfileSheet.LANGUAGE },
                currentLanguage?.let { AppLocale.nativeName(it) } ?: stringResource(R.string.language_system)
            )
            SettingsRow(
                stringResource(R.string.settings_first_day), Icons.Rounded.CalendarMonth, Sun, { sheet = ProfileSheet.FIRST_DAY },
                s.firstDayOfWeek.getDisplayName(TextStyle.FULL_STANDALONE, locale).replaceFirstChar { it.titlecase(locale) }
            )
            SettingsRow(
                stringResource(R.string.settings_day_ends_at), Icons.Rounded.NightsStay, Sky, { sheet = ProfileSheet.DAY_END },
                if (s.dayEndsAt == LocalTime.MIDNIGHT) stringResource(R.string.settings_midnight) else s.dayEndsAt.format(timeFormat)
            )
            SettingsRow(
                stringResource(R.string.notifications_title), Icons.Rounded.Notifications, Bubblegum, onOpenNotifications,
                stringResource(if (notificationsOn) R.string.notifications_on else R.string.notifications_off),
                showDivider = false
            )
        }

        SettingsGroup {
            SettingsRow(
                stringResource(R.string.settings_achievements), Icons.Rounded.EmojiEvents, Sun, onOpenAchievements,
                state.level?.let { stringResource(R.string.level_label, it) }
            )
            SettingsRow(stringResource(R.string.settings_archived), Icons.Rounded.Archive, Peach, { sheet = ProfileSheet.ARCHIVED }, state.archived.size.toString())
            SettingsRow(stringResource(R.string.data_title), Icons.Rounded.Download, Mint, onOpenData)
            SettingsRow(stringResource(R.string.privacy_screen), Icons.Rounded.VisibilityOff, Lavender, { viewModel.setPrivacyScreen(!s.privacyScreen) }) {
                StreakSwitch(s.privacyScreen, viewModel::setPrivacyScreen, contentDescription = stringResource(R.string.privacy_screen))
            }
            SettingsRow(stringResource(R.string.settings_hide_widget), Icons.Rounded.Widgets, Sky, { viewModel.setHideWidgetDetails(!s.hideWidgetDetails) }) {
                StreakSwitch(s.hideWidgetDetails, viewModel::setHideWidgetDetails, contentDescription = stringResource(R.string.settings_hide_widget))
            }
            SettingsRow(
                stringResource(if (state.recalculating) R.string.settings_recalculating else R.string.settings_recalculate),
                Icons.Rounded.Refresh, Lime, viewModel::recalculateAll, showDivider = false
            )
        }

        SettingsGroup {
            SettingsRow(stringResource(R.string.settings_about), Icons.Rounded.Info, Sky, { sheet = ProfileSheet.ABOUT }, BuildConfig.VERSION_NAME, showDivider = false)
        }
    }

    when (sheet) {
        ProfileSheet.NAME -> NameSheet(s.userName, onSave = { viewModel.setName(it); sheet = null }, onDismiss = { sheet = null })
        ProfileSheet.ANIMATIONS -> OptionSheet(
            title = stringResource(R.string.settings_animations),
            options = AnimationLevel.entries,
            selected = s.animationLevel,
            label = { stringResource(animationLabel(it)) },
            description = { stringResource(animationDescription(it)) },
            onSelect = viewModel::setAnimations,
            onDismiss = { sheet = null }
        )
        ProfileSheet.LANGUAGE -> OptionSheet(
            title = stringResource(R.string.language_title),
            options = listOf<String?>(null) + AppLocale.sorted(locale),
            selected = currentLanguage,
            label = { it?.let(AppLocale::nativeName) ?: stringResource(R.string.language_system) },
            // Ime v trenutnem jeziku, ce se razlikuje (npr. "Deutsch" - "nemscina")
            description = { tag ->
                tag?.let { java.util.Locale.forLanguageTag(it).getDisplayName(locale) }
                    ?.takeIf { it != AppLocale.nativeName(tag) && !it.equals(AppLocale.nativeName(tag), ignoreCase = true) }
            },
            onSelect = { tag -> activity?.let { AppLocale.set(it, tag) } },
            onDismiss = { sheet = null }
        )
        ProfileSheet.FIRST_DAY -> OptionSheet(
            title = stringResource(R.string.settings_first_day),
            options = listOf(DayOfWeek.MONDAY, DayOfWeek.SUNDAY, DayOfWeek.SATURDAY),
            selected = s.firstDayOfWeek,
            label = { it.getDisplayName(TextStyle.FULL_STANDALONE, locale).replaceFirstChar { c -> c.titlecase(locale) } },
            onSelect = viewModel::setFirstDayOfWeek,
            onDismiss = { sheet = null }
        )
        ProfileSheet.DAY_END -> OptionSheet(
            title = stringResource(R.string.settings_day_ends_at),
            options = (0..6).map { LocalTime.of(it, 0) },
            selected = s.dayEndsAt,
            label = { if (it == LocalTime.MIDNIGHT) stringResource(R.string.settings_midnight) else it.format(timeFormat) },
            description = { if (it == LocalTime.MIDNIGHT) stringResource(R.string.settings_day_ends_at_desc) else null },
            onSelect = viewModel::setDayEndsAt,
            onDismiss = { sheet = null }
        )
        ProfileSheet.ARCHIVED -> ArchivedSheet(
            habits = state.archived,
            onRestore = viewModel::unarchive,
            onOpen = { sheet = null; onEditHabit(it.id) },
            onDelete = { pendingDelete = it },
            onDismiss = { sheet = null }
        )
        ProfileSheet.ABOUT -> StreakSheet(onDismiss = { sheet = null }, title = stringResource(R.string.settings_about), showClose = true) {
            Text(stringResource(R.string.privacy_long), style = MaterialTheme.typography.bodyLarge, color = palette.ink)
            Text(stringResource(R.string.health_disclaimer), style = MaterialTheme.typography.bodyMedium, color = palette.inkSoft, modifier = Modifier.padding(top = 12.dp))
            Text(stringResource(R.string.licenses_fonts), style = MaterialTheme.typography.bodyMedium, color = palette.inkSoft, modifier = Modifier.padding(top = 12.dp))
            Text(stringResource(R.string.app_version, BuildConfig.VERSION_NAME), style = MaterialTheme.typography.bodyMedium, color = palette.inkSoft, modifier = Modifier.padding(top = 12.dp))
        }
        null -> Unit
    }

    pendingDelete?.let { habit ->
        StreakDialog(
            onDismiss = { pendingDelete = null },
            title = stringResource(R.string.delete_habit_title_named, habit.title),
            text = stringResource(R.string.delete_habit_text_short),
            mascot = MascotMood.SAD
        ) {
            DialogAction(stringResource(R.string.delete_forever), ButtonVariant.Danger) { viewModel.deleteHabit(habit); pendingDelete = null }
            DialogAction(stringResource(R.string.cancel), ButtonVariant.Secondary) { pendingDelete = null }
        }
    }
}

@Composable
private fun NameSheet(current: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(current) }
    StreakSheet(onDismiss = onDismiss, title = stringResource(R.string.settings_name)) {
        StreakTextField(value = name, onValueChange = { name = it.take(40) }, placeholder = stringResource(R.string.onboarding_name_label))
        StreakButton(stringResource(R.string.save), { onSave(name) }, Modifier.padding(top = 14.dp))
    }
}

@Composable
private fun ArchivedSheet(habits: List<Habit>, onRestore: (Habit) -> Unit, onOpen: (Habit) -> Unit, onDelete: (Habit) -> Unit, onDismiss: () -> Unit) {
    val palette = StreakTheme.palette
    StreakSheet(onDismiss = onDismiss, title = stringResource(R.string.settings_archived), showClose = true) {
        if (habits.isEmpty()) Text(stringResource(R.string.archived_empty), style = MaterialTheme.typography.bodyLarge, color = palette.inkSoft)
        habits.forEach { habit ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .heightIn(min = 60.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(palette.surfaceSoft)
                    .padding(start = 10.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(parseHexColor(habit.colorHex))
                ) { androidx.compose.material3.Icon(IconMap.resolve(habit.icon), null, tint = InkOnPastel, modifier = Modifier.size(20.dp)) }
                Text(habit.title, style = MaterialTheme.typography.titleSmall, color = palette.ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(horizontal = 10.dp))
                TextAction(stringResource(R.string.action_unarchive), { onRestore(habit) })
                TextAction(stringResource(R.string.delete), { onDelete(habit) }, color = palette.danger)
            }
        }
    }
}

private fun themeLabel(mode: ThemeMode) = when (mode) {
    ThemeMode.SYSTEM -> R.string.theme_system
    ThemeMode.LIGHT -> R.string.theme_light
    ThemeMode.DARK -> R.string.theme_dark
}

private fun animationLabel(level: AnimationLevel) = when (level) {
    AnimationLevel.FULL -> R.string.animations_full
    AnimationLevel.REDUCED -> R.string.animations_reduced
    AnimationLevel.OFF -> R.string.animations_off
}

private fun animationDescription(level: AnimationLevel) = when (level) {
    AnimationLevel.FULL -> R.string.animations_full_desc
    AnimationLevel.REDUCED -> R.string.animations_reduced_desc
    AnimationLevel.OFF -> R.string.animations_off_desc
}
