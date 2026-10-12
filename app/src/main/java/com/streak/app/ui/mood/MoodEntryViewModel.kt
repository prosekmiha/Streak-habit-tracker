package com.streak.app.ui.mood

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streak.app.domain.model.MoodEntry
import com.streak.app.domain.repository.MoodRepository
import com.streak.app.domain.time.DayClock
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class MoodForm(
    val date: LocalDate? = null,
    val mood: Int? = null,
    val energy: Int? = null,
    val note: String = "",
    val exists: Boolean = false
) {
    val canSave: Boolean get() = date != null && mood != null
}

// Vpis razpolozenja za izbran dan (privzeto danes); prihodnjih dni ni mogoce vpisati
@HiltViewModel
class MoodEntryViewModel @Inject constructor(
    private val repository: MoodRepository,
    private val dayClock: DayClock
) : ViewModel() {

    private val _form = MutableStateFlow(MoodForm())
    val form: StateFlow<MoodForm> = _form.asStateFlow()

    // preset: izraz, izbran ze na kartici domacega zaslona
    fun open(date: LocalDate?, preset: Int? = null) {
        viewModelScope.launch {
            val today = dayClock.today()
            val day = (date ?: today).coerceAtMost(today)
            val existing = repository.get(day)
            _form.value = MoodForm(day, preset ?: existing?.mood, existing?.energy, existing?.note.orEmpty(), existing != null)
        }
    }

    fun selectMood(level: Int) = _form.update { it.copy(mood = level) }

    // Ponoven dotik izbrane energije jo odznaci (neobvezno polje)
    fun selectEnergy(level: Int) = _form.update { it.copy(energy = if (it.energy == level) null else level) }

    fun setNote(note: String) = _form.update { it.copy(note = note.take(MAX_NOTE)) }

    fun save(onDone: () -> Unit) {
        val f = _form.value
        val date = f.date ?: return
        val mood = f.mood ?: return
        viewModelScope.launch {
            repository.save(MoodEntry(date, mood, f.energy, f.note.trim().ifBlank { null }, dayClock.now()))
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        val date = _form.value.date ?: return
        viewModelScope.launch {
            repository.delete(date)
            onDone()
        }
    }

    private companion object {
        const val MAX_NOTE = 500
    }
}
