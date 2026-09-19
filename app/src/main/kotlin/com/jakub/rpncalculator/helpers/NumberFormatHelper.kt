package com.jakub.rpncalculator.helpers

import java.math.BigDecimal
import java.math.MathContext
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import kotlin.math.abs

class NumberFormatHelper(
    val decimalSeparator: String = getDecimalSeparator(),
    val groupingSeparator: String = getGroupingSeparator()
) {
    companion object {
        private const val MAX_FRACTION_DIGITS = 15

        // Includes the mantissa's digits and the exponent's digits, but not the "e", its sign,
        // the decimal point or a leading minus sign.
        private const val MAX_DISPLAY_DIGITS = 16
    }

    /**
     * Formats [bd] for display, never touching the value itself: [bd] keeps its full stored
     * precision regardless of what gets shown. Falls back to scientific notation, capped at
     * [MAX_DISPLAY_DIGITS] total digits, whenever the plain decimal form would need more digits
     * than that to show every digit at a fixed (non-shrinking) text size.
     *
     * The decision is made from the value's exact magnitude (significant-digit count and
     * exponent), not from a pre-rendered plain string: rendering first and measuring its length
     * would let [MAX_FRACTION_DIGITS] silently round a tiny nonzero value down to "0" before its
     * length was ever checked, hiding the fact that it needed scientific notation at all.
     *
     * A negative [BigDecimal.scale] (e.g. `BigDecimal("123E3")`, from typing "123" then the E
     * key) means the value was expressed in scientific form to begin with; that's honored as a
     * scientific-display request regardless of digit count, rather than silently normalized back
     * to plain form just because it happens to be short.
     */
    fun bigDecimalToString(bd: BigDecimal): String {
        if (bd.signum() == 0) {
            return "0"
        }

        return if (bd.scale() >= 0 && plainDigitCount(bd) <= MAX_DISPLAY_DIGITS) {
            formatPlain(bd)
        } else {
            formatScientific(bd)
        }
    }

    /** How many digit characters [bd] would need to be shown in full, untruncated plain form. */
    private fun plainDigitCount(bd: BigDecimal): Int {
        val stripped = bd.stripTrailingZeros()
        val significantDigits = stripped.unscaledValue().abs().toString().length
        val exponent = significantDigits - 1 - stripped.scale()

        return if (exponent >= 0) {
            // Digits before the point, plus any of the significant digits left over after that.
            maxOf(exponent + 1, significantDigits)
        } else {
            // The leading "0", the zeros between the point and the first significant digit, and
            // the significant digits themselves.
            1 + (-exponent - 1) + significantDigits
        }
    }

    private fun formatPlain(bd: BigDecimal): String {
        val symbols = DecimalFormatSymbols.getInstance()

        val formatter = DecimalFormat()
        formatter.maximumFractionDigits = MAX_FRACTION_DIGITS
        formatter.decimalFormatSymbols = symbols
        formatter.isGroupingUsed = true

        val result = formatter.format(bd)
        return if (result.contains(decimalSeparator)) {
            result.trimEnd('0').trimEnd(decimalSeparator.single())
        } else {
            result
        }
    }

    private fun formatScientific(bd: BigDecimal): String {
        if (bd.signum() == 0) {
            return "0"
        }

        val absValue = bd.abs()
        val significantDigits = absValue.unscaledValue().toString().length
        var exponent = significantDigits - 1 - absValue.scale()

        val exponentDigits = abs(exponent).toString().length
        val mantissaPrecision = (MAX_DISPLAY_DIGITS - exponentDigits).coerceAtLeast(1)

        var mantissa = absValue.movePointLeft(exponent).round(MathContext(mantissaPrecision))
        if (mantissa >= BigDecimal.TEN) {
            // Rounding carried the mantissa up to 10 (e.g. 9.995 -> 10.0); renormalize.
            mantissa = mantissa.movePointLeft(1).round(MathContext(mantissaPrecision))
            exponent++
        }

        var mantissaStr = mantissa.toPlainString()
        if (mantissaStr.contains('.')) {
            mantissaStr = mantissaStr.trimEnd('0').trimEnd('.')
        }
        mantissaStr = mantissaStr.replace(".", decimalSeparator)

        val sign = if (bd.signum() < 0) "-" else ""
        val exponentSign = if (exponent >= 0) "+" else "-"
        return "$sign${mantissaStr}e$exponentSign${abs(exponent)}"
    }

    @Suppress("SwallowedException")
    fun addGroupingSeparators(str: String): String {
        // Deliberately bypasses the scientific-notation fallback in bigDecimalToString: this is
        // used for the live entry buffer while the user is still typing, which should always
        // show their digits as-is (just grouped), not get reformatted mid-entry.
        return try {
            formatPlain(removeGroupingSeparator(str).toBigDecimal())
        } catch (_: NumberFormatException) {
            // Return original string if it cannot be parsed as a valid number
            str
        }
    }

    fun removeGroupingSeparator(str: String): String {
        return str.replace(groupingSeparator, "").replace(decimalSeparator, ".")
    }

    fun formatForDisplay(input: String): String {
        var formatted = addGroupingSeparators(input)
        // allow writing numbers like 0.003
        if (input.contains(decimalSeparator)) {
            val firstPart = formatted.substringBefore(decimalSeparator)
            val lastPart = input.substringAfter(decimalSeparator)
            formatted = "$firstPart$decimalSeparator$lastPart"
        }

        return formatted
    }
}

fun getDecimalSeparator(): String {
    return DecimalFormatSymbols.getInstance().decimalSeparator.toString()
}

fun getGroupingSeparator(): String {
    return DecimalFormatSymbols.getInstance().groupingSeparator.toString()
}
