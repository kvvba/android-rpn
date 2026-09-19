package com.jakub.rpncalculator.helpers.formulas

import com.jakub.rpncalculator.R
import com.jakub.rpncalculator.helpers.MATH_CONTEXT
import com.jakub.rpncalculator.helpers.RpnEngine
import java.math.BigDecimal

/** Tip and bill splitting: PerPerson = Bill * (1 + Tip / 100) / People */
object TipSplitFormula : Formula {
    override val nameResId: Int = R.string.formula_tip_split
    override val imageResId: Int = R.drawable.ic_formula_vector
    override val key: String = "TipSplitFormula"
    override val section: FormulaSection = FormulaSection.UTILITY
    override val expressionResId: Int = R.string.formula_tip_split_expression

    private val HUNDRED = BigDecimal(100)

    override val variables: List<FormulaVariable> = listOf(
        FormulaVariable("Bill", R.string.formula_var_bill),
        FormulaVariable("Tip", R.string.formula_var_tip, R.string.formula_unit_percent),
        FormulaVariable("People", R.string.formula_var_people),
        FormulaVariable("PerPerson", R.string.formula_var_per_person)
    )

    override fun solve(known: Map<String, BigDecimal>, solveFor: String): BigDecimal {
        val bill = known["Bill"]
        val tip = known["Tip"]
        val people = known["People"]
        val perPerson = known["PerPerson"]
        val tipFactor by lazy { BigDecimal.ONE.add(RpnEngine.divide(tip!!, HUNDRED), MATH_CONTEXT) }
        return when (solveFor) {
            "PerPerson" -> RpnEngine.divide(bill!!.multiply(tipFactor, MATH_CONTEXT), people!!)
            "Bill" -> RpnEngine.divide(perPerson!!.multiply(people!!, MATH_CONTEXT), tipFactor)
            "Tip" -> RpnEngine.divide(perPerson!!.multiply(people!!, MATH_CONTEXT), bill!!)
                .subtract(BigDecimal.ONE, MATH_CONTEXT)
                .multiply(HUNDRED, MATH_CONTEXT)

            "People" -> RpnEngine.divide(bill!!.multiply(tipFactor, MATH_CONTEXT), perPerson!!)
            else -> throw IllegalArgumentException("Cannot solve for $solveFor")
        }
    }
}
