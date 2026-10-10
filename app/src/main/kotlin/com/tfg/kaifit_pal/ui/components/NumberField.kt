package com.tfg.kaifit_pal.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType

/**
 * Text field for a decimal measurement, with the decimal keyboard and a unit suffix.
 *
 * Keystrokes that would not leave a valid partial number ([acceptDecimalInput]) are ignored, so
 * [onValueChange] only receives digits with at most one comma or dot. The text is kept as typed,
 * including an unfinished "80,": it is parsed with [parseDecimal] by whoever owns the state.
 *
 * @param unit unit shown after the value, such as "cm" or "kg".
 * @param hint text shown under the field while it has no error, such as the accepted range. It
 *   also keeps the unit visible when the field is empty, since the suffix only appears once the
 *   field has a value or the focus.
 * @param errorMessage message shown under the field instead of [hint], which marks the field as
 *   wrong; `null` when the value is fine.
 * @param imeAction action of the keyboard key, such as moving to the next field.
 */
@Composable
fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    unit: String,
    modifier: Modifier = Modifier,
    hint: String? = null,
    errorMessage: String? = null,
    imeAction: ImeAction = ImeAction.Next,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { text -> if (acceptDecimalInput(text)) onValueChange(text) },
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        suffix = { Text(unit) },
        supportingText = (errorMessage ?: hint)?.let { message -> { Text(message) } },
        isError = errorMessage != null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = imeAction),
        keyboardActions = keyboardActions,
        singleLine = true,
    )
}
