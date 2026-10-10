package com.streak.app.ui.settings

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AlarmOn
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.FormatListNumbered
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings as SettingsIcon
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.streak.app.R
import com.streak.app.ui.components.OptionSheet
import com.streak.app.ui.components.ScreenTopBar
import com.streak.app.ui.components.SettingsGroup
import com.streak.app.ui.components.SettingsRow
import com.streak.app.ui.components.StreakSwitch
import com.streak.app.ui.components.TimePickerSheet
import com.streak.app.ui.components.TimePreset
import com.streak.app.ui.theme.Bubblegum
import com.streak.app.ui.theme.Lavender
import com.streak.app.ui.theme.Lime
import com.streak.app.ui.theme.Mint
import com.streak.app.ui.theme.Peach
import com.streak.app.ui.theme.Sky
import com.streak.app.ui.theme.StreakTheme
import com.streak.app.ui.theme.Sun
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private enum class TimeTarget { BRIEFING, NUDGE, QUIET_START, QUIET_END }

// Obvestila: stanje dovoljenj, dnevni povzetek, vecerni opomnik, tihe ure, omejitev, zasebnost
@Composable
fun NotificationSettingsScreen(onBack: () -> Unit, viewModel: NotificationSettingsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val s = state.settings
    val sys = state.system
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val timeFormat = remember(locale) { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale) }
    var pickTime by remember { mutableStateOf<TimeTarget?>(null) }
    var pickCap by remember { mutableStateOf(false) }

    LifecycleResumeEffect(Unit) {
        viewModel.refreshSystem()
        onPauseOrDispose { }
    }

    fun openAppNotificationSettings() {
        context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        // Ce je bilo dovoljenje ze trajno zavrnjeno, ga lahko vklopi le v sistemskih nastavitvah
        if (!granted) openAppNotificationSettings()
        viewModel.refreshSystem()
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp)
    ) {
        ScreenTopBar(stringResource(R.string.notifications_title), Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back), onBack)

        // Sistemska dovoljenja
        SettingsGroup {
            SettingsRow(
                stringResource(R.string.notif_settings_permission), Icons.Rounded.Notifications, Bubblegum,
                {
                    if (!sys.notificationsEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        openAppNotificationSettings()
                    }
                },
                stringResource(if (sys.notificationsEnabled) R.string.notifications_on else R.string.notifications_off)
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                SettingsRow(
                    stringResource(R.string.notif_settings_exact), Icons.Rounded.AlarmOn, Sun,
                    {
                        context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, "package:${context.packageName}".toUri()))
                    },
                    stringResource(if (sys.exactAlarms) R.string.notif_settings_allowed else R.string.notif_settings_not_allowed)
                )
            }
            SettingsRow(
                stringResource(R.string.notif_settings_battery), Icons.Rounded.BatteryChargingFull, Lime,
                { runCatching { context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) } },
                stringResource(if (sys.batteryUnrestricted) R.string.notif_settings_unrestricted else R.string.notif_settings_optimized),
                showDivider = false
            )
        }
        val hint = when {
            !sys.notificationsEnabled -> R.string.notif_settings_hint_off
            !sys.exactAlarms -> R.string.notif_settings_hint_exact
            !sys.batteryUnrestricted -> R.string.notif_settings_hint_battery
            else -> null
        }
        hint?.let { Hint(stringResource(it)) }

        // Dnevna obvestila
        SettingsGroup {
            ToggleRow(stringResource(R.string.notif_settings_briefing), Icons.Rounded.WbSunny, Sun, s.briefingEnabled, viewModel::setBriefing)
            if (s.briefingEnabled) TimeRow(s.briefingTime.format(timeFormat), Sun) { pickTime = TimeTarget.BRIEFING }
            ToggleRow(stringResource(R.string.notif_settings_nudge), Icons.Rounded.Bedtime, Lavender, s.nudgeEnabled, viewModel::setNudge, showDivider = s.nudgeEnabled)
            if (s.nudgeEnabled) TimeRow(s.nudgeTime.format(timeFormat), Lavender, showDivider = false) { pickTime = TimeTarget.NUDGE }
        }
        Hint(stringResource(R.string.notif_settings_daily_hint))

        // Tihe ure in omejitev
        val quietOn = s.quietHoursStart != null && s.quietHoursEnd != null
        SettingsGroup {
            ToggleRow(stringResource(R.string.notif_settings_quiet), Icons.Rounded.DoNotDisturbOn, Sky, quietOn, viewModel::setQuietHours)
            if (quietOn) {
                SettingsRow(stringResource(R.string.notif_settings_from), Icons.Rounded.Schedule, Sky, { pickTime = TimeTarget.QUIET_START }, s.quietHoursStart?.format(timeFormat))
                SettingsRow(stringResource(R.string.notif_settings_to), Icons.Rounded.Schedule, Sky, { pickTime = TimeTarget.QUIET_END }, s.quietHoursEnd?.format(timeFormat))
            }
            SettingsRow(
                stringResource(R.string.notif_settings_cap), Icons.Rounded.FormatListNumbered, Peach, { pickCap = true },
                capLabel(s.dailyNotificationCap), showDivider = false
            )
        }
        Hint(stringResource(R.string.notif_settings_quiet_hint))

        // Zasebnost in sistemske nastavitve kanalov
        SettingsGroup {
            ToggleRow(stringResource(R.string.notif_settings_hide_names), Icons.Rounded.VisibilityOff, Mint, s.hideNamesInNotifications, viewModel::setHideNames)
            SettingsRow(stringResource(R.string.notif_settings_system), Icons.Rounded.SettingsIcon, Lavender, ::openAppNotificationSettings, showDivider = false)
        }
        Hint(stringResource(R.string.notif_settings_privacy_hint))
    }

    pickTime?.let { target ->
        val initial = when (target) {
            TimeTarget.BRIEFING -> s.briefingTime
            TimeTarget.NUDGE -> s.nudgeTime
            TimeTarget.QUIET_START -> s.quietHoursStart ?: LocalTime.of(22, 0)
            TimeTarget.QUIET_END -> s.quietHoursEnd ?: LocalTime.of(7, 0)
        }
        TimePickerSheet(
            title = stringResource(
                when (target) {
                    TimeTarget.BRIEFING -> R.string.notif_settings_briefing
                    TimeTarget.NUDGE -> R.string.notif_settings_nudge
                    TimeTarget.QUIET_START -> R.string.notif_settings_quiet_from
                    TimeTarget.QUIET_END -> R.string.notif_settings_quiet_to
                }
            ),
            initial = initial,
            presets = presetsFor(target),
            confirmText = stringResource(R.string.save),
            onDismiss = { pickTime = null },
            onConfirm = { time ->
                when (target) {
                    TimeTarget.BRIEFING -> viewModel.setBriefingTime(time)
                    TimeTarget.NUDGE -> viewModel.setNudgeTime(time)
                    TimeTarget.QUIET_START -> viewModel.setQuietStart(time)
                    TimeTarget.QUIET_END -> viewModel.setQuietEnd(time)
                }
                pickTime = null
            }
        )
    }

    if (pickCap) {
        OptionSheet(
            title = stringResource(R.string.notif_settings_cap),
            options = NotificationSettingsViewModel.CAP_OPTIONS,
            selected = s.dailyNotificationCap,
            label = { capLabel(it) },
            onSelect = viewModel::setCap,
            onDismiss = { pickCap = false },
            description = { if (it == 0) null else stringResource(R.string.notif_settings_cap_desc) }
        )
    }
}

@Composable
private fun ToggleRow(title: String, icon: ImageVector, iconBg: Color, checked: Boolean, onChange: (Boolean) -> Unit, showDivider: Boolean = true) {
    SettingsRow(title, icon, iconBg, { onChange(!checked) }, showDivider = showDivider) {
        StreakSwitch(checked, onChange, contentDescription = title)
    }
}

@Composable
private fun TimeRow(value: String, iconBg: Color, showDivider: Boolean = true, onClick: () -> Unit) {
    SettingsRow(stringResource(R.string.notif_settings_time), Icons.Rounded.Schedule, iconBg, onClick, value, showDivider = showDivider)
}

@Composable
private fun Hint(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = StreakTheme.palette.inkSoft,
        modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 8.dp)
    )
}

@Composable
private fun capLabel(cap: Int): String =
    if (cap == 0) stringResource(R.string.notif_settings_cap_none) else stringResource(R.string.notif_settings_cap_value, cap)

@Composable
private fun presetsFor(target: TimeTarget): List<TimePreset> = when (target) {
    TimeTarget.BRIEFING -> listOf(7, 8, 9).map { TimePreset(LocalTime.of(it, 0), "") }
    TimeTarget.NUDGE -> listOf(19, 20, 21).map { TimePreset(LocalTime.of(it, 0), "") }
    TimeTarget.QUIET_START -> listOf(21, 22, 23).map { TimePreset(LocalTime.of(it, 0), "") }
    TimeTarget.QUIET_END -> listOf(6, 7, 8).map { TimePreset(LocalTime.of(it, 0), "") }
}
