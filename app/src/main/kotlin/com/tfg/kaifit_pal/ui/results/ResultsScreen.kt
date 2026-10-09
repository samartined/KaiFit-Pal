package com.tfg.kaifit_pal.ui.results

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.tfg.kaifit_pal.R
import com.tfg.kaifit_pal.ui.components.PlaceholderScreen
import com.tfg.kaifit_pal.ui.navigation.ResultsRoute
import com.tfg.kaifit_pal.ui.theme.KaiFitTheme
import com.tfg.kaifit_pal.ui.theme.withTabularFigures

/** Detail destination reached from the calculator; [onBack] returns to it. */
@Composable
fun ResultsScreen(
    route: ResultsRoute,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PlaceholderScreen(
        title = stringResource(R.string.results_title),
        body = stringResource(R.string.results_placeholder),
        modifier = modifier,
        onBack = onBack,
    ) {
        Text(
            text = stringResource(R.string.results_tdee_value, route.tdee),
            style = MaterialTheme.typography.displayMedium.withTabularFigures(),
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@PreviewLightDark
@Composable
private fun ResultsScreenPreview() {
    KaiFitTheme {
        ResultsScreen(
            route = ResultsRoute(tdee = 2716, bodyFatPercent = 20f, method = "KATCH_MCARDLE"),
            onBack = {},
        )
    }
}
