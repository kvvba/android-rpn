package com.jakub.rpncalculator.helpers.formulas

import com.jakub.rpncalculator.R
import com.jakub.rpncalculator.helpers.MATH_CONTEXT
import com.jakub.rpncalculator.helpers.RpnEngine
import java.math.BigDecimal

/**
 * Flat-rate income tax: Tax = Income * Rate / 100
 *
 * A simple single-rate model, not any specific jurisdiction's progressive tax brackets.
 */
object FlatIncomeTaxFormula : Formula {
    override val nameResId: Int = R.string.formula_income_tax
    override val imageResId: Int = R.drawable.ic_formula_vector
    override val key: String = "FlatIncomeTaxFormula"
    override val section: FormulaSection = FormulaSection.FINANCE
    override val expressionResId: Int = R.string.formula_income_tax_expression

    private val HUNDRED = BigDecimal(100)

    override val variables: List<FormulaVariable> = listOf(
        FormulaVariable("Tax", R.string.formula_var_tax),
        FormulaVariable("Income", R.string.formula_var_income),
        FormulaVariable("Rate", R.string.formula_var_tax_rate, R.string.formula_unit_percent)
    )

    override fun solve(known: Map<String, BigDecimal>, solveFor: String): BigDecimal {
        val tax = known["Tax"]
        val income = known["Income"]
        val rate = known["Rate"]
        return when (solveFor) {
            "Tax" -> RpnEngine.divide(income!!.multiply(rate!!, MATH_CONTEXT), HUNDRED)
            "Income" -> RpnEngine.divide(tax!!.multiply(HUNDRED, MATH_CONTEXT), rate!!)
            "Rate" -> RpnEngine.divide(tax!!.multiply(HUNDRED, MATH_CONTEXT), income!!)
            else -> throw IllegalArgumentException("Cannot solve for $solveFor")
        }
    }
}
