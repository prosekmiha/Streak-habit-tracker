package com.streak.app.ui.data

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streak.app.backup.BackupManager
import com.streak.app.backup.ImportMode
import com.streak.app.domain.backup.BackupContents
import com.streak.app.domain.backup.BackupSummary
import com.streak.app.domain.backup.ParseResult
import com.streak.app.domain.model.AppSettings
import com.streak.app.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import javax.inject.Inject

enum class ExportTarget { SAVE, SHARE }

// Pripravljen izvoz, ki caka na izbiro mesta (SAF) ali deljenje
data class PendingExport(val text: String, val fileName: String, val mime: String, val target: ExportTarget, val shareUri: Uri? = null)

enum class ImportError { NOT_A_BACKUP, NEWER_VERSION, CORRUPT, READ_FAILED }

sealed interface ImportStep {
    data object Idle : ImportStep
    data object Working : ImportStep
    data class NeedsPassword(val text: String, val wrong: Boolean = false) : ImportStep
    data class Preview(val summary: BackupSummary, val contents: BackupContents) : ImportStep
    data class Done(val mode: ImportMode, val summary: BackupSummary) : ImportStep
    data class Failed(val error: ImportError) : ImportStep
}

enum class DataMessage { SAVED, CSV_SAVED, WRITE_FAILED }

data class DataUiState(
    val settings: AppSettings = AppSettings(),
    val busy: Boolean = false,
    val import: ImportStep = ImportStep.Idle,
    val pending: PendingExport? = null,
    val message: DataMessage? = null
)

@HiltViewModel
class DataViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backup: BackupManager,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val local = MutableStateFlow(DataUiState())

    val state: StateFlow<DataUiState> = combine(settingsRepository.settings, local) { s, l -> l.copy(settings = s) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DataUiState())

    // --- izvoz ---

    fun prepareBackup(password: String, target: ExportTarget) = work {
        val text = backup.exportJson(password.takeIf { it.isNotEmpty() }?.toCharArray())
        stage(PendingExport(text, "streak-backup-${LocalDate.now()}.json", "application/json", target))
    }

    fun prepareCsv() = work {
        stage(PendingExport(backup.exportCsv(), "streak-zapisi-${LocalDate.now()}.csv", "text/csv", ExportTarget.SAVE))
    }

    private suspend fun stage(export: PendingExport) {
        val ready = if (export.target == ExportTarget.SHARE) export.copy(shareUri = writeShareFile(export)) else export
        local.update { it.copy(pending = ready) }
    }

    // Deljenje: zacasna datoteka v cache/exports (prejsnje se pobrisejo)
    private suspend fun writeShareFile(export: PendingExport): Uri = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "exports").apply { deleteRecursively(); mkdirs() }
        val file = File(dir, export.fileName).apply { writeText(export.text) }
        FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    }

    // Uporabnik je izbral mesto v SAF (null = preklical)
    fun writeTo(uri: Uri?) {
        val export = local.value.pending ?: return
        local.update { it.copy(pending = null) }
        if (uri == null) return
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(export.text.toByteArray()) }
                }.isSuccess
            }
            val message = when {
                !ok -> DataMessage.WRITE_FAILED
                export.mime == "text/csv" -> DataMessage.CSV_SAVED
                else -> DataMessage.SAVED
            }
            local.update { it.copy(message = message) }
        }
    }

    fun shareStarted() = local.update { it.copy(pending = null) }

    fun clearMessage() = local.update { it.copy(message = null) }

    // --- uvoz ---

    fun readImport(uri: Uri?) {
        uri ?: return
        local.update { it.copy(import = ImportStep.Working) }
        viewModelScope.launch {
            val text = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)!!.use { input ->
                        val bytes = input.readNBytesCompat(MAX_IMPORT_BYTES + 1)
                        if (bytes.size > MAX_IMPORT_BYTES) null else bytes.decodeToString()
                    }
                }.getOrNull()
            }
            if (text == null) {
                local.update { it.copy(import = ImportStep.Failed(ImportError.READ_FAILED)) }
                return@launch
            }
            handle(text, backup.parse(text), password = null)
        }
    }

    fun submitPassword(password: String) {
        val step = local.value.import as? ImportStep.NeedsPassword ?: return
        local.update { it.copy(import = ImportStep.Working) }
        viewModelScope.launch { handle(step.text, backup.parse(step.text, password.toCharArray()), password) }
    }

    private fun handle(text: String, result: ParseResult, password: String?) {
        val step = when (result) {
            is ParseResult.Ok -> ImportStep.Preview(result.summary, result.contents)
            ParseResult.NeedsPassword -> ImportStep.NeedsPassword(text)
            ParseResult.WrongPassword -> ImportStep.NeedsPassword(text, wrong = password != null)
            ParseResult.NotABackup -> ImportStep.Failed(ImportError.NOT_A_BACKUP)
            ParseResult.NewerVersion -> ImportStep.Failed(ImportError.NEWER_VERSION)
            ParseResult.Corrupt -> ImportStep.Failed(ImportError.CORRUPT)
        }
        local.update { it.copy(import = step) }
    }

    fun confirmImport(mode: ImportMode) {
        val preview = local.value.import as? ImportStep.Preview ?: return
        local.update { it.copy(import = ImportStep.Working) }
        viewModelScope.launch {
            backup.import(preview.contents, mode)
            local.update { it.copy(import = ImportStep.Done(mode, preview.summary)) }
        }
    }

    fun closeImport() = local.update { it.copy(import = ImportStep.Idle) }

    // Obnova med onboardingom: po uvozu gre uporabnik naravnost na domaci zaslon
    fun finishOnboarding() = viewModelScope.launch {
        local.update { it.copy(import = ImportStep.Idle) }
        settingsRepository.update { it.copy(onboardingDone = true) }
    }

    // --- nastavitve in brisanje ---

    fun setBackupReminder(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.update { it.copy(backupReminderEnabled = enabled) }
    }

    fun setCloudSnapshot(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.update { it.copy(cloudSnapshotEnabled = enabled) }
        backup.writeSnapshot()
    }

    fun wipeAll() = work { backup.wipeAll() }

    private fun work(block: suspend () -> Unit) {
        local.update { it.copy(busy = true) }
        viewModelScope.launch {
            try {
                block()
            } finally {
                local.update { it.copy(busy = false) }
            }
        }
    }

    private companion object {
        // Zascita pred nakljucno izbiro ogromne datoteke
        const val MAX_IMPORT_BYTES = 64 * 1024 * 1024
    }
}

// InputStream.readNBytes je sele od API 33
private fun java.io.InputStream.readNBytesCompat(max: Int): ByteArray {
    val out = java.io.ByteArrayOutputStream()
    val buf = ByteArray(64 * 1024)
    while (out.size() < max) {
        val n = read(buf, 0, minOf(buf.size, max - out.size()))
        if (n < 0) break
        out.write(buf, 0, n)
    }
    return out.toByteArray()
}
