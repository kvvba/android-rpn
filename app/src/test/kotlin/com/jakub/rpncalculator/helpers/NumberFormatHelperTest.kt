package com.jakub.rpncalculator.helpers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class NumberFormatHelperTest {
    private lateinit var formatter: NumberFormatHelper
    private lateinit var sep: String
    private lateinit var group: String

    @Before
    fun setUp() {
        formatter = NumberFormatHelper()
        sep = formatter.decimalSeparator
        group = formatter.groupingSeparator
    }

    private fun digitsOf(str: String) = str.count { it.isDigit() }

    @Test
    fun `zero formats as 0`() {
        assertEquals("0", formatter.bigDecimalToString(BigDecimal.ZERO))
    }

    @Test
    fun `small decimal stays plain and trims trailing zeros`() {
        assertEquals("3${sep}1", formatter.bigDecimalToString(BigDecimal("3.100")))
    }

    @Test
    fun `plain integers get grouping separators`() {
        val expected = listOf("1", "234", "567").joinToString(group)
        assertEquals(expected, formatter.bigDecimalToString(BigDecimal("1234567")))
    }

    @Test
    fun `16-digit integer stays plain`() {
        val value = BigDecimal("9999999999999999") // 16 nines
        val result = formatter.bigDecimalToString(value)
        assertEquals(16, digitsOf(result))
        assertTrue(!result.contains("e"))
    }

    @Test
    fun `17-digit integer switches to scientific notation`() {
        val value = BigDecimal("99999999999999999") // 17 nines
        val result = formatter.bigDecimalToString(value)
        assertTrue(result.contains("e"))
        assertTrue(digitsOf(result) <= 16)
    }

    @Test
    fun `round power of ten stays a clean single-digit mantissa`() {
        // 1 followed by 17 zeros: 18 digits, well past the plain-form budget.
        val value = BigDecimal("100000000000000000")
        assertEquals("1e+17", formatter.bigDecimalToString(value))
    }

    @Test
    fun `tiny number that would round to zero uses scientific notation instead`() {
        // Plain form at 15 fraction digits would round this to "0" and silently lose it.
        val value = BigDecimal("1.5E-18")
        assertEquals("1${sep}5e-18", formatter.bigDecimalToString(value))
    }

    @Test
    fun `negative huge number keeps its sign and stays within the digit budget`() {
        val value = BigDecimal("-123456789012345678")
        val result = formatter.bigDecimalToString(value)
        assertTrue(result.startsWith("-"))
        assertTrue(result.contains("e+17"))
        assertTrue(digitsOf(result) <= 16)
    }

    @Test
    fun `scientific mantissa never carries an extra digit from rounding`() {
        // A mantissa of all 9s rounds up to 10 at reduced precision; it must renormalize into
        // the next exponent rather than leak a two-digit integer part into the mantissa.
        val value = BigDecimal("9.99999999999999E+50")
        val result = formatter.bigDecimalToString(value)
        assertTrue(digitsOf(result) <= 16)

        val mantissaValue = result.substringBefore("e").replace(sep, ".").toBigDecimal()
        assertTrue(mantissaValue.abs() < BigDecimal.TEN)
    }

    @Test
    fun `a value entered via scientific notation displays in scientific notation`() {
        // BigDecimal("123E3") has a negative scale (-3), unlike the equal-valued but
        // plain-entered BigDecimal("123000") (scale 0) - that distinction is what signals this
        // was typed via the "E" exponent key, and should stay scientific despite being short.
        val value = BigDecimal("123E3")
        assertEquals(0, value.compareTo(BigDecimal(123000)))
        assertEquals("1${sep}23e+5", formatter.bigDecimalToString(value))
    }

    @Test
    fun `scientific mode always uses scientific notation regardless of digit count`() {
        val result = formatter.bigDecimalToString(BigDecimal(100), DisplayMode.SCIENTIFIC)
        assertEquals("1e+2", result)
    }

    @Test
    fun `engineering mode rounds the exponent up to a multiple of 3`() {
        // 100 = 1e+2 in scientific form; engineering rounds the exponent up to 3.
        val result = formatter.bigDecimalToString(BigDecimal(100), DisplayMode.ENGINEERING)
        assertEquals("0${sep}1e+3", result)
    }

    @Test
    fun `engineering shift moves the value to the next power of a thousand`() {
        val base = formatter.bigDecimalToString(BigDecimal(100), DisplayMode.ENGINEERING, 0)
        val shifted = formatter.bigDecimalToString(BigDecimal(100), DisplayMode.ENGINEERING, 3)
        assertEquals("0${sep}1e+3", base)
        assertEquals("0${sep}0001e+6", shifted)
    }

    @Test
    fun `engineering shift can also move down to the natural grouping`() {
        // Base engineering form is 0.1e+3; shifting down by 3 reaches the natural 100e+0.
        val shiftedDown = formatter.bigDecimalToString(BigDecimal(100), DisplayMode.ENGINEERING, -3)
        assertEquals("100e+0", shiftedDown)
    }

    @Test
    fun `formatting never mutates the underlying value`() {
        val value = BigDecimal("123456789012345678.987654321")
        val copy = BigDecimal(value.toString())
        formatter.bigDecimalToString(value)
        assertEquals(0, value.compareTo(copy))
    }
}
