package com.tfg.kaifit_pal.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.tfg.kaifit_pal.R
import com.tfg.kaifit_pal.domain.Sex

/**
 * Two segmented buttons, Hombre and Mujer, of which at most one is selected.
 *
 * @param selected the chosen sex, or `null` while the user has not chosen one.
 */
@Composable
fun SexSelector(
    selected: Sex?,
    onSelect: (Sex) -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        Sex.entries.forEachIndexed { index, sex ->
            SegmentedButton(
                selected = sex == selected,
                onClick = { onSelect(sex) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = Sex.entries.size),
                label = { Text(stringResource(sex.labelRes)) },
            )
        }
    }
}

/** Name of this sex shown to the user. */
@get:StringRes
val Sex.labelRes: Int
    get() = when (this) {
        Sex.MALE -> R.string.sex_male
        Sex.FEMALE -> R.string.sex_female
    }
