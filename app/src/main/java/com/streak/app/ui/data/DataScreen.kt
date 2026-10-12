package com.streak.app.ui.data

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.streak.app.R
import com.streak.app.domain.backup.BackupSummary
import com.streak.app.domain.backup.ImportMode
import com.streak.app.ui.components.ButtonVariant
import com.streak.app.ui.components.DialogAction
import com.streak.app.ui.components.ScreenTopBar
import com.streak.app.ui.components.SettingsGroup
import com.streak.app.ui.components.SettingsRow
import com.streak.app.ui.components.StreakButton
import com.streak.app.ui.components.StreakDialog
import com.streak.app.ui.components.StreakSheet
import com.streak.app.ui.components.StreakSwitch
import com.streak.app.ui.components.StreakTextField
import com.streak.app.ui.mascot.MascotMood
import com.streak.app.ui.theme.InkOnPastel
import com.streak.app.ui.theme.Lavender
import com.streak.app.ui.theme.Lime
import com.streak.app.ui.theme.Mint
import com.streak.app.ui.theme.Peach
import com.streak.app.ui.theme.Sky
import com.streak.app.ui.theme.StreakTheme
import com.streak.app.ui.theme.Sun
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

// Podatki: varnostna kopija (izvoz z geslom, obnova s predogledom), CSV, Androidova
// varnostna kopija in brisanje vseh podatkov
@Composable
fun DataScreen(onBack: () -> Unit, viewModel: DataViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val s = state.settings
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var exportSheet by remember { mutableStateOf(false) }
    var confirmWipe by remember { mutableStateOf(false) }

    val saveJson = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { viewModel.writeTo(it) }
    val saveCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { viewModel.writeTo(it) }
    val openFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { viewModel.readImport(it) }

    // Pripravljen izvoz: izbira mesta ali deljenje
    LaunchedEffect(state.pending) {
        val export = state.pending ?: return@LaunchedEffect
        when (export.target) {
            ExportTarget.SAVE -> (if (export.mime == "text/csv") saveCsv else saveJson).launch(export.fileName)
            ExportTarget.SHARE -> {
                val send = Intent(Intent.ACTION_SEND)
                    .setType(export.mime)
                    .putExtra(Intent.EXTRA_STREAM, export.shareUri)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                context.startActivity(Intent.createChooser(send, null))
                viewModel.shareStarted()
            }
        }
    }
    val messages = mapOf(
        DataMessage.SAVED to stringResource(R.string.data_saved),
        DataMessage.CSV_SAVED to stringResource(R.string.data_csv_saved),
        DataMessage.WRITE_FAILED to stringResource(R.string.data_write_failed)
    )
    LaunchedEffect(state.message) {
        state.message?.let { snackbar.showSnackbar(messages.getValue(it)); viewModel.clearMessage() }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            ScreenTopBar(stringResource(R.string.data_title), Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back), onBack)

            LastBackupCard(s.lastBackupAt)

            SettingsGroup {
                SettingsRow(stringResource(R.string.data_export), Icons.Rounded.Download, Mint, { exportSheet = true })
                SettingsRow(stringResource(R.string.data_import), Icons.Rounded.Restore, Sky, { openFile.launch(arrayOf("*/*")) })
                ToggleRow(stringResource(R.string.data_reminder), Icons.Rounded.EventRepeat, Sun, s.backupReminderEnabled, { viewModel.setBackupReminder(it) }, showDivider = false)
            }
            Hint(stringResource(R.string.data_backup_hint))

            SettingsGroup {
                SettingsRow(stringResource(R.string.data_csv), Icons.Rounded.TableChart, Lime, viewModel::prepareCsv, showDivider = false)
            }
            Hint(stringResource(R.string.data_csv_hint))

            SettingsGroup {
                ToggleRow(stringResource(R.string.data_cloud), Icons.Rounded.CloudSync, Lavender, s.cloudSnapshotEnabled, { viewModel.setCloudSnapshot(it) }, showDivider = false)
            }
            Hint(stringResource(R.string.data_cloud_hint))

            SettingsGroup {
                SettingsRow(stringResource(R.string.data_wipe), Icons.Rounded.DeleteForever, Peach, { confirmWipe = true }, showDivider = false)
            }
            Hint(stringResource(R.string.data_wipe_hint))
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(16.dp))
    }

    if (exportSheet) {
        ExportSheet(
            onDismiss = { exportSheet = false },
            onExport = { password, target -> exportSheet = false; viewModel.prepareBackup(password, target) }
        )
    }

    ImportFlow(
        step = state.import,
        onPassword = viewModel::submitPassword,
        onConfirm = viewModel::confirmImport,
        onClose = viewModel::closeImport
    )

    if (confirmWipe) {
        StreakDialog(
            onDismiss = { confirmWipe = false },
            title = stringResource(R.string.data_wipe_title),
            text = stringResource(R.string.data_wipe_text),
            mascot = MascotMood.SAD
        ) {
            DialogAction(stringResource(R.string.data_wipe_confirm), ButtonVariant.Danger) { confirmWipe = false; viewModel.wipeAll() }
            DialogAction(stringResource(R.string.cancel), ButtonVariant.Secondary) { confirmWipe = false }
        }
    }

    if (state.busy || state.import is ImportStep.Working) BusyDialog()
}

@Composable
private fun LastBackupCard(last: Instant?) {
    val text = when {
        last == null -> stringResource(R.string.data_last_never)
        else -> {
            val days = Duration.between(last, Instant.now()).toDays().toInt().coerceAtLeast(0)
            when (days) {
                0 -> stringResource(R.string.data_last_today)
                1 -> stringResource(R.string.data_last_yesterday)
                else -> pluralStringResource(R.plurals.data_last_days, days, days)
            }
        }
    }
    Column(
        Modifier
            .padding(top = 10.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Mint)
            .padding(18.dp)
    ) {
        Text(stringResource(R.string.data_last_label), style = MaterialTheme.typography.labelLarge, color = InkOnPastel.copy(alpha = .7f))
        Text(text, style = MaterialTheme.typography.headlineSmall, color = InkOnPastel, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun ExportSheet(onDismiss: () -> Unit, onExport: (String, ExportTarget) -> Unit) {
    var password by remember { mutableStateOf("") }
    var repeat by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    val tooShort = password.isNotEmpty() && password.length < MIN_PASSWORD
    val mismatch = password.isNotEmpty() && repeat != password
    val valid = !tooShort && !mismatch
    StreakSheet(onDismiss = onDismiss, title = stringResource(R.string.data_export)) {
        Text(stringResource(R.string.data_export_text), style = MaterialTheme.typography.bodyMedium, color = StreakTheme.palette.inkSoft)
        PasswordField(password, { password = it }, stringResource(R.string.data_password_optional), visible, { visible = !visible }, isError = tooShort)
        if (password.isNotEmpty()) {
            PasswordField(repeat, { repeat = it }, stringResource(R.string.data_password_repeat), visible, { visible = !visible }, isError = repeat.isNotEmpty() && mismatch)
        }
        Text(
            when {
                tooShort -> pluralStringResource(R.plurals.data_password_short, MIN_PASSWORD, MIN_PASSWORD)
                password.isEmpty() -> stringResource(R.string.data_password_none_hint)
                else -> stringResource(R.string.data_password_hint)
            },
            style = MaterialTheme.typography.bodySmall,
            color = if (tooShort) StreakTheme.palette.danger else StreakTheme.palette.inkSoft,
            modifier = Modifier.padding(top = 8.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 20.dp)) {
            StreakButton(stringResource(R.string.share), { onExport(password, ExportTarget.SHARE) }, Modifier.weight(1f), ButtonVariant.Secondary, enabled = valid)
            StreakButton(stringResource(R.string.data_save_file), { onExport(password, ExportTarget.SAVE) }, Modifier.weight(1f), enabled = valid)
        }
    }
}

@Composable
private fun PasswordField(value: String, onChange: (String) -> Unit, label: String, visible: Boolean, onToggle: () -> Unit, isError: Boolean = false) {
    StreakTextField(
        value = value,
        onValueChange = onChange,
        label = label,
        isError = isError,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = onToggle) {
                Icon(
                    if (visible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                    contentDescription = stringResource(if (visible) R.string.data_password_hide else R.string.data_password_show),
                    tint = StreakTheme.palette.inkSoft
                )
            }
        }
    )
}

@Composable
internal fun ImportFlow(step: ImportStep, onPassword: (String) -> Unit, onConfirm: (ImportMode) -> Unit, onClose: () -> Unit) {
    when (step) {
        is ImportStep.NeedsPassword -> {
            var password by remember { mutableStateOf("") }
            var visible by remember { mutableStateOf(false) }
            StreakSheet(onDismiss = onClose, title = stringResource(R.string.data_import_password_title)) {
                Text(stringResource(R.string.data_import_password_text), style = MaterialTheme.typography.bodyMedium, color = StreakTheme.palette.inkSoft)
                PasswordField(password, { password = it }, stringResource(R.string.data_password), visible, { visible = !visible }, isError = step.wrong)
                if (step.wrong) {
                    Text(stringResource(R.string.data_password_wrong), style = MaterialTheme.typography.bodySmall, color = StreakTheme.palette.danger, modifier = Modifier.padding(top = 8.dp))
                }
                StreakButton(stringResource(R.string.data_unlock), { onPassword(password) }, Modifier.padding(top = 20.dp).fillMaxWidth(), enabled = password.isNotEmpty())
            }
        }
        is ImportStep.Preview -> StreakSheet(onDismiss = onClose, title = stringResource(R.string.data_preview_title)) {
            SummaryText(step.summary)
            if (step.summary.skipped > 0) {
                Text(pluralStringResource(R.plurals.data_preview_skipped, step.summary.skipped, step.summary.skipped), style = MaterialTheme.typography.bodySmall, color = StreakTheme.palette.inkSoft, modifier = Modifier.padding(top = 8.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 20.dp)) {
                StreakButton(stringResource(R.string.data_merge), { onConfirm(ImportMode.MERGE) }, Modifier.fillMaxWidth(), ButtonVariant.Dark)
                Text(stringResource(R.string.data_merge_hint), style = MaterialTheme.typography.bodySmall, color = StreakTheme.palette.inkSoft)
                StreakButton(stringResource(R.string.data_replace), { onConfirm(ImportMode.REPLACE) }, Modifier.fillMaxWidth(), ButtonVariant.Danger)
                Text(stringResource(R.string.data_replace_hint), style = MaterialTheme.typography.bodySmall, color = StreakTheme.palette.inkSoft)
            }
        }
        is ImportStep.Done -> StreakDialog(
            onDismiss = onClose,
            title = stringResource(R.string.data_import_done),
            text = stringResource(R.string.data_import_done_text),
            mascot = MascotMood.CHEER
        ) {
            DialogAction(stringResource(R.string.close), ButtonVariant.Dark, onClose)
        }
        is ImportStep.Failed -> StreakDialog(
            onDismiss = onClose,
            title = stringResource(R.string.data_import_failed),
            text = stringResource(
                when (step.error) {
                    ImportError.NOT_A_BACKUP -> R.string.data_error_not_backup
                    ImportError.NEWER_VERSION -> R.string.data_error_newer
                    ImportError.CORRUPT -> R.string.data_error_corrupt
                    ImportError.READ_FAILED -> R.string.data_error_read
                }
            ),
            mascot = MascotMood.THINKING
        ) {
            DialogAction(stringResource(R.string.close), ButtonVariant.Secondary, onClose)
        }
        ImportStep.Idle, ImportStep.Working -> Unit
    }
}

@Composable
private fun SummaryText(summary: BackupSummary) {
    val locale = LocalConfiguration.current.locales[0]
    val dateFormat = remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale) }
    val palette = StreakTheme.palette
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        summary.createdAt?.let {
            Text(stringResource(R.string.data_preview_created, it.atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormat)), style = MaterialTheme.typography.titleSmall, color = palette.ink)
        }
        Text(pluralStringResource(R.plurals.data_count_habits, summary.habits, summary.habits), style = MaterialTheme.typography.bodyLarge, color = palette.ink)
        Text(pluralStringResource(R.plurals.data_count_completions, summary.completions, summary.completions), style = MaterialTheme.typography.bodyLarge, color = palette.ink)
        if (summary.sessions > 0) Text(pluralStringResource(R.plurals.detail_session_count, summary.sessions, summary.sessions), style = MaterialTheme.typography.bodyLarge, color = palette.ink)
        if (summary.tasks > 0) Text(pluralStringResource(R.plurals.data_count_tasks, summary.tasks, summary.tasks), style = MaterialTheme.typography.bodyLarge, color = palette.ink)
        if (summary.moods > 0) Text(stringResource(R.string.data_count_moods, summary.moods), style = MaterialTheme.typography.bodyLarge, color = palette.ink)
        val first = summary.firstDate
        val last = summary.lastDate
        if (first != null && last != null) {
            Text(stringResource(R.string.data_preview_range, first.format(dateFormat), last.format(dateFormat)), style = MaterialTheme.typography.bodyMedium, color = palette.inkSoft)
        }
    }
}

@Composable
internal fun BusyDialog() {
    Dialog(onDismissRequest = {}) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surface)
        ) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
    }
}

@Composable
private fun ToggleRow(title: String, icon: ImageVector, iconBg: Color, checked: Boolean, onChange: (Boolean) -> Unit, showDivider: Boolean = true) {
    SettingsRow(title, icon, iconBg, { onChange(!checked) }, showDivider = showDivider) {
        StreakSwitch(checked, onChange, contentDescription = title)
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = StreakTheme.palette.inkSoft, modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 8.dp))
}

private const val MIN_PASSWORD = 6
