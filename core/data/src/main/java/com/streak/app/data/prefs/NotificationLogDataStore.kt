package com.streak.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.streak.app.domain.notify.NotificationLog
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

private val Context.notificationLog: DataStore<Preferences> by preferencesDataStore(name = "notification_log")

@Singleton
class NotificationLogDataStore @Inject constructor(@ApplicationContext private val context: Context) : NotificationLog {

    private val dayKey = longPreferencesKey("day")
    private val countKey = intPreferencesKey("count")

    override suspend fun tryConsume(today: LocalDate, cap: Int): Boolean {
        var allowed = false
        context.notificationLog.edit { p ->
            val count = if (p[dayKey] == today.toEpochDay()) p[countKey] ?: 0 else 0
            allowed = cap <= 0 || count < cap
            if (allowed) {
                p[dayKey] = today.toEpochDay()
                p[countKey] = count + 1
            }
        }
        return allowed
    }
}
