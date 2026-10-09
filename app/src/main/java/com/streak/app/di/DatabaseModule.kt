package com.streak.app.di

import android.content.Context
import androidx.room.Room
import com.streak.app.data.local.SeedCallback
import com.streak.app.data.local.StreakDatabase
import com.streak.app.data.local.dao.HabitDao
import com.streak.app.data.local.dao.HabitLogDao
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
    fun provideDatabase(@ApplicationContext context: Context): StreakDatabase =
        Room.databaseBuilder(context, StreakDatabase::class.java, StreakDatabase.NAME)
            .addCallback(SeedCallback())
            .build()

    @Provides fun provideHabitDao(db: StreakDatabase): HabitDao = db.habitDao()
    @Provides fun provideHabitLogDao(db: StreakDatabase): HabitLogDao = db.habitLogDao()
    @Provides fun provideTaskDao(db: StreakDatabase): TaskDao = db.taskDao()
}
