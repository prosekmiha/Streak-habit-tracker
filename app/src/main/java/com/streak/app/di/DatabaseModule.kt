package com.streak.app.di

import android.content.Context
import androidx.room.Room
import com.streak.app.data.local.DatabaseGuard
import com.streak.app.data.local.StreakDatabase
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
        guard: DatabaseGuard
    ): StreakDatabase {
        guard.prepareForOpen(StreakDatabase.NAME, StreakDatabase.VERSION)
        return Room.databaseBuilder(context, StreakDatabase::class.java, StreakDatabase.NAME)
            .addMigrations(*StreakDatabase.MIGRATIONS)
            // Edini dovoljeni destruktivni prehod: iz stare v1 brez izvozene sheme.
            // Splosnega destruktivnega fallbacka NI - manjkajoca migracija je napaka.
            .fallbackToDestructiveMigrationFrom(true, StreakDatabase.LEGACY_VERSION)
            .build()
    }

    @Provides fun provideTaskDao(db: StreakDatabase): TaskDao = db.taskDao()
}
