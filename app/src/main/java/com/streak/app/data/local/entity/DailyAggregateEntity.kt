package com.streak.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// Dnevni povzetek cez vse navade (nacrtovane / opravljene)
@Entity(tableName = "daily_aggregates")
data class DailyAggregateEntity(
    @PrimaryKey val epochDay: Long,
    val scheduled: Int,
    val done: Int
)
