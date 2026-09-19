package com.jakub.rpncalculator.helpers.formulas

import com.jakub.rpncalculator.R
import com.jakub.rpncalculator.helpers.RpnEngine
import java.math.BigDecimal

/**
 * Reynolds number: Re = ρvL / μ
 *
 * Main reference: https://en.wikipedia.org/wiki/Reynolds_number
 */
object ReynoldsNumberFormula : Formula {
    override val nameResId: Int = R.string.formula_reynolds_number
    override val imageResId: Int = R.drawable.ic_formula_vector
    override val key: String = "ReynoldsNumberFormula"
    override val section: FormulaSection = FormulaSection.SCIENTIFIC
    override val expressionResId: Int = R.string.formula_reynolds_number_expression

    override val variables: List<FormulaVariable> = listOf(
        FormulaVariable("Re", R.string.formula_var_reynolds_number),
        FormulaVariable("ρ", R.string.formula_var_density, R.string.unit_density_kg_per_cubic_meter_symbol),
        FormulaVariable("v", R.string.formula_var_velocity, R.string.formula_unit_meter_per_second),
        FormulaVariable("L", R.string.formula_var_characteristic_length, R.string.formula_unit_meter),
        FormulaVariable("μ", R.string.formula_var_dynamic_viscosity, R.string.formula_unit_pascal_second)
    )

    override fun solve(known: Map<String, BigDecimal>, solveFor: String, modeIndex: Int): BigDecimal {
        val rho = known["ρ"]
        val v = known["v"]
        val length = known["L"]
        val mu = known["μ"]
        val re = known["Re"]
        return when (solveFor) {
            "Re" -> RpnEngine.divide(RpnEngine.multiply(RpnEngine.multiply(rho!!, v!!), length!!), mu!!)
            "ρ" -> RpnEngine.divide(RpnEngine.multiply(re!!, mu!!), RpnEngine.multiply(v!!, length!!))
            "v" -> RpnEngine.divide(RpnEngine.multiply(re!!, mu!!), RpnEngine.multiply(rho!!, length!!))
            "L" -> RpnEngine.divide(RpnEngine.multiply(re!!, mu!!), RpnEngine.multiply(rho!!, v!!))
            "μ" -> RpnEngine.divide(RpnEngine.multiply(RpnEngine.multiply(rho!!, v!!), length!!), re!!)
            else -> throw IllegalArgumentException("Cannot solve for $solveFor")
        }
    }
}
