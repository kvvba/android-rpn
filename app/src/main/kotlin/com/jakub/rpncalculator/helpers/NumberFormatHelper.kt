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
     *
     * [displayMode] can force scientific or engineering notation regardless of digit count;
     * [engineeringShift] (a multiple of 3) further shifts an engineering-mode exponent, letting
     * the same value be viewed at different powers of a thousand.
     */
    fun bigDecimalToString(
        bd: BigDecimal,
        displayMode: DisplayMode = DisplayMode.NORMAL,
        engineeringShift: Int = 0
    ): String {
        if (bd.signum() == 0) {
            return "0"
        }

        return when (displayMode) {
            DisplayMode.NORMAL -> {
                // Only the integer part can't be truncated without changing the value's
                // magnitude, so that's what decides whether plain form is even an option here.
                // Fraction digits beyond MAX_FRACTION_DIGITS are fine to silently drop (formatPlain
                // already does that) *unless* dropping them would erase the value entirely.
                val integerDigitCount = maxOf(naturalExponent(bd) + 1, 1)
                val plain = formatPlain(bd)
                val vanished = plain == "0" || plain == "-0"
                if (integerDigitCount <= MAX_DISPLAY_DIGITS && !vanished) plain else formatScientific(bd)
            }

            DisplayMode.SCIENTIFIC -> formatScientific(bd)
            DisplayMode.ENGINEERING -> formatEngineering(bd, engineeringShift)
        }
    }

    /** floor(log10(|bd|)): the power of ten of [bd]'s leading significant digit. */
    private fun naturalExponent(bd: BigDecimal): Int {
        val stripped = bd.stripTrailingZeros()
        val significantDigits = stripped.unscaledValue().abs().toString().length
        return significantDigits - 1 - stripped.scale()
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

    /**
     * Like [formatScientific], but the exponent is rounded UP to the nearest multiple of 3 (so
     * the mantissa can land below 1, e.g. 100 -> "0.1e+3" rather than "100e+0"), plus
     * [extraShift] (itself a multiple of 3) to let the same value be viewed at an adjacent power
     * of a thousand, e.g. "0.1e+3" -> "0.0001e+6".
     */
    private fun formatEngineering(bd: BigDecimal, extraShift: Int): String {
        if (bd.signum() == 0) {
            return "0"
        }

        val absValue = bd.abs()
        val significantDigits = absValue.unscaledValue().toString().length
        val naturalExponent = significantDigits - 1 - absValue.scale()

        val quotient = Math.floorDiv(naturalExponent, 3)
        val baseExponent = if (naturalExponent - quotient * 3 == 0) quotient * 3 else (quotient + 1) * 3
        val exponent = baseExponent + extraShift

        val exponentDigits = abs(exponent).toString().length
        val mantissaPrecision = (MAX_DISPLAY_DIGITS - exponentDigits).coerceAtLeast(1)
        val mantissa = absValue.movePointLeft(exponent).round(MathContext(mantissaPrecision))

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

    /** Strips only the thousands separator, e.g. for copying "1,000" to the clipboard as "1000". */
    fun removeThousandsSeparator(str: String): String = str.replace(groupingSeparator, "")

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
