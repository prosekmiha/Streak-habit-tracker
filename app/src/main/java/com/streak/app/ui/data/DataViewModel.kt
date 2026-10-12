package com.streak.app.ui.data

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streak.app.backup.BackupFiles
import com.streak.app.domain.backup.BackupContents
import com.streak.app.domain.backup.BackupService
import com.streak.app.domain.backup.BackupSummary
import com.streak.app.domain.backup.ImportMode
import com.streak.app.domain.backup.ParseResult
import com.streak.app.domain.model.AppSettings
import com.streak.app.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
    private val backup: BackupService,
    private val files: BackupFiles,
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
        val ready = if (export.target == ExportTarget.SHARE) {
            export.copy(shareUri = files.writeShareFile(export.fileName, export.text))
        } else export
        local.update { it.copy(pending = ready) }
    }

    // Uporabnik je izbral mesto v SAF (null = preklical)
    fun writeTo(uri: Uri?) {
        val export = local.value.pending ?: return
        local.update { it.copy(pending = null) }
        if (uri == null) return
        viewModelScope.launch {
            val ok = files.write(uri, export.text)
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
            val text = files.read(uri, MAX_IMPORT_BYTES)
            if (text == null) {
                local.update { it.copy(import = ImportStep.Failed(ImportError.READ_FAILED)) }
                return@launch
            }
            handle(text, parseSafely(text, null), password = null)
        }
    }

    fun submitPassword(password: String) {
        val step = local.value.import as? ImportStep.NeedsPassword ?: return
        local.update { it.copy(import = ImportStep.Working) }
        viewModelScope.launch { handle(step.text, parseSafely(step.text, password.toCharArray()), password) }
    }

    // Nepricakovana napaka pri branju je za uporabnika poskodovana datoteka (ne sesutje)
    private suspend fun parseSafely(text: String, password: CharArray?): ParseResult = try {
        backup.parse(text, password)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e("DataViewModel", "Branje varnostne kopije ni uspelo", e)
        ParseResult.Corrupt
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
            // Uvoz je ena transakcija: ob napaki ostanejo stari podatki nespremenjeni
            val step = try {
                backup.import(preview.contents, mode)
                ImportStep.Done(mode, preview.summary)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("DataViewModel", "Uvoz ni uspel", e)
                ImportStep.Failed(ImportError.CORRUPT)
            }
            local.update { it.copy(import = step) }
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
        // Posnetek se bo zapisal ob naslednji spremembi; napaka pisanja ne sme sesuti zaslona
        runCatching { backup.writeSnapshot() }.onFailure { Log.e("DataViewModel", "Posnetka ni mogoce zapisati", it) }
    }

    fun wipeAll() = work { backup.wipeAll() }

    private fun work(block: suspend () -> Unit) {
        local.update { it.copy(busy = true) }
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Izvoz ali brisanje ni uspelo: sporocilo namesto sesutja
                Log.e("DataViewModel", "Opravilo s podatki ni uspelo", e)
                local.update { it.copy(message = DataMessage.WRITE_FAILED) }
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
