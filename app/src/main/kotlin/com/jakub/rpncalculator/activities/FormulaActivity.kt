package com.jakub.rpncalculator.activities

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.TextView
import org.fossify.commons.extensions.copyToClipboard
import org.fossify.commons.extensions.getProperTextColor
import org.fossify.commons.extensions.showKeyboard
import org.fossify.commons.extensions.toast
import org.fossify.commons.extensions.viewBinding
import org.fossify.commons.helpers.NavigationIcon
import com.jakub.rpncalculator.R
import com.jakub.rpncalculator.databinding.ActivityFormulaBinding
import com.jakub.rpncalculator.databinding.ItemFormulaVariableBinding
import com.jakub.rpncalculator.extensions.config
import com.jakub.rpncalculator.helpers.NumberFormatHelper
import com.jakub.rpncalculator.helpers.formulas.Formula
import com.jakub.rpncalculator.helpers.formulas.FormulaSection
import java.math.BigDecimal
import java.math.RoundingMode

class FormulaActivity : SimpleActivity() {
    companion object {
        const val EXTRA_FORMULA_ID = "formula_id"
        private const val PREFS_NAME = "formula_inputs"
    }

    private val binding by viewBinding(ActivityFormulaBinding::inflate)
    private lateinit var formula: Formula
    private val inputsBySymbol = LinkedHashMap<String, EditText>()
    private val formatter = NumberFormatHelper()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setupEdgeToEdge()

        val formulaId = intent.getIntExtra(EXTRA_FORMULA_ID, 0)
        val selected = Formula.ALL.getOrNull(formulaId)
        if (selected == null) {
            finish()
            return
        }
        formula = selected

        binding.formulaToolbar.title = getString(formula.nameResId)
        if (formula.showExpression) {
            binding.formulaExpression.text = getString(formula.expressionResId)
            binding.formulaExpression.setOnLongClickListener {
                copyToClipboard(binding.formulaExpression.text.toString())
                true
            }
        } else {
            binding.formulaExpression.visibility = View.GONE
        }

        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        if (formula.modeOptions.isNotEmpty()) {
            binding.formulaModeSpinner.visibility = View.VISIBLE
            val textColor = getProperTextColor()
            binding.formulaModeSpinner.adapter = object : ArrayAdapter<String>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                formula.modeOptions.map { getString(it) }
            ) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
                    super.getView(position, convertView, parent).apply {
                        (this as TextView).setTextColor(textColor)
                    }

                override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View =
                    super.getDropDownView(position, convertView, parent).apply {
                        (this as TextView).setTextColor(textColor)
                    }
            }
            binding.formulaModeSpinner.setSelection(prefs.getInt(modeKey(), 0))
        }
        formula.variables.forEach { variable ->
            val row = ItemFormulaVariableBinding.inflate(layoutInflater, binding.formulaVariablesContainer, true)
            val unitSuffix = variable.unitResId?.let { " [${getString(it)}]" }.orEmpty()
            row.formulaVariableLabel.text = "${getString(variable.nameResId)} (${variable.symbol})$unitSuffix"
            row.formulaVariableInput.setText(prefs.getString(prefsKey(variable.symbol), ""))
            row.formulaVariableInput.setOnLongClickListener {
                copyToClipboard(row.formulaVariableInput.text.toString())
                true
            }
            row.formulaVariableInput.setOnClickListener {
                showKeyboard(row.formulaVariableInput)
            }
            inputsBySymbol[variable.symbol] = row.formulaVariableInput
        }

        binding.formulaCalculateButton.setOnClickListener { calculate() }
        binding.formulaClearButton.setOnClickListener { clearFields() }
    }

    override fun onResume() {
        super.onResume()
        setupTopAppBar(binding.formulaAppbar, NavigationIcon.Arrow)
        updateViewColorsRecursively()

        if (config.preventPhoneFromSleeping) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    override fun onPause() {
        super.onPause()
        if (config.preventPhoneFromSleeping) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        persistFields()
    }

    private fun prefsKey(symbol: String) = "${formula.key}_$symbol"

    private fun modeKey() = "${formula.key}_mode"

    private fun persistFields() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        prefs.edit().apply {
            for ((symbol, input) in inputsBySymbol) {
                putString(prefsKey(symbol), input.text.toString())
            }
            if (formula.modeOptions.isNotEmpty()) {
                putInt(modeKey(), binding.formulaModeSpinner.selectedItemPosition)
            }
        }.apply()
    }

    private fun clearFields() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        prefs.edit().apply {
            for ((symbol, input) in inputsBySymbol) {
                input.setText("")
                remove(prefsKey(symbol))
            }
        }.apply()
        binding.formulaDerivedOutputsContainer.removeAllViews()
    }

    private fun updateViewColorsRecursively() {
        val textColor = getProperTextColor()
        binding.formulaExpression.setTextColor(textColor)
        val container = binding.formulaVariablesContainer
        for (i in 0 until container.childCount) {
            val row = container.getChildAt(i) as android.view.ViewGroup
            for (j in 0 until row.childCount) {
                (row.getChildAt(j) as? android.widget.TextView)?.setTextColor(textColor)
            }
        }
    }

    private fun calculate() {
        if (formula.solveFromSingleField) {
            calculateFromSingleField()
            return
        }

        val known = mutableMapOf<String, BigDecimal>()
        var blankSymbol: String? = null
        var blankCount = 0

        for ((symbol, input) in inputsBySymbol) {
            val text = input.text.toString().trim()
            if (text.isEmpty()) {
                blankCount++
                blankSymbol = symbol
            } else {
                val value = formatter.removeGroupingSeparator(text).toBigDecimalOrNull()
                if (value == null) {
                    toast(getString(R.string.formula_invalid_number, symbol))
                    return
                }
                known[symbol] = value
            }
        }

        if (blankCount != 1 || blankSymbol == null) {
            toast(R.string.formula_fill_all_but_one)
            return
        }

        try {
            val modeIndex = if (formula.modeOptions.isNotEmpty()) binding.formulaModeSpinner.selectedItemPosition else 0
            var result = formula.solve(known, blankSymbol, modeIndex)
            if (formula.section == FormulaSection.FINANCE) {
                result = result.setScale(2, RoundingMode.HALF_UP)
            }
            inputsBySymbol.getValue(blankSymbol).setText(formatter.bigDecimalToString(result))
            known[blankSymbol] = result

            binding.formulaDerivedOutputsContainer.removeAllViews()
            for ((labelResId, value) in formula.derivedOutputs(known, modeIndex)) {
                val displayValue = if (formula.section == FormulaSection.FINANCE) {
                    value.setScale(2, RoundingMode.HALF_UP)
                } else {
                    value
                }
                val row = TextView(this)
                row.text = "${getString(labelResId)}: ${formatter.bigDecimalToString(displayValue)}"
                row.setTextColor(getProperTextColor())
                binding.formulaDerivedOutputsContainer.addView(row)
            }
        } catch (e: IllegalArgumentException) {
            toast(e.message.orEmpty())
        } catch (e: ArithmeticException) {
            toast(e.message.orEmpty())
        }
    }

    private fun calculateFromSingleField() {
        val filled = inputsBySymbol.filterValues { it.text.toString().trim().isNotEmpty() }
        if (filled.size != 1) {
            toast(R.string.formula_fill_exactly_one)
            return
        }
        val (symbol, input) = filled.entries.first()
        val value = formatter.removeGroupingSeparator(input.text.toString().trim()).toBigDecimalOrNull()
        if (value == null) {
            toast(getString(R.string.formula_invalid_number, symbol))
            return
        }

        try {
            val modeIndex = if (formula.modeOptions.isNotEmpty()) binding.formulaModeSpinner.selectedItemPosition else 0
            val results = formula.solveAll(symbol, value, modeIndex)
            for ((otherSymbol, otherInput) in inputsBySymbol) {
                if (otherSymbol == symbol) {
                    continue
                }
                var result = results.getValue(otherSymbol)
                if (formula.section == FormulaSection.FINANCE) {
                    result = result.setScale(2, RoundingMode.HALF_UP)
                }
                otherInput.setText(formatter.bigDecimalToString(result))
            }
        } catch (e: IllegalArgumentException) {
            toast(e.message.orEmpty())
        } catch (e: ArithmeticException) {
            toast(e.message.orEmpty())
        }
    }
}
