package com.streak.app.di

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import com.streak.app.data.local.DatabaseGuard
import com.streak.app.data.local.LegacyEncryptedDatabase
import com.streak.app.data.local.StreakDatabase
import com.streak.app.data.local.dao.BackupDao
import com.streak.app.data.local.dao.GameDao
import com.streak.app.data.local.dao.MoodDao
import com.streak.app.data.local.dao.StatsDao
import com.streak.app.data.local.dao.TaskDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        guard: DatabaseGuard
    ): StreakDatabase {
        val dbFile = context.getDatabasePath(StreakDatabase.NAME)
        LegacyEncryptedDatabase.discardIfEncrypted(context, dbFile)
        guard.prepareForOpen(StreakDatabase.NAME, StreakDatabase.VERSION, ::readVersion)
        return Room.databaseBuilder(context, StreakDatabase::class.java, StreakDatabase.NAME)
            .addMigrations(*StreakDatabase.MIGRATIONS)
            // Edini dovoljeni destruktivni prehod: iz stare v1 brez izvozene sheme.
            // Splosnega destruktivnega fallbacka NI - manjkajoca migracija je napaka.
            .fallbackToDestructiveMigrationFrom(true, StreakDatabase.LEGACY_VERSION)
            .build()
    }

    // Verzija sheme obstojece datoteke (za kopijo pred migracijo)
    private fun readVersion(file: File): Int? = runCatching {
        SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY).use { it.version }
    }.getOrNull()

    @Provides fun provideTaskDao(db: StreakDatabase): TaskDao = db.taskDao()
    @Provides fun provideBackupDao(db: StreakDatabase): BackupDao = db.backupDao()
    @Provides fun provideGameDao(db: StreakDatabase): GameDao = db.gameDao()
    @Provides fun provideStatsDao(db: StreakDatabase): StatsDao = db.statsDao()
    @Provides fun provideMoodDao(db: StreakDatabase): MoodDao = db.moodDao()
}
