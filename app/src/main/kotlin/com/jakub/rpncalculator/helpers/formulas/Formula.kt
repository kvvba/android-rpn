package com.jakub.rpncalculator.helpers.formulas

import com.jakub.rpncalculator.R
import java.math.BigDecimal

/**
 * One fillable variable in a [Formula]: its symbol, display name, and optional unit hint.
 * [isCurrency] forces a computed value into fixed 2-decimal display (e.g. "5.00", not "5").
 */
data class FormulaVariable(
    val symbol: String,
    val nameResId: Int,
    val unitResId: Int? = null,
    val isCurrency: Boolean = false
)

enum class FormulaSection(val nameResId: Int) {
    UTILITY(R.string.formula_section_utility),
    SCIENTIFIC(R.string.formula_section_scientific),
    FINANCE(R.string.formula_section_finance)
}

interface Formula {
    companion object {
        val ALL: List<Formula> = listOf(
            ReynoldsNumberFormula,
            PrandtlNumberFormula,
            HydraulicDiameterFormula,
            CompoundInterestFormula,
            CountryIncomeTaxFormula,
            TipSplitFormula
        )
    }

    val nameResId: Int
    val imageResId: Int
    val key: String
    val section: FormulaSection

    /** The formula written symbolically, e.g. "Re = ρ·v·L / μ". */
    val expressionResId: Int

    /** False hides the symbolic expression line entirely. */
    val showExpression: Boolean get() = true

    val variables: List<FormulaVariable>

    /** Display names for a mode dropdown shown above the fields; empty means no dropdown. */
    val modeOptions: List<Int> get() = emptyList()

    /** True if the user fills exactly one field and every other one is derived from it. */
    val solveFromSingleField: Boolean get() = false

    /** Currency symbol string res for [isCurrency] fields, if it depends on [modeIndex]. */
    fun currencySymbolResId(modeIndex: Int): Int? = null

    /** Human-readable breakdown of how [modeIndex] computes its result, shown below the buttons. */
    fun explanation(modeIndex: Int): String? = null

    /**
     * Computes [solveFor] from [known], which holds every other variable's symbol mapped to its
     * value. [modeIndex] selects among [modeOptions] and is 0 when there is no dropdown. Throws
     * [IllegalArgumentException] if the combination can't be solved (e.g. a variable this formula
     * can't isolate, or a division by zero). Not used when [solveFromSingleField] is true.
     */
    fun solve(known: Map<String, BigDecimal>, solveFor: String, modeIndex: Int = 0): BigDecimal

    /**
     * Only used when [solveFromSingleField] is true: computes every other variable's symbol from
     * the single field [knownSymbol]/[knownValue] the user filled in.
     */
    fun solveAll(knownSymbol: String, knownValue: BigDecimal, modeIndex: Int = 0): Map<String, BigDecimal> =
        throw UnsupportedOperationException()

    /**
     * Extra read-only figures to show after a successful [solve], derived from [known] plus the
     * just-solved value. Empty by default; [known] holds every variable's symbol mapped to its
     * value.
     */
    fun derivedOutputs(known: Map<String, BigDecimal>, modeIndex: Int = 0): List<Pair<Int, BigDecimal>> = emptyList()
}
