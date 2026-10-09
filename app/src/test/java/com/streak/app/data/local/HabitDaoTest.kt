package com.streak.app.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.streak.app.data.local.entity.HabitEntity
import com.streak.app.data.local.entity.HabitLogEntity
import com.streak.app.domain.model.Frequency
import com.streak.app.domain.model.LogStatus
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HabitDaoTest {

    private lateinit var db: StreakDatabase

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), StreakDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After fun tearDown() = db.close()

    private fun habit(title: String) = HabitEntity(
        id = "h1", title = title, icon = "Star", colorHex = "#000000",
        frequency = Frequency.DAILY, targetPerWeek = 7, reminderMinuteOfDay = null,
        reminderEnabled = false, isActive = true, createdAt = 0L, sortOrder = 0
    )

    // Regresija: REPLACE na nadrejeni tabeli je sprozil ON DELETE CASCADE in
    // ob vsakem urejanju navade izbrisal vso njeno zgodovino.
    @Test fun editingHabitKeepsItsLogs() = runTest {
        db.habitDao().upsert(habit("Branje"))
        db.habitLogDao().upsert(HabitLogEntity("l1", "h1", 100, LogStatus.DONE, 1L, null))
        db.habitLogDao().upsert(HabitLogEntity("l2", "h1", 101, LogStatus.DONE, 1L, null))

        db.habitDao().upsert(habit("Branje 20 strani"))

        assertEquals("Branje 20 strani", db.habitDao().getHabit("h1")?.title)
        assertEquals(2, db.habitLogDao().getForHabit("h1").size)
    }
}
