package com.tfg.kaifit_pal.data.settings

/** Colour scheme the app uses: the one of the system, or always light or dark. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Hardware that runs the on-device language model. */
enum class InferenceBackend { GPU, CPU }

/**
 * User preferences, with the values used until the user changes them.
 *
 * @property themeMode colour scheme; follows the system by default.
 * @property dynamicColor whether to use the wallpaper colours (Android 12+) instead of the brand
 *   green; off by default.
 * @property inferenceBackend hardware for the language model; GPU by default, with CPU as the
 *   alternative when the GPU fails.
 * @property downloadOnWifiOnly whether the model download waits for an unmetered network; on by
 *   default, because the model takes about 2.6 GB.
 */
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
    val inferenceBackend: InferenceBackend = InferenceBackend.GPU,
    val downloadOnWifiOnly: Boolean = true,
) {

    companion object {
        /** Settings of a fresh install. */
        val DEFAULT = AppSettings()
    }
}
