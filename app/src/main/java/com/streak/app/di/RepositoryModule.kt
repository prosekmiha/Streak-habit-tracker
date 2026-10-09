package com.streak.app.di

import com.streak.app.data.prefs.SettingsDataStore
import com.streak.app.data.remote.RemoteDataSource
import com.streak.app.data.remote.SupabaseRemoteDataSource
import com.streak.app.data.repository.HabitRepositoryImpl
import com.streak.app.data.repository.TaskRepositoryImpl
import com.streak.app.domain.repository.HabitRepository
import com.streak.app.domain.repository.SettingsRepository
import com.streak.app.domain.repository.TaskRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindHabitRepository(impl: HabitRepositoryImpl): HabitRepository

    @Binds @Singleton
    abstract fun bindTaskRepository(impl: TaskRepositoryImpl): TaskRepository

    @Binds @Singleton
    abstract fun bindSettingsRepository(impl: SettingsDataStore): SettingsRepository

    @Binds @Singleton
    abstract fun bindRemoteDataSource(impl: SupabaseRemoteDataSource): RemoteDataSource
}
