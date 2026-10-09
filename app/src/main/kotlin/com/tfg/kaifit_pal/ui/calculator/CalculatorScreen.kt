package com.tfg.kaifit_pal.ui.calculator

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.tfg.kaifit_pal.R
import com.tfg.kaifit_pal.ui.components.PlaceholderScreen
import com.tfg.kaifit_pal.ui.theme.KaiFitTheme

/**
 * @param onShowResults opens Results with sample values. Temporary: it goes away when the real
 * calculator form replaces this placeholder.
 */
@Composable
fun CalculatorScreen(
    onShowResults: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PlaceholderScreen(
        title = stringResource(R.string.calculator_title),
        body = stringResource(R.string.calculator_placeholder),
        modifier = modifier,
    ) {
        Button(onClick = onShowResults) {
            Text(stringResource(R.string.calculator_show_sample_results))
        }
    }
}

@PreviewLightDark
@Composable
private fun CalculatorScreenPreview() {
    KaiFitTheme {
        CalculatorScreen(onShowResults = {})
    }
}
