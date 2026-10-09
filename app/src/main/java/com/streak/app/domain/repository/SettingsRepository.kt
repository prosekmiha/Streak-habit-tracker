package com.streak.app.domain.repository

import com.streak.app.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val userName: Flow<String>
    val themeMode: Flow<ThemeMode>
    suspend fun setUserName(name: String)
    suspend fun setThemeMode(mode: ThemeMode)
}
