package com.streak.app.di

import android.content.Context
import android.util.Log
import com.streak.app.data.local.DatabaseEncryption
import com.streak.app.data.local.DatabaseKeyStore
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.io.File
import androidx.room.Room
import com.streak.app.data.local.DatabaseGuard
import com.streak.app.data.local.StreakDatabase
import com.streak.app.data.local.dao.BackupDao
import com.streak.app.data.local.dao.GameDao
import com.streak.app.data.local.dao.StatsDao
import com.streak.app.data.local.dao.TaskDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        guard: DatabaseGuard,
        keyStore: DatabaseKeyStore
    ): StreakDatabase {
        DatabaseEncryption.loadLibrary()
        val dbFile = context.getDatabasePath(StreakDatabase.NAME)
        val key = runCatching { keyStore.rawKey() }.getOrElse { error ->
            // Kljuca ni mogoce odviti (ponastavljen Keystore): baze ni vec mogoce brati.
            // Odmaknemo jo in zacnemo znova; ce obstaja samodejni posnetek, ga aplikacija ponudi.
            Log.e("DatabaseModule", "Kljuc baze ni dosegljiv", error)
            if (dbFile.exists()) dbFile.renameTo(File(dbFile.path + ".unreadable-" + System.currentTimeMillis()))
            listOf("-wal", "-shm").forEach { File(dbFile.path + it).delete() }
            keyStore.reset()
            keyStore.rawKey()
        }
        // Enkratna pretvorba baze iz razlicic brez sifriranja
        if (dbFile.exists() && DatabaseEncryption.isPlaintext(dbFile)) {
            runCatching { DatabaseEncryption.encryptInPlace(dbFile, key) }
                .onFailure { Log.e("DatabaseModule", "Sifriranje obstojece baze ni uspelo", it) }
        }
        guard.prepareForOpen(StreakDatabase.NAME, StreakDatabase.VERSION) { DatabaseEncryption.readVersion(it, key) }
        return Room.databaseBuilder(context, StreakDatabase::class.java, StreakDatabase.NAME)
            .openHelperFactory(SupportOpenHelperFactory(key.toByteArray()))
            .addMigrations(*StreakDatabase.MIGRATIONS)
            // Edini dovoljeni destruktivni prehod: iz stare v1 brez izvozene sheme.
            // Splosnega destruktivnega fallbacka NI - manjkajoca migracija je napaka.
            .fallbackToDestructiveMigrationFrom(true, StreakDatabase.LEGACY_VERSION)
            .build()
    }

    @Provides fun provideTaskDao(db: StreakDatabase): TaskDao = db.taskDao()
    @Provides fun provideBackupDao(db: StreakDatabase): BackupDao = db.backupDao()
    @Provides fun provideGameDao(db: StreakDatabase): GameDao = db.gameDao()
    @Provides fun provideStatsDao(db: StreakDatabase): StatsDao = db.statsDao()
}
