package com.streak.app.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

// Room relacija: navada + vsi njeni dnevni zapisi
data class HabitWithLogsEntity(
    @Embedded val habit: HabitEntity,
    @Relation(parentColumn = "id", entityColumn = "habitId")
    val logs: List<HabitLogEntity>
)
