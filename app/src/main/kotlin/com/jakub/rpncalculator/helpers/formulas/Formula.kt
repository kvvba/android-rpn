package com.jakub.rpncalculator.helpers.formulas

import com.jakub.rpncalculator.R
import java.math.BigDecimal

/** One fillable variable in a [Formula]: its symbol, display name, and optional unit hint. */
data class FormulaVariable(val symbol: String, val nameResId: Int, val unitResId: Int? = null)

enum class FormulaSection(val nameResId: Int) {
    SCIENTIFIC(R.string.formula_section_scientific),
    FINANCE(R.string.formula_section_finance),
    UTILITY(R.string.formula_section_utility)
}

interface Formula {
    companion object {
        val ALL: List<Formula> = listOf(
            ReynoldsNumberFormula,
            PrandtlNumberFormula,
            CompoundInterestFormula,
            FlatIncomeTaxFormula,
            TipSplitFormula
        )
    }

    val nameResId: Int
    val imageResId: Int
    val key: String
    val section: FormulaSection

    /** The formula written symbolically, e.g. "Re = ρ·v·L / μ". */
    val expressionResId: Int

    val variables: List<FormulaVariable>

    /**
     * Computes [solveFor] from [known], which holds every other variable's symbol mapped to its
     * value. Throws [IllegalArgumentException] if the combination can't be solved (e.g. a
     * variable this formula can't isolate, or a division by zero).
     */
    fun solve(known: Map<String, BigDecimal>, solveFor: String): BigDecimal
}
