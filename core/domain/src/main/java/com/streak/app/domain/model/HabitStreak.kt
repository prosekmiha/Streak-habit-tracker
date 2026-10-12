package com.streak.app.domain.model

// Izracunani podatki o streaku za posamezno navado
data class HabitStreak(
    val currentStreak: Int,
    val bestStreak: Int,
    val completionRate: Float, // 0f..1f za zadnjih 30 dni oz. 4 obdobja
    val unit: StreakUnit = StreakUnit.DAY,
    val totalDone: Int = 0
) {
    companion object {
        val Empty = HabitStreak(0, 0, 0f)
    }
}
