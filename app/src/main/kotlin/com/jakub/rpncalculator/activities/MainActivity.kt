package com.jakub.rpncalculator.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.TextView
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
import org.fossify.commons.helpers.APP_ICON_IDS
import org.fossify.commons.helpers.LICENSE_AUTOFITTEXTVIEW
import org.fossify.commons.helpers.LICENSE_EVALEX
import org.fossify.commons.helpers.LOWER_ALPHA_INT
import org.fossify.commons.helpers.MAX_ALPHA_INT
import org.fossify.commons.helpers.MEDIUM_ALPHA_INT
import org.fossify.commons.models.FAQItem
import com.jakub.rpncalculator.BuildConfig
import com.jakub.rpncalculator.R
import com.jakub.rpncalculator.databases.CalculatorDatabase
import com.jakub.rpncalculator.databinding.ActivityMainBinding
import com.jakub.rpncalculator.dialogs.HistoryDialog
import com.jakub.rpncalculator.extensions.config
import com.jakub.rpncalculator.extensions.updateViewColors
import com.jakub.rpncalculator.helpers.CALCULATOR_STATE
import com.jakub.rpncalculator.helpers.Calculator
import com.jakub.rpncalculator.helpers.CalculatorImpl
import com.jakub.rpncalculator.helpers.COS
import com.jakub.rpncalculator.helpers.DIVIDE
import com.jakub.rpncalculator.helpers.HistoryHelper
import com.jakub.rpncalculator.helpers.LN
import com.jakub.rpncalculator.helpers.LOG
import com.jakub.rpncalculator.helpers.MINUS
import com.jakub.rpncalculator.helpers.MULTIPLY
import com.jakub.rpncalculator.helpers.PERCENT
import com.jakub.rpncalculator.helpers.PLUS
import com.jakub.rpncalculator.helpers.POWER
import com.jakub.rpncalculator.helpers.ROOT
import com.jakub.rpncalculator.helpers.RpnEngine
import com.jakub.rpncalculator.helpers.SIN
import com.jakub.rpncalculator.helpers.SQUARE
import com.jakub.rpncalculator.helpers.TAN
import com.jakub.rpncalculator.helpers.getDecimalSeparator

class MainActivity : SimpleActivity(), Calculator {
    private var storedTextColor = 0
    private var vibrateOnButtonPress = true
    private var saveCalculatorState: String = ""
    private var secondLayerActive = false
    private lateinit var calc: CalculatorImpl

    private val binding by viewBinding(ActivityMainBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        appLaunched(BuildConfig.APPLICATION_ID)
        setupOptionsMenu()
        refreshMenuItems()
        setupEdgeToEdge(padBottomSystem = listOf(binding.mainNestedScrollview))
        setupMaterialScrollListener(binding.mainNestedScrollview, binding.mainAppbar!!)

        if (savedInstanceState != null) {
            saveCalculatorState = savedInstanceState.getCharSequence(CALCULATOR_STATE) as String
        }

        calc = CalculatorImpl(
            calculator = this,
            context = applicationContext,
            calculatorState = saveCalculatorState
        )
        binding.btnPlus?.setOnClickOperation(PLUS)
        binding.btnMinus?.setOnClickOperation(MINUS)
        binding.btnMultiply?.setOnClickOperation(MULTIPLY)
        binding.btnDivide?.setOnClickOperation(DIVIDE)
        binding.btnPercent?.setOnClickOperation(PERCENT)
        binding.btnPower?.setOnClickOperation(POWER)
        binding.btnRoot?.setOnClickOperation(ROOT)
        binding.btnEnter?.setVibratingOnClickListener { calc.handleEnter() }
        binding.btnUndo?.setVibratingOnClickListener { calc.handleUndo() }
        binding.btnRollUp?.setVibratingOnClickListener { calc.handleRollUp() }
        binding.btnRollDown?.setVibratingOnClickListener { calc.handleRollDown() }
        binding.btnSwap?.setVibratingOnClickListener { calc.handleSwap() }
        binding.btnDrop?.setVibratingOnClickListener { calc.handleDrop() }
        binding.btnChs?.setVibratingOnClickListener { calc.handleChs() }
        binding.btnBackspace?.setVibratingOnClickListener { calc.handleBackspace() }
        binding.btnAc?.setVibratingOnClickListener { calc.handleReset() }

        binding.btnSecond?.setVibratingOnClickListener { toggleSecondLayer() }
        binding.btnPi?.setVibratingOnClickListener { calc.handleConstant(RpnEngine.PI) }
        binding.btnE?.setVibratingOnClickListener { calc.handleConstant(RpnEngine.E) }
        binding.btnLog?.setOnClickOperation(LOG)
        binding.btnLn?.setOnClickOperation(LN)
        binding.btnSin?.setOnClickOperation(SIN)
        binding.btnCos?.setOnClickOperation(COS)
        binding.btnTan?.setOnClickOperation(TAN)
        binding.btnSquare?.setOnClickOperation(SQUARE)
        binding.btnMemoryClear?.setVibratingOnClickListener { calc.handleMemoryClear() }
        binding.btnMemoryRecall?.setVibratingOnClickListener { calc.handleMemoryRecall() }
        binding.btnMemoryAdd?.setVibratingOnClickListener { calc.handleMemoryAdd() }
        binding.btnMemorySubtract?.setVibratingOnClickListener { calc.handleMemorySubtract() }
        updateSecondLayerVisibility()

        getButtonIds().forEach {
            it?.setVibratingOnClickListener { view ->
                calc.numpadClicked(view.id)
            }
        }

        binding.formula?.setOnLongClickListener { copyToClipboard(false) }
        binding.result?.setOnLongClickListener { copyToClipboard(true) }
        AutofitHelper.create(binding.result)
        storeStateVariables()
        binding.calculatorHolder?.let { updateViewColors(it, getProperTextColor()) }
        setupDecimalButton()
        checkAppOnSDCard()
    }

    override fun onResume() {
        super.onResume()
        setupTopAppBar(binding.mainAppbar!!)
        setupMaterialScrollListener(binding.mainNestedScrollview, binding.mainAppbar)
        if (storedTextColor != config.textColor) {
            binding.calculatorHolder?.let { updateViewColors(it, getProperTextColor()) }
        }

        if (config.preventPhoneFromSleeping) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        setupDecimalButton()
        vibrateOnButtonPress = config.vibrateOnButtonPress

        binding.apply {
            arrayOf(
                btnPercent, btnPower, btnRoot, btnSwap, btnDrop, btnChs, btnBackspace, btnAc,
                btnDivide, btnMultiply, btnPlus, btnMinus, btnEnter, btnDecimal,
                btnRollUp, btnRollDown, btnUndo,
                btnPi, btnE, btnLog, btnLn, btnSin, btnCos, btnTan, btnSquare,
                btnMemoryClear, btnMemoryRecall, btnMemoryAdd, btnMemorySubtract
            ).forEach {
                it?.background = ResourcesCompat.getDrawable(
                    resources, org.fossify.commons.R.drawable.pill_background, theme
                )
                it?.background?.alpha = MEDIUM_ALPHA_INT
            }

            arrayOf(btn0, btn1, btn2, btn3, btn4, btn5, btn6, btn7, btn8, btn9).forEach {
                it?.background = ResourcesCompat.getDrawable(
                    resources, org.fossify.commons.R.drawable.pill_background, theme
                )
                it?.background?.alpha = LOWER_ALPHA_INT
            }

            btnSecond?.background = ResourcesCompat.getDrawable(
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
        binding.apply {
            row789?.visibility = firstLayerVisibility
            row456?.visibility = firstLayerVisibility
            row123?.visibility = firstLayerVisibility
            rowConstants?.visibility = secondLayerVisibility
            rowTrig?.visibility = secondLayerVisibility
            rowMemory?.visibility = secondLayerVisibility
        }
        updateSecondLayerToggleVisuals()
    }

    private fun updateSecondLayerToggleVisuals() {
        binding.btnSecond?.background?.alpha = if (secondLayerActive) MAX_ALPHA_INT else MEDIUM_ALPHA_INT
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
        startActivity(
            Intent(applicationContext, SettingsActivity::class.java).apply {
                putIntegerArrayListExtra(APP_ICON_IDS, getAppIconIDs())
            }
        )
    }

    private fun launchAbout() {
        val licenses = LICENSE_AUTOFITTEXTVIEW or LICENSE_EVALEX

        val faqItems = arrayListOf(
            FAQItem(R.string.faq_1_title, R.string.faq_1_text),
            FAQItem(R.string.faq_2_title, R.string.faq_2_text),
            FAQItem(
                title = org.fossify.commons.R.string.faq_1_title_commons,
                text = org.fossify.commons.R.string.faq_1_text_commons
            ),
            FAQItem(
                title = org.fossify.commons.R.string.faq_4_title_commons,
                text = org.fossify.commons.R.string.faq_4_text_commons
            )
        )

        if (!resources.getBoolean(org.fossify.commons.R.bool.hide_google_relations)) {
            faqItems.add(
                FAQItem(
                    title = org.fossify.commons.R.string.faq_2_title_commons,
                    text = org.fossify.commons.R.string.faq_2_text_commons
                )
            )
            faqItems.add(
                FAQItem(
                    title = org.fossify.commons.R.string.faq_6_title_commons,
                    text = org.fossify.commons.R.string.faq_6_text_commons
                )
            )
        }

        startAboutActivity(
            appNameId = R.string.app_name,
            licenseMask = licenses,
            versionName = BuildConfig.VERSION_NAME,
            faqItems = faqItems,
            showFAQBeforeMail = true
        )
    }

    private fun getButtonIds() = binding.run {
        arrayOf(btnDecimal, btn0, btn1, btn2, btn3, btn4, btn5, btn6, btn7, btn8, btn9)
    }

    private fun copyToClipboard(copyResult: Boolean): Boolean {
        var value = binding.formula?.value
        if (copyResult) {
            value = binding.result?.value
        }

        return if (value.isNullOrEmpty()) {
            false
        } else {
            copyToClipboard(value)
            true
        }
    }

    override fun showNewResult(value: String, context: Context) {
        binding.result?.text = value
    }

    override fun showNewFormula(value: String, context: Context) {
        binding.formula?.let { it.text = truncateStackToFit(it, value) }
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
        binding.btnDecimal?.text = getDecimalSeparator()
    }

    private fun View.setVibratingOnClickListener(callback: (view: View) -> Unit) {
        setOnClickListener {
            callback(it)
            checkHaptic(it)
        }
    }

    private fun View.setOnClickOperation(operation: String) {
        setVibratingOnClickListener {
            calc.handleOperation(operation)
        }
    }
}
