package com.streak.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.streak.app.domain.model.CompletionStatus

@Entity(
    tableName = "completions",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["habitId", "epochDay"], unique = true), Index("epochDay")]
)
data class CompletionEntity(
    @PrimaryKey val id: String,
    val habitId: String,
    // Logicni lokalni dan (LocalDate.toEpochDay)
    val epochDay: Long,
    val status: CompletionStatus,
    val value: Double,
    // UTC trenutek vpisa in zamik pasu ob vpisu
    val loggedAtMillis: Long,
    val zoneOffsetSeconds: Int,
    val note: String?,
    val manuallyEdited: Boolean
)
