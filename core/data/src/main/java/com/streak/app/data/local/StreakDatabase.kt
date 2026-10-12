package com.streak.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.streak.app.data.local.dao.BackupDao
import com.streak.app.data.local.dao.CompletionDao
import com.streak.app.data.local.dao.GameDao
import com.streak.app.data.local.dao.HabitDao
import com.streak.app.data.local.dao.MoodDao
import com.streak.app.data.local.dao.SessionDao
import com.streak.app.data.local.dao.StatsDao
import com.streak.app.data.local.dao.TaskDao
import com.streak.app.data.local.dao.VacationDao
import com.streak.app.data.local.entity.CompletionEntity
import com.streak.app.data.local.entity.DailyAggregateEntity
import com.streak.app.data.local.entity.HabitEntity
import com.streak.app.data.local.entity.HabitReminderEntity
import com.streak.app.data.local.entity.MoodEntity
import com.streak.app.data.local.entity.SessionEntity
import com.streak.app.data.local.entity.StreakCacheEntity
import com.streak.app.data.local.entity.TaskEntity
import com.streak.app.data.local.entity.VacationEntity

// Verzija 2 je nova osnova sheme. v1 ni imela izvozenih shem, podatki pa niso bili
// produkcijski, zato je prehod 1 -> 2 edini dovoljeni destruktivni prehod.
// Vsaka nadaljnja sprememba MORA imeti eksplicitno migracijo v MIGRATIONS in test.
@Database(
    entities = [
        HabitEntity::class,
        HabitReminderEntity::class,
        CompletionEntity::class,
        SessionEntity::class,
        StreakCacheEntity::class,
        DailyAggregateEntity::class,
        VacationEntity::class,
        TaskEntity::class,
        MoodEntity::class
    ],
    version = StreakDatabase.VERSION,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class StreakDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun completionDao(): CompletionDao
    abstract fun statsDao(): StatsDao
    abstract fun vacationDao(): VacationDao
    abstract fun sessionDao(): SessionDao
    abstract fun taskDao(): TaskDao
    abstract fun backupDao(): BackupDao
    abstract fun gameDao(): GameDao
    abstract fun moodDao(): MoodDao

    companion object {
        const val NAME = "streak.db"
        const val VERSION = 4

        // Verzija, iz katere je destruktivni prehod dovoljen (le stara v1)
        const val LEGACY_VERSION = 1

        // v3: dnevnik razpolozenja
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `mood_entries` (`epochDay` INTEGER NOT NULL, `mood` INTEGER NOT NULL, " +
                        "`energy` INTEGER, `note` TEXT, `loggedAtMillis` INTEGER NOT NULL, PRIMARY KEY(`epochDay`))"
                )
            }
        }

        // v4: opomniki nalog (TaskReminder.encode)
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `tasks` ADD COLUMN `reminders` TEXT NOT NULL DEFAULT ''")
            }
        }

        // Vse migracije od v2 naprej
        val MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_2_3, MIGRATION_3_4)
    }
}
