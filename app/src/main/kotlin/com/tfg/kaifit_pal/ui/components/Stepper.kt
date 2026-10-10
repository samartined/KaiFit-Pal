package com.tfg.kaifit_pal.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tfg.kaifit_pal.R
import com.tfg.kaifit_pal.ui.theme.Spacing
import com.tfg.kaifit_pal.ui.theme.withTabularFigures

/**
 * A whole number changed one unit at a time with − and + buttons, such as the age.
 *
 * Each button is 48 dp and is disabled at its end of [range], so the value never leaves it.
 * The value uses tabular figures, so its width does not jump while it changes.
 *
 * @param valueText [value] as shown to the user, for example "30 años".
 * @param decreaseDescription spoken name of the − button.
 * @param increaseDescription spoken name of the + button.
 */
@Composable
fun Stepper(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    valueText: String,
    decreaseDescription: String,
    increaseDescription: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.l, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilledTonalIconButton(
            onClick = { onValueChange(value - 1) },
            enabled = value > range.first,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(painterResource(R.drawable.ic_remove), contentDescription = decreaseDescription)
        }
        Text(
            text = valueText,
            modifier = Modifier
                .widthIn(min = 96.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
            style = MaterialTheme.typography.titleLarge.withTabularFigures(),
            textAlign = TextAlign.Center,
        )
        FilledTonalIconButton(
            onClick = { onValueChange(value + 1) },
            enabled = value < range.last,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(painterResource(R.drawable.ic_add), contentDescription = increaseDescription)
        }
    }
}
