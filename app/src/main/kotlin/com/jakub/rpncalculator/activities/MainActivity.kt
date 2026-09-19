package com.jakub.rpncalculator.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import me.grantland.widget.AutofitHelper
import org.fossify.commons.extensions.appLaunched
import org.fossify.commons.extensions.copyToClipboard
import org.fossify.commons.extensions.getProperTextColor
import org.fossify.commons.extensions.hideKeyboard
import org.fossify.commons.extensions.launchMoreAppsFromUsIntent
import org.fossify.commons.extensions.performHapticFeedback
import org.fossify.commons.extensions.toast
import org.fossify.commons.extensions.value
import org.fossify.commons.extensions.viewBinding
import org.fossify.commons.helpers.LOWER_ALPHA_INT
import org.fossify.commons.helpers.MAX_ALPHA_INT
import org.fossify.commons.helpers.MEDIUM_ALPHA_INT
import com.jakub.rpncalculator.BuildConfig
import com.jakub.rpncalculator.R
import com.jakub.rpncalculator.databases.CalculatorDatabase
import com.jakub.rpncalculator.databinding.ActivityMainBinding
import com.jakub.rpncalculator.dialogs.HistoryDialog
import com.jakub.rpncalculator.extensions.config
import com.jakub.rpncalculator.extensions.updateViewColors
import com.jakub.rpncalculator.helpers.ACOS
import com.jakub.rpncalculator.helpers.ACOSH
import com.jakub.rpncalculator.helpers.ASIN
import com.jakub.rpncalculator.helpers.ASINH
import com.jakub.rpncalculator.helpers.ATAN
import com.jakub.rpncalculator.helpers.ATANH
import com.jakub.rpncalculator.helpers.CALCULATOR_STATE
import com.jakub.rpncalculator.helpers.Calculator
import com.jakub.rpncalculator.helpers.CalculatorImpl
import com.jakub.rpncalculator.helpers.COS
import com.jakub.rpncalculator.helpers.COSH
import com.jakub.rpncalculator.helpers.DIVIDE
import com.jakub.rpncalculator.helpers.HistoryHelper
import com.jakub.rpncalculator.helpers.LN
import com.jakub.rpncalculator.helpers.LOG
import com.jakub.rpncalculator.helpers.MINUS
import com.jakub.rpncalculator.helpers.MULTIPLY
import com.jakub.rpncalculator.helpers.NCR
import com.jakub.rpncalculator.helpers.NPR
import com.jakub.rpncalculator.helpers.PERCENT
import com.jakub.rpncalculator.helpers.PLUS
import com.jakub.rpncalculator.helpers.POWER
import com.jakub.rpncalculator.helpers.ROOT
import com.jakub.rpncalculator.helpers.RpnEngine
import com.jakub.rpncalculator.helpers.SIN
import com.jakub.rpncalculator.helpers.SINH
import com.jakub.rpncalculator.helpers.SQUARE
import com.jakub.rpncalculator.helpers.TAN
import com.jakub.rpncalculator.helpers.TANH
import com.jakub.rpncalculator.helpers.XTH_ROOT
import com.jakub.rpncalculator.helpers.getDecimalSeparator

class MainActivity : SimpleActivity(), Calculator {
    private var storedTextColor = 0
    private var vibrateOnButtonPress = true
    private var saveCalculatorState: String = ""
    private var secondLayerActive = false
    private var hypActive = false
    private lateinit var calc: CalculatorImpl

    private val binding by viewBinding(ActivityMainBinding::inflate)
    private val calcBinding get() = binding.viewCalculator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        appLaunched(BuildConfig.APPLICATION_ID)
        setupOptionsMenu()
        refreshMenuItems()
        setupEdgeToEdge(padBottomSystem = listOf(binding.mainNestedScrollview))
        setupMaterialScrollListener(binding.mainNestedScrollview, binding.mainAppbar)

        if (savedInstanceState != null) {
            saveCalculatorState = savedInstanceState.getCharSequence(CALCULATOR_STATE) as String
        }

        calc = CalculatorImpl(
            calculator = this,
            context = applicationContext,
            calculatorState = saveCalculatorState
        )
        calcBinding.btnPlus.setOnClickOperation(PLUS)
        calcBinding.btnMinus.setOnClickOperation(MINUS)
        calcBinding.btnMultiply.setOnClickOperation(MULTIPLY)
        calcBinding.btnDivide.setOnClickOperation(DIVIDE)
        calcBinding.btnPercent.setOnClickOperation(PERCENT)
        calcBinding.btnPower.setOnClickOperation(POWER)
        calcBinding.btnRoot.setOnClickOperation(ROOT)
        calcBinding.btnEnter.setVibratingOnClickListener { calc.handleEnter() }
        calcBinding.btnUndo.setVibratingOnClickListener { calc.handleUndo() }
        calcBinding.btnRollUp.setVibratingOnClickListener { calc.handleRollUp() }
        calcBinding.btnRollDown.setVibratingOnClickListener { calc.handleRollDown() }
        calcBinding.btnSwap.setVibratingOnClickListener { calc.handleSwap() }
        calcBinding.btnDrop.text = twoLineLabel("AC", "drop")
        calcBinding.btnDrop.setVibratingOnClickListener { calc.handleDrop() }
        calcBinding.btnDrop.setVibratingOnLongClickListener { calc.handleReset() }
        calcBinding.btnChs.setVibratingOnClickListener { calc.handleChs() }
        calcBinding.btnBackspace.setVibratingOnClickListener { calc.handleBackspace() }

        calcBinding.btnSecond.setVibratingOnClickListener { toggleSecondLayer() }
        calcBinding.btnPi.setVibratingOnClickListener { calc.handleConstant(RpnEngine.PI) }
        calcBinding.btnE.setVibratingOnClickListener { calc.handleConstant(RpnEngine.E) }
        calcBinding.btnLog.setOnClickOperation(LOG)
        calcBinding.btnLn.setOnClickOperation(LN)
        calcBinding.btnHyp.setVibratingOnClickListener { toggleHyp() }
        calcBinding.btnSin.setVibratingOnClickListener {
            calc.handleOperation(if (hypActive) SINH else SIN)
        }
        calcBinding.btnSin.setVibratingOnLongClickListener {
            calc.handleOperation(if (hypActive) ASINH else ASIN)
        }
        calcBinding.btnCos.setVibratingOnClickListener {
            calc.handleOperation(if (hypActive) COSH else COS)
        }
        calcBinding.btnCos.setVibratingOnLongClickListener {
            calc.handleOperation(if (hypActive) ACOSH else ACOS)
        }
        calcBinding.btnTan.setVibratingOnClickListener {
            calc.handleOperation(if (hypActive) TANH else TAN)
        }
        calcBinding.btnTan.setVibratingOnLongClickListener {
            calc.handleOperation(if (hypActive) ATANH else ATAN)
        }
        calcBinding.btnSquare.setOnClickOperation(SQUARE)
        calcBinding.btnNpr.setOnClickOperation(NPR)
        calcBinding.btnNcr.setOnClickOperation(NCR)
        calcBinding.btnXthroot.setOnClickOperation(XTH_ROOT)
        calcBinding.btnExponent.setVibratingOnClickListener { calc.handleExponent() }
        calcBinding.btnMemoryClear.setVibratingOnClickListener { calc.handleMemoryClear() }
        calcBinding.btnMemoryRecall.setVibratingOnClickListener { calc.handleMemoryRecall() }
        calcBinding.btnMemoryAdd.setVibratingOnClickListener { calc.handleMemoryAdd() }
        calcBinding.btnMemorySubtract.setVibratingOnClickListener { calc.handleMemorySubtract() }
        updateSecondLayerVisibility()
        updateModifierVisuals()

        binding.angleUnitIndicator.text = calc.currentAngleUnit().name
        binding.angleUnitIndicator.setTextColor(getProperTextColor())
        binding.angleUnitIndicator.setVibratingOnClickListener {
            calc.handleToggleAngleUnit()
            binding.angleUnitIndicator.text = calc.currentAngleUnit().name
        }

        getButtonIds().forEach {
            it.setVibratingOnClickListener { view ->
                calc.numpadClicked(view.id)
            }
        }

        calcBinding.formula.setOnLongClickListener { copyToClipboard(false) }
        calcBinding.result.setOnLongClickListener { copyToClipboard(true) }
        AutofitHelper.create(calcBinding.result)
        storeStateVariables()
        calcBinding.calculatorHolder.let { updateViewColors(it, getProperTextColor()) }
        setupDecimalButton()
        checkAppOnSDCard()
    }

    override fun onResume() {
        super.onResume()
        setupTopAppBar(binding.mainAppbar)
        setupMaterialScrollListener(binding.mainNestedScrollview, binding.mainAppbar)
        if (storedTextColor != config.textColor) {
            calcBinding.calculatorHolder.let { updateViewColors(it, getProperTextColor()) }
            binding.angleUnitIndicator.setTextColor(getProperTextColor())
        }

        if (config.preventPhoneFromSleeping) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        setupDecimalButton()
        vibrateOnButtonPress = config.vibrateOnButtonPress

        calcBinding.apply {
            arrayOf(
                btnPercent, btnPower, btnRoot, btnSwap, btnDrop, btnChs, btnBackspace,
                btnDivide, btnMultiply, btnPlus, btnMinus, btnEnter, btnDecimal, btnExponent,
                btnRollUp, btnRollDown, btnUndo,
                btnPi, btnE, btnLog, btnLn, btnHyp, btnSin, btnCos, btnTan, btnSquare,
                btnNpr, btnNcr, btnXthroot,
                btnMemoryClear, btnMemoryRecall, btnMemoryAdd, btnMemorySubtract,
                btnBlankZero, btnBlankDecimal
            ).forEach {
                it.background = ResourcesCompat.getDrawable(
                    resources, org.fossify.commons.R.drawable.pill_background, theme
                )
                it.background?.alpha = MEDIUM_ALPHA_INT
            }

            arrayOf(btn0, btn1, btn2, btn3, btn4, btn5, btn6, btn7, btn8, btn9).forEach {
                it.background = ResourcesCompat.getDrawable(
                    resources, org.fossify.commons.R.drawable.pill_background, theme
                )
                it.background?.alpha = LOWER_ALPHA_INT
            }

            btnSecond.background = ResourcesCompat.getDrawable(
                resources, org.fossify.commons.R.drawable.pill_background, theme
            )
        }
        updateSecondLayerToggleVisuals()
    }

    private fun toggleSecondLayer() {
        secondLayerActive = !secondLayerActive
        updateSecondLayerVisibility()
    }

    private fun updateSecondLayerVisibility() {
        val firstLayerVisibility = if (secondLayerActive) View.GONE else View.VISIBLE
        val secondLayerVisibility = if (secondLayerActive) View.VISIBLE else View.GONE
        calcBinding.apply {
            rowOperators.visibility = firstLayerVisibility
            row789.visibility = firstLayerVisibility
            row456.visibility = firstLayerVisibility
            row123.visibility = firstLayerVisibility
            btn0.visibility = firstLayerVisibility
            btnDecimal.visibility = firstLayerVisibility

            rowMemory.visibility = secondLayerVisibility
            rowConstants.visibility = secondLayerVisibility
            rowTrig.visibility = secondLayerVisibility
            rowBlank.visibility = secondLayerVisibility
            btnBlankZero.visibility = secondLayerVisibility
            btnBlankDecimal.visibility = secondLayerVisibility
        }
        updateSecondLayerToggleVisuals()
    }

    private fun updateSecondLayerToggleVisuals() {
        calcBinding.btnSecond.background?.alpha = if (secondLayerActive) MAX_ALPHA_INT else MEDIUM_ALPHA_INT
    }

    private fun toggleHyp() {
        hypActive = !hypActive
        updateModifierVisuals()
    }

    private fun updateModifierVisuals() {
        calcBinding.btnHyp.background?.alpha = if (hypActive) MAX_ALPHA_INT else MEDIUM_ALPHA_INT
        calcBinding.btnSin.text = trigLabel("sin")
        calcBinding.btnCos.text = trigLabel("cos")
        calcBinding.btnTan.text = trigLabel("tan")
    }

    /** Primary (tap) function big below, secondary (hold) function small above. */
    private fun trigLabel(base: String) = if (hypActive) {
        twoLineLabel("${base}h⁻¹", "${base}h")
    } else {
        twoLineLabel("$base⁻¹", base)
    }

    private fun twoLineLabel(secondary: String, primary: String): CharSequence {
        val text = "$secondary\n$primary"
        return SpannableString(text).apply {
            setSpan(RelativeSizeSpan(0.6f), 0, secondary.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    override fun onPause() {
        super.onPause()
        storeStateVariables()
        if (config.preventPhoneFromSleeping) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!isChangingConfigurations) {
            CalculatorDatabase.destroyInstance()
        }
    }

    override fun onSaveInstanceState(bundle: Bundle) {
        super.onSaveInstanceState(bundle)
        bundle.putString(CALCULATOR_STATE, calc.getCalculatorStateJson().toString())
    }

    private fun setupOptionsMenu() {
        binding.mainToolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.history -> showHistory()
                R.id.more_apps_from_us -> launchMoreAppsFromUsIntent()
                R.id.unit_converter -> launchUnitConverter()
                R.id.settings -> launchSettings()
                R.id.about -> launchAbout()
                else -> return@setOnMenuItemClickListener false
            }
            return@setOnMenuItemClickListener true
        }
    }

    private fun refreshMenuItems() {
        binding.mainToolbar.menu.apply {
            findItem(R.id.more_apps_from_us).isVisible =
                !resources.getBoolean(org.fossify.commons.R.bool.hide_google_relations)
        }
    }

    private fun storeStateVariables() {
        config.apply {
            storedTextColor = textColor
        }
    }

    private fun checkHaptic(view: View) {
        if (vibrateOnButtonPress) {
            view.performHapticFeedback()
        }
    }

    private fun showHistory() {
        HistoryHelper(this).getHistory {
            if (it.isEmpty()) {
                toast(R.string.history_empty)
            } else {
                HistoryDialog(this, it, calc)
            }
        }
    }

    private fun launchUnitConverter() {
        hideKeyboard()
        startActivity(Intent(applicationContext, UnitConverterPickerActivity::class.java))
    }

    private fun launchSettings() {
        hideKeyboard()
        startActivity(Intent(applicationContext, SettingsActivity::class.java))
    }

    private fun launchAbout() {
        val dialog = AlertDialog.Builder(this)
            .setTitle(org.fossify.commons.R.string.about)
            .setMessage(R.string.about_fork_notice)
            .setPositiveButton(android.R.string.ok, null)
            .create()
        dialog.show()
        dialog.findViewById<TextView>(androidx.appcompat.R.id.alertTitle)?.setTextColor(getProperTextColor())
    }

    private fun getButtonIds() = calcBinding.run {
        arrayOf(btnDecimal, btn0, btn1, btn2, btn3, btn4, btn5, btn6, btn7, btn8, btn9)
    }

    private fun copyToClipboard(copyResult: Boolean): Boolean {
        var value = calcBinding.formula.value
        if (copyResult) {
            value = calcBinding.result.value
        }

        return if (value.isNullOrEmpty()) {
            false
        } else {
            copyToClipboard(value)
            true
        }
    }

    override fun showNewResult(value: String, context: Context) {
        calcBinding.result.text = value
    }

    override fun showNewFormula(value: String, context: Context) {
        calcBinding.formula.text = truncateStackToFit(calcBinding.formula, value)
    }

    /**
     * Keeps only the last lines of [text] that fit within [view]'s measured height at its
     * current (fixed) text size, dropping the oldest/topmost stack entries first — the ones
     * closest to X are the most relevant and should never be pushed out by older ones.
     */
    private fun truncateStackToFit(view: TextView, text: String): String {
        val lineHeight = view.lineHeight
        val availableHeight = view.height - view.paddingTop - view.paddingBottom
        if (text.isEmpty() || lineHeight <= 0 || availableHeight <= 0) {
            return text
        }

        val lines = text.split("\n")
        val maxLines = (availableHeight / lineHeight).coerceAtLeast(1)
        return if (lines.size > maxLines) {
            lines.takeLast(maxLines).joinToString("\n")
        } else {
            text
        }
    }

    private fun setupDecimalButton() {
        calcBinding.btnDecimal.text = getDecimalSeparator()
    }

    private fun View.setVibratingOnClickListener(callback: (view: View) -> Unit) {
        setOnClickListener {
            callback(it)
            checkHaptic(it)
        }
    }

    private fun View.setVibratingOnLongClickListener(callback: (view: View) -> Unit) {
        setOnLongClickListener {
            callback(it)
            checkHaptic(it)
            true
        }
    }

    private fun View.setOnClickOperation(operation: String) {
        setVibratingOnClickListener {
            calc.handleOperation(operation)
        }
    }
}
