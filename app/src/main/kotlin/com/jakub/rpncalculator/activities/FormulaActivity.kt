package com.jakub.rpncalculator.activities

import android.os.Bundle
import android.text.InputFilter
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
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols

class FormulaActivity : SimpleActivity() {
    companion object {
        const val EXTRA_FORMULA_ID = "formula_id"
        private const val PREFS_NAME = "formula_inputs"
    }

    private val binding by viewBinding(ActivityFormulaBinding::inflate)
    private lateinit var formula: Formula
    private lateinit var variablesBySymbol: Map<String, com.jakub.rpncalculator.helpers.formulas.FormulaVariable>
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
        variablesBySymbol = formula.variables.associateBy { it.symbol }

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
            val symbolSuffix = if (formula.showExpression) " (${variable.symbol})" else ""
            row.formulaVariableLabel.text = "${getString(variable.nameResId)}$symbolSuffix$unitSuffix"
            row.formulaVariableInput.setText(prefs.getString(prefsKey(variable.symbol), ""))
            if (variable.isCurrency) {
                row.formulaVariableInput.filters = arrayOf(twoDecimalInputFilter)
            }
            row.formulaVariableInput.setOnLongClickListener {
                copyToClipboard(formatter.removeThousandsSeparator(row.formulaVariableInput.text.toString()))
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
            val result = formula.solve(known, blankSymbol, modeIndex)
            inputsBySymbol.getValue(blankSymbol).setText(formatValue(blankSymbol, result))
            known[blankSymbol] = result

            binding.formulaDerivedOutputsContainer.removeAllViews()
            for ((labelResId, value) in formula.derivedOutputs(known, modeIndex)) {
                val row = TextView(this)
                row.text = "${getString(labelResId)}: ${formatCurrency(value)}"
                row.setTextColor(getProperTextColor())
                binding.formulaDerivedOutputsContainer.addView(row)
            }
        } catch (e: IllegalArgumentException) {
            toast(e.message.orEmpty())
        } catch (e: ArithmeticException) {
            toast(e.message.orEmpty())
        }
    }

    /** [isCurrency] variables always display 2 decimals (e.g. "5.00"); everything else uses the
     * calculator's normal trailing-zero-stripping format. */
    private fun formatValue(symbol: String, value: BigDecimal): String =
        if (variablesBySymbol[symbol]?.isCurrency == true) {
            formatCurrency(value)
        } else {
            formatter.bigDecimalToString(value)
        }

    private fun formatCurrency(value: BigDecimal): String {
        val rounded = value.setScale(2, RoundingMode.HALF_UP)
        val df = DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance())
        return df.format(rounded)
    }

    /** Blocks typing a 3rd digit after the decimal point in a currency field. */
    private val twoDecimalInputFilter = InputFilter { source, start, end, dest, dstart, dend ->
        val result = dest.toString().substring(0, dstart) + source.subSequence(start, end) + dest.toString().substring(dend)
        val decimalIndex = result.indexOf(formatter.decimalSeparator)
        if (decimalIndex != -1 && result.length - decimalIndex - formatter.decimalSeparator.length > 2) "" else null
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
                otherInput.setText(formatValue(otherSymbol, results.getValue(otherSymbol)))
            }
        } catch (e: IllegalArgumentException) {
            toast(e.message.orEmpty())
        } catch (e: ArithmeticException) {
            toast(e.message.orEmpty())
        }
    }
}
