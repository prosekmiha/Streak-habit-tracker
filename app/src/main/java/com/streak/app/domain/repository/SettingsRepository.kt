package com.streak.app.domain.repository

import com.streak.app.domain.model.AppSettings
import com.streak.app.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

interface SettingsRepository {
    val settings: Flow<AppSettings>

    suspend fun update(transform: (AppSettings) -> AppSettings)

    val userName: Flow<String> get() = settings.map { it.userName }.distinctUntilChanged()
    val themeMode: Flow<ThemeMode> get() = settings.map { it.themeMode }.distinctUntilChanged()

    suspend fun setUserName(name: String) = update { it.copy(userName = name) }
    suspend fun setThemeMode(mode: ThemeMode) = update { it.copy(themeMode = mode) }
}
