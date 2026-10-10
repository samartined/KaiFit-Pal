package com.tfg.kaifit_pal.ui.components

import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/*
 * Reading and writing the decimal numbers of the measurement fields.
 *
 * Users may type either a comma or a dot as the decimal separator, whatever the locale of the
 * device: "80,5" and "80.5" are both 80.5. A locale-aware parser cannot do this, because with
 * a Spanish locale it reads the dot as the grouping separator ("80.5" would become 805). The
 * locale is only used to write numbers back into a field.
 */

/** A number with digits before an optional separator, or a separator followed by digits. */
private val DECIMAL_NUMBER = Regex("""\d+([.,]\d*)?|[.,]\d+""")

/** Partial input accepted while typing: up to 3 whole digits, one separator and 2 decimals. */
private val DECIMAL_INPUT = Regex("""\d{0,3}([.,]\d{0,2})?""")

/**
 * The number in [text], with a comma or a dot as the decimal separator, or `null` if [text] is
 * blank, is not a plain decimal number (signs, exponents, grouping separators, `NaN` and
 * `Infinity` are rejected) or is not finite.
 */
fun parseDecimal(text: String): Double? {
    val trimmed = text.trim()
    if (!DECIMAL_NUMBER.matches(trimmed)) return null
    return trimmed.replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }
}

/**
 * Whether a field may take [text] as its new content: only digits, at most one separator
 * (comma or dot), up to 3 digits before it and up to 2 after it. Empty text is accepted, so the
 * field can be cleared.
 */
fun acceptDecimalInput(text: String): Boolean = DECIMAL_INPUT.matches(text)

/**
 * [value] written for a field: the decimal separator of [locale] (a comma or a dot, the only two
 * that [parseDecimal] reads back), no grouping, at most 2 decimals and no trailing zeros. For
 * example 80.0 gives "80" and 80.5 gives "80,5" in Spanish.
 */
fun formatDecimalInput(value: Double, locale: Locale): String {
    val format = DecimalFormat("0.##", DecimalFormatSymbols(Locale.ROOT)).apply {
        roundingMode = RoundingMode.HALF_UP
    }
    val separator = if (DecimalFormatSymbols.getInstance(locale).decimalSeparator == ',') ',' else '.'
    return format.format(value).replace('.', separator)
}
