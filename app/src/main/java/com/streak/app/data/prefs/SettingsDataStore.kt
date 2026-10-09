package com.streak.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.streak.app.domain.model.ThemeMode
import com.streak.app.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {

    private object Keys {
        val NAME = stringPreferencesKey("user_name")
        val THEME = stringPreferencesKey("theme_mode")
    }

    override val userName: Flow<String> =
        context.dataStore.data.map { it[Keys.NAME] ?: "" }

    override val themeMode: Flow<ThemeMode> =
        context.dataStore.data.map {
            ThemeMode.valueOf(it[Keys.THEME] ?: ThemeMode.DARK.name)
        }

    override suspend fun setUserName(name: String) {
        context.dataStore.edit { it[Keys.NAME] = name }
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME] = mode.name }
    }
}
