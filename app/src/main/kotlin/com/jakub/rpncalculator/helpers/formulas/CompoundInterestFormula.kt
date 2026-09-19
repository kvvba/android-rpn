package com.jakub.rpncalculator.helpers.formulas

import com.jakub.rpncalculator.R
import com.jakub.rpncalculator.helpers.MATH_CONTEXT
import com.jakub.rpncalculator.helpers.RpnEngine
import java.math.BigDecimal

/**
 * Compound interest: A = P(1 + r/n)^(nt)
 *
 * [n] (compounds per year) isn't solvable in closed form (it appears both inside and outside the
 * exponent), so it must always be supplied.
 *
 * Main reference: https://en.wikipedia.org/wiki/Compound_interest
 */
object CompoundInterestFormula : Formula {
    override val nameResId: Int = R.string.formula_compound_interest
    override val imageResId: Int = R.drawable.ic_formula_vector
    override val key: String = "CompoundInterestFormula"
    override val section: FormulaSection = FormulaSection.FINANCE
    override val expressionResId: Int = R.string.formula_compound_interest_expression

    override val variables: List<FormulaVariable> = listOf(
        FormulaVariable("A", R.string.formula_var_final_amount),
        FormulaVariable("P", R.string.formula_var_principal),
        FormulaVariable("r", R.string.formula_var_annual_rate, R.string.formula_unit_decimal_rate),
        FormulaVariable("n", R.string.formula_var_compounds_per_year),
        FormulaVariable("t", R.string.formula_var_years)
    )

    override fun solve(known: Map<String, BigDecimal>, solveFor: String, modeIndex: Int): BigDecimal {
        val n = known["n"] ?: throw IllegalArgumentException("n must always be supplied")
        val growthFactor by lazy {
            BigDecimal.ONE.add(RpnEngine.divide(known["r"]!!, n), MATH_CONTEXT)
        }
        return when (solveFor) {
            "A" -> {
                val exponent = n.multiply(known["t"]!!, MATH_CONTEXT)
                RpnEngine.multiply(known["P"]!!, RpnEngine.power(growthFactor, exponent))
            }

            "P" -> {
                val exponent = n.multiply(known["t"]!!, MATH_CONTEXT)
                RpnEngine.divide(known["A"]!!, RpnEngine.power(growthFactor, exponent))
            }

            "t" -> {
                val ratio = RpnEngine.divide(known["A"]!!, known["P"]!!)
                RpnEngine.divide(RpnEngine.ln(ratio), n.multiply(RpnEngine.ln(growthFactor), MATH_CONTEXT))
            }

            "r" -> {
                val exponent = n.multiply(known["t"]!!, MATH_CONTEXT)
                val ratio = RpnEngine.divide(known["A"]!!, known["P"]!!)
                val root = RpnEngine.power(ratio, BigDecimal.ONE.divide(exponent, MATH_CONTEXT))
                n.multiply(root.subtract(BigDecimal.ONE, MATH_CONTEXT), MATH_CONTEXT)
            }

            "n" -> throw IllegalArgumentException("n cannot be solved for; it must be entered")
            else -> throw IllegalArgumentException("Cannot solve for $solveFor")
        }
    }
}
