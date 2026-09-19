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

    override val variables: List<FormulaVariable> = listOf(
        FormulaVariable("Income", R.string.formula_var_income),
        FormulaVariable("Tax", R.string.formula_var_tax)
    )

    override val modeOptions: List<Int> = listOf(
        R.string.formula_tax_country_us,
        R.string.formula_tax_country_uk,
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

    private fun usTax(income: BigDecimal) = bracketTax(
        income, listOf(
            "0" to "0.10", "11000" to "0.12", "44725" to "0.22", "95375" to "0.24",
            "182100" to "0.32", "231250" to "0.35", "578125" to "0.37"
        )
    )

    private fun ukTax(income: BigDecimal) = bracketTax(
        income, listOf(
            "0" to "0.0", "12570" to "0.20", "50270" to "0.40", "125140" to "0.45"
        )
    )

    private fun polandTax(income: BigDecimal) = bracketTax(
        income, listOf("0" to "0.0", "30000" to "0.12", "120000" to "0.32")
    )

    private fun hungaryTax(income: BigDecimal) = bracketTax(income, listOf("0" to "0.15"))

    private fun franceTax(income: BigDecimal) = bracketTax(
        income, listOf(
            "0" to "0.0", "11294" to "0.11", "28797" to "0.30",
            "82341" to "0.41", "177106" to "0.45"
        )
    )

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
        0 -> usTax(income)
        1 -> ukTax(income)
        2 -> polandTax(income)
        3 -> hungaryTax(income)
        4 -> germanyTax(income)
        5 -> franceTax(income)
        else -> throw IllegalArgumentException("Unknown tax country: $modeIndex")
    }

    /** Inverts the monotonic, non-invertible-in-closed-form tax function by bisection. */
    private fun incomeForTax(tax: BigDecimal, modeIndex: Int): BigDecimal {
        var low = BigDecimal.ZERO
        var high = BigDecimal("1000000000")
        repeat(80) {
            val mid = low.add(high, MATH_CONTEXT).divide(BigDecimal(2), MATH_CONTEXT)
            if (taxForIncome(mid, modeIndex) < tax) low = mid else high = mid
        }
        return low.add(high, MATH_CONTEXT).divide(BigDecimal(2), MATH_CONTEXT)
    }

    override fun solve(known: Map<String, BigDecimal>, solveFor: String, modeIndex: Int): BigDecimal {
        return when (solveFor) {
            "Tax" -> taxForIncome(known.getValue("Income"), modeIndex)
            "Income" -> incomeForTax(known.getValue("Tax"), modeIndex)
            else -> throw IllegalArgumentException("Cannot solve for $solveFor")
        }
    }
}
