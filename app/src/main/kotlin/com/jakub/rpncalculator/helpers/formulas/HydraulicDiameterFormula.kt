package com.jakub.rpncalculator.helpers.formulas

import com.jakub.rpncalculator.R
import com.jakub.rpncalculator.helpers.MATH_CONTEXT
import com.jakub.rpncalculator.helpers.RpnEngine
import java.math.BigDecimal

/**
 * Hydraulic diameter of a rectangular duct: Dh = 2·W·H / (W + H)
 *
 * Main reference: https://en.wikipedia.org/wiki/Hydraulic_diameter
 */
object HydraulicDiameterFormula : Formula {
    override val nameResId: Int = R.string.formula_hydraulic_diameter
    override val imageResId: Int = R.drawable.ic_formula_vector
    override val key: String = "HydraulicDiameterFormula"
    override val section: FormulaSection = FormulaSection.SCIENTIFIC
    override val expressionResId: Int = R.string.formula_hydraulic_diameter_expression

    private val TWO = BigDecimal(2)

    override val variables: List<FormulaVariable> = listOf(
        FormulaVariable("Dh", R.string.formula_var_hydraulic_diameter, R.string.formula_unit_meter),
        FormulaVariable("W", R.string.formula_var_width, R.string.formula_unit_meter),
        FormulaVariable("H", R.string.formula_var_height, R.string.formula_unit_meter)
    )

    override fun solve(known: Map<String, BigDecimal>, solveFor: String, modeIndex: Int): BigDecimal {
        val dh = known["Dh"]
        val w = known["W"]
        val h = known["H"]
        return when (solveFor) {
            "Dh" -> RpnEngine.divide(TWO.multiply(w!!, MATH_CONTEXT).multiply(h!!, MATH_CONTEXT), w.add(h, MATH_CONTEXT))
            "W" -> RpnEngine.divide(dh!!.multiply(h!!, MATH_CONTEXT), TWO.multiply(h, MATH_CONTEXT).subtract(dh, MATH_CONTEXT))
            "H" -> RpnEngine.divide(dh!!.multiply(w!!, MATH_CONTEXT), TWO.multiply(w, MATH_CONTEXT).subtract(dh, MATH_CONTEXT))
            else -> throw IllegalArgumentException("Cannot solve for $solveFor")
        }
    }
}
