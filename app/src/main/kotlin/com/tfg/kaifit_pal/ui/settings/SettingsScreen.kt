package com.tfg.kaifit_pal.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.tfg.kaifit_pal.R
import com.tfg.kaifit_pal.ui.components.PlaceholderScreen
import com.tfg.kaifit_pal.ui.theme.KaiFitTheme

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        title = stringResource(R.string.settings_title),
        body = stringResource(R.string.settings_placeholder),
        modifier = modifier,
    )
}

@PreviewLightDark
@Composable
private fun SettingsScreenPreview() {
    KaiFitTheme {
        SettingsScreen()
    }
}
