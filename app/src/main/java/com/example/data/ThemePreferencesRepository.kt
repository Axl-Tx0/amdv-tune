package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AppThemeMode
import com.example.model.EqualizerPreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "amdv_tune_prefs")

data class PersistedEqualizerConfig(
    val enabled: Boolean = false,
    val preset: EqualizerPreset = EqualizerPreset.NORMAL,
    val bandLevelsMb: List<Int> = EqualizerPreset.NORMAL.bandLevelsMb,
    val bassBoostStrength: Int = 0
)

class ThemePreferencesRepository(private val context: Context) {
    private val themeKey = stringPreferencesKey("app_theme_mode")
    private val autoContinueKey = booleanPreferencesKey("playback_auto_continue")
    private val muteOnNotificationsKey = booleanPreferencesKey("playback_mute_on_notifications_v2")
    private val eqEnabledKey = booleanPreferencesKey("audio_eq_enabled")
    private val eqPresetKey = stringPreferencesKey("audio_eq_preset")
    private val eqBandsKey = stringPreferencesKey("audio_eq_bands_csv")
    private val bassStrengthKey = intPreferencesKey("audio_bass_boost_strength")
    private val crossfadeSecondsKey = intPreferencesKey("playback_crossfade_seconds")
    private val lastSeenReleaseMsKey = longPreferencesKey("inbox_last_seen_release_ms")

    val themeModeFlow: Flow<AppThemeMode> = context.themeDataStore.data.map { prefs ->
        AppThemeMode.fromKey(prefs[themeKey])
    }

    val autoContinueFlow: Flow<Boolean> = context.themeDataStore.data.map { prefs ->
        prefs[autoContinueKey] ?: true
    }

    val muteOnNotificationsFlow: Flow<Boolean> = context.themeDataStore.data.map { prefs ->
        prefs[muteOnNotificationsKey] ?: false
    }

    val crossfadeSecondsFlow: Flow<Int> = context.themeDataStore.data.map { prefs ->
        (prefs[crossfadeSecondsKey] ?: 0).coerceIn(0, 12)
    }

    val lastSeenReleaseMsFlow: Flow<Long> = context.themeDataStore.data.map { prefs ->
        prefs[lastSeenReleaseMsKey] ?: 0L
    }

    val equalizerConfigFlow: Flow<PersistedEqualizerConfig> = context.themeDataStore.data.map { prefs ->
        val enabled = prefs[eqEnabledKey] ?: false
        val preset = EqualizerPreset.fromKey(prefs[eqPresetKey])
        val parsedBands = prefs[eqBandsKey]
            ?.split(",")
            ?.mapNotNull { it.trim().toIntOrNull() }
            ?.takeIf { it.size >= 3 }
            ?: preset.bandLevelsMb
        val bass = (prefs[bassStrengthKey] ?: preset.defaultBass).coerceIn(0, 1000)
        PersistedEqualizerConfig(
            enabled = enabled,
            preset = preset,
            bandLevelsMb = parsedBands,
            bassBoostStrength = bass
        )
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

    suspend fun saveEqualizerConfig(config: PersistedEqualizerConfig) {
        context.themeDataStore.edit { prefs ->
            prefs[eqEnabledKey] = config.enabled
            prefs[eqPresetKey] = config.preset.key
            prefs[eqBandsKey] = config.bandLevelsMb.joinToString(",")
            prefs[bassStrengthKey] = config.bassBoostStrength.coerceIn(0, 1000)
        }
    }

    suspend fun setCrossfadeSeconds(seconds: Int) {
        context.themeDataStore.edit { prefs ->
            prefs[crossfadeSecondsKey] = seconds.coerceIn(0, 12)
        }
    }

    suspend fun setLastSeenReleaseMs(timestampMs: Long) {
        context.themeDataStore.edit { prefs ->
            prefs[lastSeenReleaseMsKey] = timestampMs.coerceAtLeast(0L)
        }
    }
}
