package com.jakub.rpncalculator.helpers.formulas

import com.jakub.rpncalculator.R
import com.jakub.rpncalculator.helpers.RpnEngine
import java.math.BigDecimal

/**
 * Prandtl number: Pr = cp·μ / k
 *
 * Main reference: https://en.wikipedia.org/wiki/Prandtl_number
 */
object PrandtlNumberFormula : Formula {
    override val nameResId: Int = R.string.formula_prandtl_number
    override val imageResId: Int = R.drawable.ic_formula_vector
    override val key: String = "PrandtlNumberFormula"
    override val section: FormulaSection = FormulaSection.SCIENTIFIC
    override val expressionResId: Int = R.string.formula_prandtl_number_expression

    override val variables: List<FormulaVariable> = listOf(
        FormulaVariable("Pr", R.string.formula_var_prandtl_number),
        FormulaVariable("cp", R.string.formula_var_specific_heat, R.string.formula_unit_specific_heat),
        FormulaVariable("μ", R.string.formula_var_dynamic_viscosity, R.string.formula_unit_pascal_second),
        FormulaVariable("k", R.string.formula_var_thermal_conductivity, R.string.formula_unit_thermal_conductivity)
    )

    override fun solve(known: Map<String, BigDecimal>, solveFor: String): BigDecimal {
        val cp = known["cp"]
        val mu = known["μ"]
        val k = known["k"]
        val pr = known["Pr"]
        return when (solveFor) {
            "Pr" -> RpnEngine.divide(RpnEngine.multiply(cp!!, mu!!), k!!)
            "cp" -> RpnEngine.divide(RpnEngine.multiply(pr!!, k!!), mu!!)
            "μ" -> RpnEngine.divide(RpnEngine.multiply(pr!!, k!!), cp!!)
            "k" -> RpnEngine.divide(RpnEngine.multiply(cp!!, mu!!), pr!!)
            else -> throw IllegalArgumentException("Cannot solve for $solveFor")
        }
    }
}
