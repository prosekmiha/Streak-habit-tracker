package com.streak.app.domain.model

import java.time.LocalDate

// Vnaprej izracunan dnevni povzetek cez vse navade (za koledar, heatmap, grafe)
data class DailyAggregate(
    val date: LocalDate,
    val scheduled: Int,
    val done: Int
) {
    val ratio: Float get() = if (scheduled == 0) 0f else (done.toFloat() / scheduled).coerceIn(0f, 1f)
}
