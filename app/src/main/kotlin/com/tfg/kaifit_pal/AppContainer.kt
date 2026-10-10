package com.tfg.kaifit_pal

import android.content.Context
import androidx.datastore.preferences.preferencesDataStoreFile
import com.tfg.kaifit_pal.data.createPreferencesDataStore
import com.tfg.kaifit_pal.data.llm.AndroidDeviceInfo
import com.tfg.kaifit_pal.data.llm.AndroidSystemDownloads
import com.tfg.kaifit_pal.data.llm.DownloadModelManager
import com.tfg.kaifit_pal.data.llm.ModelManager
import com.tfg.kaifit_pal.data.llm.ModelSpec
import com.tfg.kaifit_pal.data.profile.ProfileRepository
import com.tfg.kaifit_pal.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.File

/**
 * Manual dependency injection: builds the app-wide objects on first use and keeps one instance
 * of each.
 *
 * A DataStore must be the only one open on its file in the process, so the DataStores are
 * created here and nowhere else: one file per repository, `settings` and `profile`, under
 * `files/datastore/`. Clearing the profile therefore never touches the settings.
 *
 * The on-device model is also app-wide: one [modelManager] tracks its download or import, so a
 * transfer goes on when the user leaves the screen that started it.
 *
 * @param context any context; only the application context is kept, never an activity.
 * @param scope lifetime of the DataStores and of the model transfers. The app never cancels it;
 *   tests pass their own scope and cancel it to release the files.
 */
class AppContainer(
    context: Context,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {

    private val appContext: Context = context.applicationContext

    /** Scope that outlives every screen, for work that must finish after leaving one. */
    val applicationScope: CoroutineScope
        get() = scope

    /** Theme, dynamic colour, inference backend and Wi-Fi-only download. */
    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(createPreferencesDataStore(scope) { appContext.preferencesDataStoreFile(SETTINGS_FILE) })
    }

    /** Last saved measurement and result. */
    val profileRepository: ProfileRepository by lazy {
        ProfileRepository(createPreferencesDataStore(scope) { appContext.preferencesDataStoreFile(PROFILE_FILE) })
    }

    /**
     * Download, import and deletion of the on-device model, kept in the external files directory
     * (internal storage if there is none).
     */
    val modelManager: ModelManager by lazy {
        DownloadModelManager(
            modelsDir = appContext.getExternalFilesDir(ModelSpec.MODELS_DIR)
                ?: File(appContext.filesDir, ModelSpec.MODELS_DIR),
            downloads = AndroidSystemDownloads(appContext),
            deviceInfo = AndroidDeviceInfo(appContext),
            scope = scope,
            ioDispatcher = Dispatchers.IO,
            downloadTitle = appContext.getString(R.string.model_download_title),
            downloadDescription = appContext.getString(R.string.model_download_description),
        )
    }

    internal companion object {
        /** Name of the settings DataStore file, without its `.preferences_pb` extension. */
        const val SETTINGS_FILE = "settings"

        /** Name of the profile DataStore file, without its `.preferences_pb` extension. */
        const val PROFILE_FILE = "profile"
    }
}
