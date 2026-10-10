package com.tfg.kaifit_pal.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class DecimalInputTest {

    @Test
    fun commaIsReadAsDecimalSeparator() {
        assertEquals(80.5, parseDecimal("80,5")!!, 0.0)
    }

    @Test
    fun dotIsReadAsDecimalSeparator() {
        assertEquals(80.5, parseDecimal("80.5")!!, 0.0)
    }

    @Test
    fun blankTextIsNoNumber() {
        assertNull(parseDecimal(""))
        assertNull(parseDecimal(" "))
    }

    @Test
    fun malformedTextIsNoNumber() {
        assertNull(parseDecimal("1,2,3"))
        assertNull(parseDecimal("1.2.3"))
        assertNull(parseDecimal("abc"))
        assertNull(parseDecimal(","))
    }

    @Test
    fun keystrokeFilterAllowsOnlyShortDecimalNumbers() {
        assertFalse(acceptDecimalInput("8a"))
        assertFalse(acceptDecimalInput("80,5,"))
        assertFalse(acceptDecimalInput("80.5,"))
        assertFalse(acceptDecimalInput("1800"))
        assertFalse(acceptDecimalInput("80,555"))

        assertTrue(acceptDecimalInput(""))
        assertTrue(acceptDecimalInput("8"))
        assertTrue(acceptDecimalInput("80,"))
        assertTrue(acceptDecimalInput("80,55"))
    }

    @Test
    fun nanAndInfinityAreNoNumbers() {
        assertNull(parseDecimal("NaN"))
        assertNull(parseDecimal("Infinity"))
    }

    @Test
    fun formattingUsesLocaleSeparatorWithoutTrailingZeros() {
        val spanish = Locale.forLanguageTag("es-ES")

        assertEquals("80", formatDecimalInput(80.0, spanish))
        assertEquals("80,5", formatDecimalInput(80.5, spanish))
        assertEquals("80.25", formatDecimalInput(80.25, Locale.US))
    }
}
