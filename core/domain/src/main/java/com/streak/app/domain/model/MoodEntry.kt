package com.streak.app.domain.model

import java.time.Instant
import java.time.LocalDate

// Dnevni vnos razpolozenja (en na logicni dan)
data class MoodEntry(
    val date: LocalDate,
    // 1 (slabo) .. 5 (odlicno)
    val mood: Int,
    // 1 (izcrpan) .. 5 (poln energije), neobvezno
    val energy: Int? = null,
    val note: String? = null,
    val loggedAt: Instant
) {
    init {
        require(mood in RANGE) { "mood $mood" }
        require(energy == null || energy in RANGE) { "energy $energy" }
    }

    companion object {
        val RANGE = 1..5
    }
}
