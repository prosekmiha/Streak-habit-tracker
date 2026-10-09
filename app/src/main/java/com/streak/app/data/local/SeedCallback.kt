package com.streak.app.data.local

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.streak.app.domain.model.Frequency
import com.streak.app.domain.model.LogStatus
import java.time.LocalDate
import java.util.UUID

// Ob prvem ustvarjanju baze vstavi 3 vzorcne navade z nekaj logi,
// da je UI takoj poln.
class SeedCallback : RoomDatabase.Callback() {

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        val now = System.currentTimeMillis()
        val today = LocalDate.now()

        data class Seed(val title: String, val icon: String, val color: String, val reminderMin: Int, val doneOffsets: List<Long>)
        val seeds = listOf(
            Seed("Tek zjutraj", "DirectionsRun", "#5dcb80", 7 * 60, listOf(0L, 1, 2, 3, 4, 5)),
            Seed("Branje 20 min", "MenuBook", "#b89dff", 21 * 60, listOf(0L, 1, 3, 4)),
            Seed("2L vode", "WaterDrop", "#ff8c69", 12 * 60, listOf(0L, 1, 2, 3))
        )

        seeds.forEachIndexed { index, s ->
            val habitId = UUID.randomUUID().toString()
            db.execSQL(
                "INSERT INTO habits (id,title,icon,colorHex,frequency,targetPerWeek,reminderMinuteOfDay,reminderEnabled,isActive,createdAt,sortOrder) " +
                    "VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                arrayOf<Any?>(
                    habitId, s.title, s.icon, s.color, Frequency.DAILY.name, 7,
                    s.reminderMin, 1, 1, now, index
                )
            )
            // Vzorcni logi (DONE) za pretekle dni
            s.doneOffsets.forEach { off ->
                val day = today.minusDays(off)
                val logId = UUID.randomUUID().toString()
                db.execSQL(
                    "INSERT INTO habit_logs (id,habitId,epochDay,status,completedAt,note) VALUES (?,?,?,?,?,?)",
                    arrayOf<Any?>(logId, habitId, day.toEpochDay(), LogStatus.DONE.name, now, null)
                )
            }
        }

        // Dve vzorcni nalogi za danes
        val t1 = UUID.randomUUID().toString()
        db.execSQL(
            "INSERT INTO tasks (id,title,dueEpochDay,dueMinuteOfDay,priority,isDone,isRecurring,recurrenceRule,createdAt) VALUES (?,?,?,?,?,?,?,?,?)",
            arrayOf<Any?>(t1, "Oddati poročilo", today.toEpochDay(), 14 * 60, "URGENT", 0, 0, null, now)
        )
        val t2 = UUID.randomUUID().toString()
        db.execSQL(
            "INSERT INTO tasks (id,title,dueEpochDay,dueMinuteOfDay,priority,isDone,isRecurring,recurrenceRule,createdAt) VALUES (?,?,?,?,?,?,?,?,?)",
            arrayOf<Any?>(t2, "Pokliči zdravnika", today.toEpochDay(), 16 * 60 + 30, "NORMAL", 0, 0, null, now)
        )
    }
}
