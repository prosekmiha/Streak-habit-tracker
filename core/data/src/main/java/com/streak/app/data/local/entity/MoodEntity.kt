package com.streak.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// Razpolozenje: en zapis na logicni dan
@Entity(tableName = "mood_entries")
data class MoodEntity(
    @PrimaryKey val epochDay: Long,
    val mood: Int,
    val energy: Int?,
    val note: String?,
    val loggedAtMillis: Long
)
