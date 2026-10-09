package com.streak.app.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

// Zapis navade za logicni dan. Hrani UTC trenutek, lokalni datum in zamik pasu
// ob vpisu, da zgodovina ostane pravilna tudi med potovanji.
data class Completion(
    val id: String,
    val habitId: String,
    val date: LocalDate,
    val status: CompletionStatus,
    // YES_NO: 1/0, QUANTITY: kolicina, DURATION: sekunde
    val value: Double,
    val loggedAt: Instant,
    val zoneOffset: ZoneOffset,
    val note: String? = null,
    // Vpisano za pretekli dan (backfill) ali rocno popravljeno
    val manuallyEdited: Boolean = false
)
