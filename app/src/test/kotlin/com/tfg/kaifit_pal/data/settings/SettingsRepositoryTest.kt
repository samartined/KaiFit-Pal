package com.tfg.kaifit_pal.data.settings

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.tfg.kaifit_pal.data.FailingDataStore
import com.tfg.kaifit_pal.data.closeableScope
import com.tfg.kaifit_pal.data.close
import com.tfg.kaifit_pal.data.newPreferencesFile
import com.tfg.kaifit_pal.data.preferencesDataStoreOn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException

class SettingsRepositoryTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun TestScope.newDataStore() = backgroundScope.preferencesDataStoreOn(tmp.newPreferencesFile())

    private fun TestScope.newRepository() = SettingsRepository(newDataStore())

    @Test
    fun emptyStoreGivesDefaultSettings() = runTest {
        val settings = newRepository().settings.first()

        assertEquals(ThemeMode.SYSTEM, settings.themeMode)
        assertFalse(settings.dynamicColor)
        assertEquals(InferenceBackend.GPU, settings.inferenceBackend)
        assertTrue(settings.downloadOnWifiOnly)
        assertEquals(AppSettings.DEFAULT, settings)
    }

    @Test
    fun setThemeModeStoresItsNameAndKeepsTheOtherSettings() = runTest {
        val dataStore = newDataStore()
        dataStore.edit {
            it[SettingsKeys.DYNAMIC_COLOR] = true
            it[SettingsKeys.INFERENCE_BACKEND] = "CPU"
            it[SettingsKeys.DOWNLOAD_ON_WIFI_ONLY] = false
        }
        val repository = SettingsRepository(dataStore)

        repository.setThemeMode(ThemeMode.DARK)

        assertEquals(AppSettings(ThemeMode.DARK, true, InferenceBackend.CPU, false), repository.settings.first())
        assertEquals("DARK", dataStore.data.first()[SettingsKeys.THEME_MODE])
    }

    @Test
    fun setDynamicColorKeepsTheOtherSettings() = runTest {
        val dataStore = newDataStore()
        dataStore.edit {
            it[SettingsKeys.THEME_MODE] = "DARK"
            it[SettingsKeys.INFERENCE_BACKEND] = "CPU"
            it[SettingsKeys.DOWNLOAD_ON_WIFI_ONLY] = false
        }
        val repository = SettingsRepository(dataStore)

        repository.setDynamicColor(true)

        assertEquals(AppSettings(ThemeMode.DARK, true, InferenceBackend.CPU, false), repository.settings.first())
    }

    @Test
    fun setInferenceBackendStoresItsNameAndKeepsTheOtherSettings() = runTest {
        val dataStore = newDataStore()
        dataStore.edit {
            it[SettingsKeys.THEME_MODE] = "LIGHT"
            it[SettingsKeys.DYNAMIC_COLOR] = true
            it[SettingsKeys.DOWNLOAD_ON_WIFI_ONLY] = false
        }
        val repository = SettingsRepository(dataStore)

        repository.setInferenceBackend(InferenceBackend.CPU)

        assertEquals(AppSettings(ThemeMode.LIGHT, true, InferenceBackend.CPU, false), repository.settings.first())
        assertEquals("CPU", dataStore.data.first()[SettingsKeys.INFERENCE_BACKEND])
    }

    @Test
    fun setDownloadOnWifiOnlyKeepsTheOtherSettings() = runTest {
        val dataStore = newDataStore()
        dataStore.edit {
            it[SettingsKeys.THEME_MODE] = "LIGHT"
            it[SettingsKeys.DYNAMIC_COLOR] = true
            it[SettingsKeys.INFERENCE_BACKEND] = "CPU"
        }
        val repository = SettingsRepository(dataStore)

        repository.setDownloadOnWifiOnly(false)

        assertEquals(AppSettings(ThemeMode.LIGHT, true, InferenceBackend.CPU, false), repository.settings.first())
    }

    @Test
    fun allSettingsChangedTogetherAreReadBack() = runTest {
        val repository = newRepository()

        repository.setThemeMode(ThemeMode.LIGHT)
        repository.setDynamicColor(true)
        repository.setInferenceBackend(InferenceBackend.CPU)
        repository.setDownloadOnWifiOnly(false)

        assertEquals(AppSettings(ThemeMode.LIGHT, true, InferenceBackend.CPU, false), repository.settings.first())
    }

    @Test
    fun settingsSurviveReopeningTheFile() = runTest {
        val file = tmp.newPreferencesFile()
        val firstScope = closeableScope()
        val first = SettingsRepository(firstScope.preferencesDataStoreOn(file))
        first.setThemeMode(ThemeMode.DARK)
        first.setDynamicColor(true)
        first.setInferenceBackend(InferenceBackend.CPU)
        first.setDownloadOnWifiOnly(false)
        firstScope.close()

        val reopened = SettingsRepository(backgroundScope.preferencesDataStoreOn(file)).settings.first()

        assertEquals(AppSettings(ThemeMode.DARK, true, InferenceBackend.CPU, false), reopened)
    }

    @Test
    fun unknownEnumNamesFallBackToDefaults() = runTest {
        val dataStore = newDataStore()
        dataStore.edit {
            it[SettingsKeys.THEME_MODE] = "PURPLE"
            it[SettingsKeys.INFERENCE_BACKEND] = "NPU"
            it[SettingsKeys.DYNAMIC_COLOR] = true
        }

        val settings = SettingsRepository(dataStore).settings.first()

        assertEquals(ThemeMode.SYSTEM, settings.themeMode)
        assertEquals(InferenceBackend.GPU, settings.inferenceBackend)
        assertTrue(settings.dynamicColor)
    }

    @Test
    fun valuesOfAnotherTypeFallBackToDefaults() = runTest {
        val dataStore = newDataStore()
        dataStore.edit {
            it[intPreferencesKey("theme_mode")] = 2
            it[stringPreferencesKey("dynamic_color")] = "true"
            it[booleanPreferencesKey("inference_backend")] = true
            it[stringPreferencesKey("download_on_wifi_only")] = "false"
        }

        val settings = SettingsRepository(dataStore).settings.first()

        assertEquals(AppSettings(ThemeMode.SYSTEM, false, InferenceBackend.GPU, true), settings)
    }

    @Test
    fun readErrorGivesDefaultSettings() = runTest {
        val repository = SettingsRepository(FailingDataStore(IOException("disk unavailable")))

        assertEquals(AppSettings.DEFAULT, repository.settings.first())
    }

    @Test
    fun errorOtherThanIoReachesTheCollector() = runTest {
        val repository = SettingsRepository(FailingDataStore(IllegalStateException("bug")))

        try {
            repository.settings.first()
            fail("IllegalStateException expected")
        } catch (e: IllegalStateException) {
            assertEquals("bug", e.message)
        }
    }
}
