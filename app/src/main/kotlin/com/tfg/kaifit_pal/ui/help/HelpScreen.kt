package com.tfg.kaifit_pal.ui.help

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.tfg.kaifit_pal.R
import com.tfg.kaifit_pal.ui.components.PlaceholderScreen
import com.tfg.kaifit_pal.ui.theme.KaiFitTheme

@Composable
fun HelpScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        title = stringResource(R.string.help_title),
        body = stringResource(R.string.help_placeholder),
        modifier = modifier,
    )
}

@PreviewLightDark
@Composable
private fun HelpScreenPreview() {
    KaiFitTheme {
        HelpScreen()
    }
}
