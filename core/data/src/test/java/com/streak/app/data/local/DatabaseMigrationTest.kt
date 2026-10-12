package com.streak.app.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.streak.app.domain.health.DatabaseHealth
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val name = "migration-test.db"

    @Before fun setUp() { context.deleteDatabase(name) }
    @After fun tearDown() { context.deleteDatabase(name) }

    // Vsaka verzija od 2 naprej mora imeti eksplicitno migracijo do naslednje
    @Test fun everyVersionAfterBaselineHasAMigration() {
        val covered = StreakDatabase.MIGRATIONS.map { it.startVersion to it.endVersion }.toSet()
        for (v in 2 until StreakDatabase.VERSION) {
            assertTrue("Manjka migracija $v -> ${v + 1}", (v to v + 1) in covered)
        }
    }

    // Izvozena shema za trenutno verzijo mora obstajati (osnova za MigrationTestHelper)
    @Test fun currentSchemaIsExported() {
        val schema = File("schemas/${StreakDatabase::class.java.name}/${StreakDatabase.VERSION}.json")
        assertTrue("Manjka ${schema.path}", schema.exists())
    }

    // 2 -> 3: nova tabela razpolozenja, obstojeci podatki ostanejo
    @Test fun migrate2To3KeepsDataAndAddsMoodTable() {
        val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), StreakDatabase::class.java)
        // Absolutna pot: gonilnik v Robolectricu primerja ime s polno potjo
        val path = context.getDatabasePath(name).also { it.parentFile?.mkdirs() }.path
        helper.createDatabase(path, 2).use { db ->
            db.execSQL("INSERT INTO vacations (id, startEpochDay, endEpochDay) VALUES (1, 100, 105)")
        }
        helper.runMigrationsAndValidate(path, 3, true, StreakDatabase.MIGRATION_2_3).use { db ->
            db.query("SELECT startEpochDay, endEpochDay FROM vacations").use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(100L, c.getLong(0))
                assertEquals(105L, c.getLong(1))
            }
            db.execSQL("INSERT INTO mood_entries (epochDay, mood, energy, note, loggedAtMillis) VALUES (100, 4, NULL, 'ok', 0)")
            db.query("SELECT mood FROM mood_entries WHERE epochDay = 100").use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(4, c.getInt(0))
            }
        }
    }

    // 3 -> 4: naloge dobijo prazne opomnike, ostala polja ostanejo
    @Test fun migrate3To4AddsEmptyTaskReminders() {
        val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), StreakDatabase::class.java)
        val path = context.getDatabasePath(name).also { it.parentFile?.mkdirs() }.path
        helper.createDatabase(path, 3).use { db ->
            db.execSQL(
                "INSERT INTO tasks (id, title, dueEpochDay, dueMinuteOfDay, priority, isDone, isRecurring, recurrenceRule, createdAt) " +
                    "VALUES ('t1', 'Nakup', 100, 600, 'NORMAL', 0, 0, NULL, 1)"
            )
        }
        helper.runMigrationsAndValidate(path, 4, true, StreakDatabase.MIGRATION_3_4).use { db ->
            db.query("SELECT title, dueMinuteOfDay, reminders FROM tasks WHERE id = 't1'").use { c ->
                assertTrue(c.moveToFirst())
                assertEquals("Nakup", c.getString(0))
                assertEquals(600, c.getInt(1))
                assertEquals("", c.getString(2))
            }
        }
    }

    // Stara v1 baza se ob odprtju nadomesti z novo shemo (edini dovoljen destruktivni prehod),
    // DatabaseGuard pa pred tem naredi kopijo.
    @Test fun legacyV1IsBackedUpAndReplaced() = runTest {
        createLegacyV1(context.getDatabasePath(name))
        val guard = DatabaseGuard(context)
        guard.prepareForOpen(name, StreakDatabase.VERSION) { f ->
            SQLiteDatabase.openDatabase(f.path, null, SQLiteDatabase.OPEN_READONLY).use { it.version }
        }

        val backups = File(context.noBackupFilesDir, "db-backups").listFiles { f -> f.name.startsWith("streak-v1-") && f.name.endsWith(".db") }
        assertEquals(1, backups?.size)

        val db = Room.databaseBuilder(context, StreakDatabase::class.java, name)
            .addMigrations(*StreakDatabase.MIGRATIONS)
            .fallbackToDestructiveMigrationFrom(true, StreakDatabase.LEGACY_VERSION)
            .allowMainThreadQueries()
            .build()
        guard.openVerified(db, name)
        assertEquals(StreakDatabase.VERSION, db.openHelper.readableDatabase.version)
        assertEquals(0, db.habitDao().getAll().size)
        assertEquals(DatabaseHealth.Ok, guard.health.value)
        db.close()
    }

    private fun createLegacyV1(file: File) {
        file.parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            db.execSQL("CREATE TABLE habits (id TEXT PRIMARY KEY NOT NULL, title TEXT NOT NULL)")
            db.execSQL("INSERT INTO habits VALUES ('x', 'Tek')")
            db.version = 1
        }
    }
}
