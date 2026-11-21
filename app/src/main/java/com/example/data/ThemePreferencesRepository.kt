package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AppThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "amdv_tune_prefs")

class ThemePreferencesRepository(private val context: Context) {
    private val themeKey = stringPreferencesKey("app_theme_mode")
    private val autoContinueKey = booleanPreferencesKey("playback_auto_continue")
    private val muteOnNotificationsKey = booleanPreferencesKey("playback_mute_on_notifications_v2")

    val themeModeFlow: Flow<AppThemeMode> = context.themeDataStore.data.map { prefs ->
        AppThemeMode.fromKey(prefs[themeKey])
    }

    val autoContinueFlow: Flow<Boolean> = context.themeDataStore.data.map { prefs ->
        prefs[autoContinueKey] ?: true
    }

    val muteOnNotificationsFlow: Flow<Boolean> = context.themeDataStore.data.map { prefs ->
        prefs[muteOnNotificationsKey] ?: false
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.themeDataStore.edit { prefs ->
            prefs[themeKey] = mode.key
        }
    }

    suspend fun setAutoContinue(enabled: Boolean) {
        context.themeDataStore.edit { prefs ->
            prefs[autoContinueKey] = enabled
        }
    }

    suspend fun setMuteOnNotifications(enabled: Boolean) {
        context.themeDataStore.edit { prefs ->
            prefs[muteOnNotificationsKey] = enabled
        }
    }
}
