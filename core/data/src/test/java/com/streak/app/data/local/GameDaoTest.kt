package com.streak.app.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.streak.app.data.local.entity.StreakCacheEntity
import com.streak.app.data.mapper.reminderEntities
import com.streak.app.data.mapper.toEntity
import com.streak.app.domain.model.Completion
import com.streak.app.domain.model.CompletionStatus
import com.streak.app.domain.model.GoalType
import com.streak.app.domain.model.HabitSession
import com.streak.app.domain.model.StreakUnit
import com.streak.app.testing.testHabit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

@RunWith(AndroidJUnit4::class)
class GameDaoTest {

    private lateinit var db: StreakDatabase
    private val day = LocalDate.of(2026, 10, 11)
    private val cest = ZoneOffset.ofHours(2)

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), StreakDatabase::class.java)
            .allowMainThreadQueries().build()
    }

    @After fun tearDown() = db.close()

    private suspend fun habit(id: String, goalType: GoalType = GoalType.YES_NO) {
        val h = testHabit(id = id, goalType = goalType)
        db.habitDao().upsertWithReminders(h.toEntity(), h.reminderEntities())
    }

    private suspend fun log(
        habitId: String,
        date: LocalDate,
        localTime: String,
        loggedDate: LocalDate = date,
        status: CompletionStatus = CompletionStatus.DONE,
        note: String? = null,
        edited: Boolean = false
    ) {
        val at = LocalDateTime.of(loggedDate, java.time.LocalTime.parse(localTime)).toInstant(cest)
        db.completionDao().upsert(
            Completion("$habitId-$date", habitId, date, status, 1.0, at, cest, note, edited).toEntity()
        )
    }

    @Test fun dayActivityUsesLocalTimeOfLogging() = runTest {
        listOf("a", "b", "c", "d", "e").forEach { habit(it) }
        log("a", day, "08:30")                          // pred 9:00 (lokalno; v UTC je 06:30)
        log("b", day, "17:59", note = "dobro")          // pred 18:00, z opombo
        log("c", day, "20:00", note = "  ")             // prazna opomba ne steje
        log("d", day, "07:00", edited = true)           // naknadni vpis ne steje kot zgodnji
        log("e", day, "01:00", loggedDate = day.plusDays(1)) // vpis po polnoci (dan se konca ob 4:00)
        log("x".also { habit(it) }, day, "06:00", status = CompletionStatus.SKIPPED) // ni opravljeno

        val row = db.gameDao().observeDayActivity().first().single()
        assertEquals(day.toEpochDay(), row.epochDay)
        assertEquals(5, row.done)
        assertEquals(1, row.doneBefore9)
        assertEquals(2, row.doneBefore18)
        assertEquals(1, row.notes)
    }

    @Test fun sessionUnitsAreCappedPerSession() = runTest {
        habit("m")
        val start = LocalDateTime.of(day, java.time.LocalTime.NOON).toInstant(cest)
        fun session(id: String, minutes: Long) =
            HabitSession(id, "m", day, start, start.plusSeconds(minutes * 60), minutes * 60).toEntity()
        db.sessionDao().upsert(session("s1", 12))  // 2 enoti
        db.sessionDao().upsert(session("s2", 4))   // 0 enot
        db.sessionDao().upsert(session("s3", 90))  // najvec 12 enot

        val row = db.gameDao().observeDaySessions().first().single()
        assertEquals(3, row.count)
        assertEquals((12 + 4 + 90) * 60L, row.seconds)
        assertEquals(14, row.units)
    }

    @Test fun cleanDaysSumOnlyQuitHabits() = runTest {
        habit("q1", GoalType.QUIT)
        habit("q2", GoalType.QUIT)
        habit("y")
        fun cache(id: String, total: Int) = StreakCacheEntity(id, 0, 0, 0f, StreakUnit.DAY, total, day.toEpochDay())
        assertEquals(0, db.gameDao().observeCleanDays().first())
        db.statsDao().upsertStreaks(listOf(cache("q1", 5), cache("q2", 3), cache("y", 40)))
        assertEquals(8, db.gameDao().observeCleanDays().first())
    }
}
