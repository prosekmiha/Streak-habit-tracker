package com.streak.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

// Dnevni povzetek opravljenih navad za igrifikacijo (ena vrstica na dan, ne vsi zapisi)
data class DayActivityRow(
    val epochDay: Long,
    val done: Int,
    val doneBefore9: Int,
    val doneBefore18: Int,
    val notes: Int
)

data class DaySessionsRow(
    val epochDay: Long,
    val count: Int,
    val seconds: Long,
    val units: Int
)

@Dao
interface GameDao {

    // Lokalni cas vpisa = UTC + zamik pasu ob vpisu. "Pred 9:00" velja le za vpise na isti
    // koledarski dan kot logicni dan (ne za naknadne vpise ali vpise po polnoci).
    @Query(
        """
        SELECT epochDay,
            COUNT(*) AS done,
            SUM(CASE WHEN manuallyEdited = 0
                AND (loggedAtMillis / 1000 + zoneOffsetSeconds) / 86400 = epochDay
                AND (loggedAtMillis / 1000 + zoneOffsetSeconds) % 86400 < 32400 THEN 1 ELSE 0 END) AS doneBefore9,
            SUM(CASE WHEN manuallyEdited = 0
                AND (loggedAtMillis / 1000 + zoneOffsetSeconds) / 86400 = epochDay
                AND (loggedAtMillis / 1000 + zoneOffsetSeconds) % 86400 < 64800 THEN 1 ELSE 0 END) AS doneBefore18,
            SUM(CASE WHEN note IS NOT NULL AND TRIM(note) != '' THEN 1 ELSE 0 END) AS notes
        FROM completions
        WHERE status = 'DONE'
        GROUP BY epochDay
        """
    )
    fun observeDayActivity(): Flow<List<DayActivityRow>>

    // 5-minutne enote seje, najvec 12 na sejo (1 ura)
    @Query(
        """
        SELECT epochDay, COUNT(*) AS count, SUM(durationSeconds) AS seconds,
            SUM(MIN(durationSeconds / 300, 12)) AS units
        FROM sessions
        GROUP BY epochDay
        """
    )
    fun observeDaySessions(): Flow<List<DaySessionsRow>>
}
