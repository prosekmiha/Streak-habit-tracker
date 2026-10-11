package com.streak.app.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
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
