package com.tfg.kaifit_pal

import android.app.Application
import android.content.ComponentCallbacks2

/**
 * Application class, which owns the [AppContainer] of the process.
 *
 * ViewModels get the repositories through `(application as KaiFitApp).container`. The container,
 * and with it every data file, is only created on first use, so starting the app does no disk
 * work here.
 */
class KaiFitApp : Application() {

    /** App-wide dependencies, created on first access. */
    val container: AppContainer by lazy { AppContainer(this) }

    /**
     * Frees the language model once the app is in the background and the system needs memory.
     * A short trip to another app (only [ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN]) keeps it, so
     * coming back does not wait for the model to load again.
     */
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND) {
            container.releaseLanguageModel()
        }
    }
}
