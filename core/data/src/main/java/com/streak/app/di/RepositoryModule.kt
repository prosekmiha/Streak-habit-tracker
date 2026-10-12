package com.streak.app.di

import com.streak.app.data.prefs.GameDataStore
import com.streak.app.data.prefs.NotificationLogDataStore
import com.streak.app.data.prefs.SessionDataStore
import com.streak.app.data.prefs.SettingsDataStore
import com.streak.app.data.repository.GameRepositoryImpl
import com.streak.app.data.repository.HabitRepositoryImpl
import com.streak.app.data.repository.MoodRepositoryImpl
import com.streak.app.data.repository.TaskRepositoryImpl
import com.streak.app.data.time.SystemDayClock
import com.streak.app.domain.notify.NotificationLog
import com.streak.app.domain.repository.GameMemoryRepository
import com.streak.app.domain.repository.GameRepository
import com.streak.app.domain.repository.HabitRepository
import com.streak.app.domain.repository.MoodRepository
import com.streak.app.domain.repository.SettingsRepository
import com.streak.app.domain.repository.TaskRepository
import com.streak.app.domain.session.ActiveSessionStore
import com.streak.app.domain.time.DayClock
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// Podatkovna plast: domenski vmesniki -> Room / DataStore
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindHabitRepository(impl: HabitRepositoryImpl): HabitRepository

    @Binds @Singleton
    abstract fun bindTaskRepository(impl: TaskRepositoryImpl): TaskRepository

    @Binds @Singleton
    abstract fun bindMoodRepository(impl: MoodRepositoryImpl): MoodRepository

    @Binds @Singleton
    abstract fun bindSettingsRepository(impl: SettingsDataStore): SettingsRepository

    @Binds @Singleton
    abstract fun bindGameRepository(impl: GameRepositoryImpl): GameRepository

    @Binds @Singleton
    abstract fun bindGameMemoryRepository(impl: GameDataStore): GameMemoryRepository

    @Binds @Singleton
    abstract fun bindActiveSessionStore(impl: SessionDataStore): ActiveSessionStore

    @Binds @Singleton
    abstract fun bindNotificationLog(impl: NotificationLogDataStore): NotificationLog

    @Binds @Singleton
    abstract fun bindDayClock(impl: SystemDayClock): DayClock
}
