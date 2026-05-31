package com.vontext.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.vontext.processor.whisper.WhisperMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

data class Settings(
    val defaultWhisperMode: WhisperMode = WhisperMode.LOCAL_SMALL,
    val defaultFrameInterval: Int = 0,
    val openaiApiKey: String? = null,
    val openaiBaseUrl: String? = null,
    val whisperModel: String = "whisper-small",
    val maxVideoSizeMb: Int = 2048,
    val cleanupTempAfterHours: Int = 24,
    val cleanupOutputAfterHours: Int = 48,
    val notificationsEnabled: Boolean = true,
    val language: String = "es"
)

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val DEFAULT_WHISPER_MODE = stringPreferencesKey("default_whisper_mode")
        val DEFAULT_FRAME_INTERVAL = intPreferencesKey("default_frame_interval")
        val OPENAI_API_KEY = stringPreferencesKey("openai_api_key")
        val OPENAI_BASE_URL = stringPreferencesKey("openai_base_url")
        val WHISPER_MODEL = stringPreferencesKey("whisper_model")
        val MAX_VIDEO_SIZE_MB = intPreferencesKey("max_video_size_mb")
        val CLEANUP_TEMP_HOURS = intPreferencesKey("cleanup_temp_hours")
        val CLEANUP_OUTPUT_HOURS = intPreferencesKey("cleanup_output_hours")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val LANGUAGE = stringPreferencesKey("language")
    }

    val settings: Flow<Settings> = context.dataStore.data.map { prefs ->
        Settings(
            defaultWhisperMode = try {
                WhisperMode.valueOf(prefs[Keys.DEFAULT_WHISPER_MODE] ?: "LOCAL_SMALL")
            } catch (_: Exception) { WhisperMode.LOCAL_SMALL },
            defaultFrameInterval = prefs[Keys.DEFAULT_FRAME_INTERVAL] ?: 0,
            openaiApiKey = prefs[Keys.OPENAI_API_KEY],
            openaiBaseUrl = prefs[Keys.OPENAI_BASE_URL],
            whisperModel = prefs[Keys.WHISPER_MODEL] ?: "whisper-small",
            maxVideoSizeMb = prefs[Keys.MAX_VIDEO_SIZE_MB] ?: 2048,
            cleanupTempAfterHours = prefs[Keys.CLEANUP_TEMP_HOURS] ?: 24,
            cleanupOutputAfterHours = prefs[Keys.CLEANUP_OUTPUT_HOURS] ?: 48,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
            language = prefs[Keys.LANGUAGE] ?: "es"
        )
    }

    suspend fun updateWhisperMode(mode: WhisperMode) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DEFAULT_WHISPER_MODE] = mode.name
        }
    }

    suspend fun updateApiKey(key: String?) {
        context.dataStore.edit { prefs ->
            if (key != null) prefs[Keys.OPENAI_API_KEY] = key
            else prefs.remove(Keys.OPENAI_API_KEY)
        }
    }

    suspend fun updateBaseUrl(url: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.OPENAI_BASE_URL] = url
        }
    }

    suspend fun updateWhisperModel(model: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.WHISPER_MODEL] = model
        }
    }

    suspend fun updateFrameInterval(interval: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DEFAULT_FRAME_INTERVAL] = interval
        }
    }

    suspend fun updateLanguage(language: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LANGUAGE] = language
        }
    }
}
