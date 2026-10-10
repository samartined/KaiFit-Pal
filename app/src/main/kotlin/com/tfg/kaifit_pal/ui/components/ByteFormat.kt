package com.tfg.kaifit_pal.ui.components

import java.util.Locale

/** Bytes in a decimal gigabyte, as storage sizes are shown to users. */
private const val BYTES_PER_GIGABYTE = 1_000_000_000.0

/** [bytes] in decimal gigabytes with one decimal, without the unit: 1.100.000.000 → "1,1" in Spanish. */
fun formatGigabytesValue(bytes: Long, locale: Locale): String =
    String.format(locale, "%.1f", bytes / BYTES_PER_GIGABYTE)

/**
 * [bytes] in decimal gigabytes with one decimal and the unit: 2.588.147.712 → "2,6 GB" in Spanish.
 * A no-break space joins the number and the unit, so a line never ends between them.
 */
fun formatGigabytes(bytes: Long, locale: Locale): String = formatGigabytesValue(bytes, locale) + "\u00A0GB"

/** Whole percentage of [done] over [total], from 0 to 100; 0 when [total] is not positive. */
fun progressPercent(done: Long, total: Long): Int =
    if (total <= 0) 0 else (done.coerceAtLeast(0) * 100 / total).coerceIn(0, 100).toInt()
