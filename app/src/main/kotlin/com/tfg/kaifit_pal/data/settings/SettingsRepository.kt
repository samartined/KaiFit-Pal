package com.tfg.kaifit_pal.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.tfg.kaifit_pal.data.getOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * Stores the [AppSettings] in their own Preferences [DataStore], one key per setting.
 *
 * Each setter writes only its own key, so two screens that change different settings never
 * overwrite each other.
 */
class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    /**
     * The current settings, emitted again whenever they change.
     *
     * A missing key, an unknown enum name or a value of another type gives the default of that
     * field. If the file cannot be read, the flow emits [AppSettings.DEFAULT] and completes; any
     * other error is a bug and reaches the collector.
     */
    val settings: Flow<AppSettings> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { decode(it) }
        .distinctUntilChanged()

    /** Stores the colour scheme. */
    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[SettingsKeys.THEME_MODE] = mode.name }
    }

    /** Turns the wallpaper colours on or off. */
    suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { it[SettingsKeys.DYNAMIC_COLOR] = enabled }
    }

    /** Stores the hardware that runs the language model. */
    suspend fun setInferenceBackend(backend: InferenceBackend) {
        dataStore.edit { it[SettingsKeys.INFERENCE_BACKEND] = backend.name }
    }

    /** Turns the Wi-Fi-only restriction of the model download on or off. */
    suspend fun setDownloadOnWifiOnly(enabled: Boolean) {
        dataStore.edit { it[SettingsKeys.DOWNLOAD_ON_WIFI_ONLY] = enabled }
    }

    private fun decode(prefs: Preferences): AppSettings {
        val defaults = AppSettings.DEFAULT
        val themeName = prefs.getOrNull(SettingsKeys.THEME_MODE)
        val backendName = prefs.getOrNull(SettingsKeys.INFERENCE_BACKEND)
        return AppSettings(
            themeMode = ThemeMode.entries.firstOrNull { it.name == themeName } ?: defaults.themeMode,
            dynamicColor = prefs.getOrNull(SettingsKeys.DYNAMIC_COLOR) ?: defaults.dynamicColor,
            inferenceBackend = InferenceBackend.entries.firstOrNull { it.name == backendName }
                ?: defaults.inferenceBackend,
            downloadOnWifiOnly = prefs.getOrNull(SettingsKeys.DOWNLOAD_ON_WIFI_ONLY) ?: defaults.downloadOnWifiOnly,
        )
    }
}

/** Keys of the settings file. Enums are stored by name, so reordering their values is safe. */
internal object SettingsKeys {
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
    val INFERENCE_BACKEND = stringPreferencesKey("inference_backend")
    val DOWNLOAD_ON_WIFI_ONLY = booleanPreferencesKey("download_on_wifi_only")
}
