package com.tfg.kaifit_pal.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.tfg.kaifit_pal.R
import com.tfg.kaifit_pal.ui.theme.Spacing
import com.tfg.kaifit_pal.ui.theme.withTabularFigures
import kotlin.math.roundToInt

/**
 * One macronutrient of the daily plan: its name, grams, share of the calorie target and calories.
 *
 * With [onGramsChange], a slider from 0 to [maxGrams] lets the user change the grams; the value
 * is rounded to whole grams and whoever owns the plan decides the final amount. Without it, the
 * card only shows the values (carbohydrates, which are derived from the other two).
 *
 * @param kcalPerGram energy of one gram, 4 or 9 kcal.
 * @param targetKcal calorie target the share is computed from; must be positive.
 * @param maxGrams upper end of the slider, in g.
 */
@Composable
fun MacroCard(
    name: String,
    grams: Int,
    kcalPerGram: Int,
    targetKcal: Int,
    modifier: Modifier = Modifier,
    maxGrams: Int = 0,
    onGramsChange: ((Int) -> Unit)? = null,
) {
    val kcal = grams * kcalPerGram
    val percentOfTarget = (kcal * 100.0 / targetKcal).roundToInt()
    val gramsText = stringResource(R.string.value_grams, grams)
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(Spacing.l),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) {},
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = stringResource(R.string.macro_share, percentOfTarget, kcal),
                        style = MaterialTheme.typography.bodyMedium.withTabularFigures(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = gramsText,
                    style = MaterialTheme.typography.titleLarge.withTabularFigures(),
                )
            }
            if (onGramsChange != null) {
                Slider(
                    value = grams.toFloat().coerceIn(0f, maxGrams.toFloat()),
                    onValueChange = { onGramsChange(it.roundToInt()) },
                    valueRange = 0f..maxGrams.toFloat(),
                    modifier = Modifier.semantics {
                        contentDescription = name
                        stateDescription = gramsText
                    },
                )
            }
        }
    }
}
