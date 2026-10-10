package com.tfg.kaifit_pal

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tfg.kaifit_pal.data.settings.AppSettings
import com.tfg.kaifit_pal.ui.MainUiState
import com.tfg.kaifit_pal.ui.MainViewModel
import com.tfg.kaifit_pal.ui.navigation.KaiFitNavHost
import com.tfg.kaifit_pal.ui.theme.KaiFitTheme
import com.tfg.kaifit_pal.ui.theme.isDark

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels { MainViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must run before super.onCreate: it swaps the splash theme for the app theme.
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        // Keep the splash screen until the saved theme is known, so the app never flashes the
        // system theme first.
        splashScreen.setKeepOnScreenCondition { viewModel.uiState.value is MainUiState.Loading }
        enableEdgeToEdge()
        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val settings = (uiState as? MainUiState.Ready)?.settings ?: AppSettings.DEFAULT
            val darkTheme = settings.themeMode.isDark(isSystemInDarkTheme())

            // The system bar icons must follow the theme chosen in the app, which can differ from
            // the system one: light icons on the dark theme and dark icons on the light theme.
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(LIGHT_NAVIGATION_SCRIM, DARK_NAVIGATION_SCRIM) { darkTheme },
                )
                onDispose {}
            }

            KaiFitTheme(darkTheme = darkTheme, dynamicColor = settings.dynamicColor) {
                KaiFitNavHost()
            }
        }
    }

    private companion object {
        // Scrims of the three-button navigation bar, the same ones enableEdgeToEdge() uses by default.
        val LIGHT_NAVIGATION_SCRIM = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
        val DARK_NAVIGATION_SCRIM = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
    }
}
