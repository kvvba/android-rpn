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
    fun `formatting never mutates the underlying value`() {
        val value = BigDecimal("123456789012345678.987654321")
        val copy = BigDecimal(value.toString())
        formatter.bigDecimalToString(value)
        assertEquals(0, value.compareTo(copy))
    }
}
