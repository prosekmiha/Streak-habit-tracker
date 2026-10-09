package com.streak.app.domain.model

// Izracunani podatki o streaku za posamezno navado
data class HabitStreak(
    val currentStreak: Int,
    val bestStreak: Int,
    val completionRate: Float // 0f..1f za zadnjih N dni
)
