package com.jakub.rpncalculator.helpers.formulas

import com.jakub.rpncalculator.R
import com.jakub.rpncalculator.helpers.MATH_CONTEXT
import java.math.BigDecimal

/**
 * National income tax by country, picked from a dropdown. Each model is a simplified single
 * filer, national-level-only approximation of that country's current brackets: no deductions,
 * allowances, social contributions, or regional/state tax are applied. Germany's progressive
 * zone is approximated as a linear ramp of the marginal rate rather than its exact quadratic
 * formula. Figures are illustrative, not tax advice.
 */
object CountryIncomeTaxFormula : Formula {
    override val nameResId: Int = R.string.formula_income_tax
    override val imageResId: Int = R.drawable.ic_formula_vector
    override val key: String = "CountryIncomeTaxFormula"
    override val section: FormulaSection = FormulaSection.FINANCE
    override val expressionResId: Int = R.string.formula_income_tax_expression

    override val showExpression: Boolean = false
    override val solveFromSingleField: Boolean = true

    override val variables: List<FormulaVariable> = listOf(
        FormulaVariable("Income", R.string.formula_var_income, isCurrency = true),
        FormulaVariable("TakeHomeYear", R.string.formula_take_home_year, isCurrency = true),
        FormulaVariable("TakeHomeMonth", R.string.formula_take_home_month, isCurrency = true),
        FormulaVariable("Tax", R.string.formula_var_tax, isCurrency = true)
    )

    override val modeOptions: List<Int> = listOf(
        R.string.formula_tax_country_uk,
        R.string.formula_tax_country_us,
        R.string.formula_tax_country_poland,
        R.string.formula_tax_country_hungary,
        R.string.formula_tax_country_germany,
        R.string.formula_tax_country_france
    )

    /** Ascending (bandStart, marginalRate) pairs; the last band's rate applies with no cap. */
    private fun bracketTax(income: BigDecimal, brackets: List<Pair<String, String>>): BigDecimal {
        var tax = BigDecimal.ZERO
        for (i in brackets.indices) {
            val start = BigDecimal(brackets[i].first)
            val rate = BigDecimal(brackets[i].second)
            if (income <= start) break
            val end = brackets.getOrNull(i + 1)?.first?.let { BigDecimal(it) }
            val bandTop = if (end != null) end.min(income) else income
            val bandAmount = bandTop.subtract(start, MATH_CONTEXT)
            if (bandAmount.signum() > 0) {
                tax = tax.add(bandAmount.multiply(rate, MATH_CONTEXT), MATH_CONTEXT)
            }
        }
        return tax
    }

    private val ukBrackets = listOf("0" to "0.0", "12570" to "0.20", "50270" to "0.40", "125140" to "0.45")
    private val usBrackets = listOf(
        "0" to "0.10", "11000" to "0.12", "44725" to "0.22", "95375" to "0.24",
        "182100" to "0.32", "231250" to "0.35", "578125" to "0.37"
    )
    private val polandBrackets = listOf("0" to "0.0", "30000" to "0.12", "120000" to "0.32")
    private val hungaryBrackets = listOf("0" to "0.15")
    private val franceBrackets = listOf(
        "0" to "0.0", "11294" to "0.11", "28797" to "0.30", "82341" to "0.41", "177106" to "0.45"
    )

    private fun usTax(income: BigDecimal) = bracketTax(income, usBrackets)
    private fun ukTax(income: BigDecimal) = bracketTax(income, ukBrackets)
    private fun polandTax(income: BigDecimal) = bracketTax(income, polandBrackets)
    private fun hungaryTax(income: BigDecimal) = bracketTax(income, hungaryBrackets)
    private fun franceTax(income: BigDecimal) = bracketTax(income, franceBrackets)

    /** Approximates Germany's quadratic progressive zone as a linear ramp of the marginal rate. */
    private fun germanyTax(income: BigDecimal): BigDecimal {
        val x0 = BigDecimal("11604")
        val x1 = BigDecimal("66760")
        val x2 = BigDecimal("277825")
        val r0 = BigDecimal("0.14")
        val r1 = BigDecimal("0.42")
        val r2 = BigDecimal("0.45")

        fun zoneTwoTax(upTo: BigDecimal): BigDecimal {
            val span = x1.subtract(x0, MATH_CONTEXT)
            val y = upTo.subtract(x0, MATH_CONTEXT)
            val linear = r0.multiply(y, MATH_CONTEXT)
            val quadratic = r1.subtract(r0, MATH_CONTEXT)
                .divide(BigDecimal(2).multiply(span, MATH_CONTEXT), MATH_CONTEXT)
                .multiply(y.multiply(y, MATH_CONTEXT), MATH_CONTEXT)
            return linear.add(quadratic, MATH_CONTEXT)
        }

        if (income <= x0) return BigDecimal.ZERO
        if (income <= x1) return zoneTwoTax(income)

        val taxAtX1 = zoneTwoTax(x1)
        if (income <= x2) {
            return taxAtX1.add(income.subtract(x1, MATH_CONTEXT).multiply(r1, MATH_CONTEXT), MATH_CONTEXT)
        }
        val taxAtX2 = taxAtX1.add(x2.subtract(x1, MATH_CONTEXT).multiply(r1, MATH_CONTEXT), MATH_CONTEXT)
        return taxAtX2.add(income.subtract(x2, MATH_CONTEXT).multiply(r2, MATH_CONTEXT), MATH_CONTEXT)
    }

    private fun taxForIncome(income: BigDecimal, modeIndex: Int): BigDecimal = when (modeIndex) {
        0 -> ukTax(income)
        1 -> usTax(income)
        2 -> polandTax(income)
        3 -> hungaryTax(income)
        4 -> germanyTax(income)
        5 -> franceTax(income)
        else -> throw IllegalArgumentException("Unknown tax country: $modeIndex")
    }

    private fun netForIncome(income: BigDecimal, modeIndex: Int): BigDecimal =
        income.subtract(taxForIncome(income, modeIndex), MATH_CONTEXT)

    /** Bisects a monotonically increasing, non-invertible-in-closed-form function of income. */
    private fun incomeWhere(target: BigDecimal, valueForIncome: (BigDecimal) -> BigDecimal): BigDecimal {
        var low = BigDecimal.ZERO
        var high = BigDecimal("1000000000")
        repeat(80) {
            val mid = low.add(high, MATH_CONTEXT).divide(BigDecimal(2), MATH_CONTEXT)
            if (valueForIncome(mid) < target) low = mid else high = mid
        }
        return low.add(high, MATH_CONTEXT).divide(BigDecimal(2), MATH_CONTEXT)
    }

    private val currencySymbolResIds = listOf(
        R.string.currency_gbp_symbol, R.string.currency_usd_symbol, R.string.currency_pln_symbol,
        R.string.currency_huf_symbol, R.string.currency_eur_symbol, R.string.currency_eur_symbol
    )

    override fun currencySymbolResId(modeIndex: Int): Int? = currencySymbolResIds.getOrNull(modeIndex)

    /** One "up to X: rate%" line per band, the last band shown as "over X". */
    private fun bandLines(brackets: List<Pair<String, String>>): String =
        brackets.mapIndexed { i, (start, rate) ->
            val ratePercent = BigDecimal(rate).multiply(BigDecimal(100)).stripTrailingZeros().toPlainString()
            val next = brackets.getOrNull(i + 1)?.first
            val startFormatted = BigDecimal(start).toBigInteger().toString()
            when {
                next == null -> "Over $startFormatted: $ratePercent%"
                start == "0" -> "Up to ${BigDecimal(next).toBigInteger()}: $ratePercent%"
                else -> "$startFormatted–${BigDecimal(next).toBigInteger()}: $ratePercent%"
            }
        }.joinToString("\n")

    override fun explanation(modeIndex: Int): String = when (modeIndex) {
        0 -> "UK tax bands (2024/25):\n${bandLines(ukBrackets)}"
        1 -> "US federal brackets, single filer (2024):\n${bandLines(usBrackets)}"
        2 -> "Poland PIT scale:\n${bandLines(polandBrackets)}"
        3 -> "Hungary: flat rate\n${bandLines(hungaryBrackets)}"
        4 -> "Germany: approximated as a linear ramp of the marginal rate\n" +
            "Up to 11604: 0%\n11604–66760: 14% ramping to 42%\n66760–277825: 42%\nOver 277825: 45%"
        5 -> "France income tax bands:\n${bandLines(franceBrackets)}"
        else -> "Unknown country"
    }

    override fun solve(known: Map<String, BigDecimal>, solveFor: String, modeIndex: Int): BigDecimal =
        throw UnsupportedOperationException("CountryIncomeTaxFormula solves via solveAll")

    override fun solveAll(knownSymbol: String, knownValue: BigDecimal, modeIndex: Int): Map<String, BigDecimal> {
        val income = when (knownSymbol) {
            "Income" -> knownValue
            "TakeHomeYear" -> incomeWhere(knownValue) { netForIncome(it, modeIndex) }
            "TakeHomeMonth" -> incomeWhere(knownValue.multiply(BigDecimal(12), MATH_CONTEXT)) { netForIncome(it, modeIndex) }
            "Tax" -> incomeWhere(knownValue) { taxForIncome(it, modeIndex) }
            else -> throw IllegalArgumentException("Unknown field: $knownSymbol")
        }
        val tax = taxForIncome(income, modeIndex)
        val takeHomeYear = income.subtract(tax, MATH_CONTEXT)
        val takeHomeMonth = takeHomeYear.divide(BigDecimal(12), MATH_CONTEXT)
        return mapOf(
            "Income" to income,
            "TakeHomeYear" to takeHomeYear,
            "TakeHomeMonth" to takeHomeMonth,
            "Tax" to tax
        )
    }
}
