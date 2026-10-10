package com.tfg.kaifit_pal.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class ByteFormatTest {

    private val spanish = Locale.forLanguageTag("es-ES")

    // BY1
    @Test
    fun gigabytesAreDecimalWithOneDecimalAndTheLocaleSeparator() {
        assertEquals("2,6 GB", formatGigabytes(2_588_147_712, spanish))
        assertEquals("1,1 GB", formatGigabytes(1_100_000_000, spanish))
        assertEquals("2.6 GB", formatGigabytes(2_588_147_712, Locale.US))
        assertEquals("1,1", formatGigabytesValue(1_090_000_000, spanish))
    }

    // BY1
    @Test
    fun progressIsAWholePercentageLimitedTo0And100() {
        assertEquals(50, progressPercent(1_294_073_856, 2_588_147_712))
        assertEquals(42, progressPercent(1_090_000_000, 2_588_147_712))
        assertEquals(0, progressPercent(5, 0))
        assertEquals(100, progressPercent(10, 5))
        assertEquals(0, progressPercent(-1, 5))
    }
}
