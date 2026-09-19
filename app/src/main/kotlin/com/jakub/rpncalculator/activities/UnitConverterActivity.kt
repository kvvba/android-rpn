package com.jakub.rpncalculator.activities

import android.content.res.Configuration
import android.os.Bundle
import android.text.format.DateUtils
import android.view.View
import android.view.WindowManager
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import org.fossify.commons.extensions.beVisibleIf
import org.fossify.commons.extensions.getProperTextColor
import org.fossify.commons.extensions.performHapticFeedback
import org.fossify.commons.extensions.toast
import org.fossify.commons.extensions.viewBinding
import org.fossify.commons.helpers.LOWER_ALPHA_INT
import org.fossify.commons.helpers.MEDIUM_ALPHA_INT
import org.fossify.commons.helpers.NavigationIcon
import com.jakub.rpncalculator.R
import com.jakub.rpncalculator.databinding.ActivityUnitConverterBinding
import com.jakub.rpncalculator.extensions.config
import com.jakub.rpncalculator.extensions.updateViewColors
import com.jakub.rpncalculator.helpers.CONVERTER_STATE
import com.jakub.rpncalculator.helpers.CurrencyRatesStore
import com.jakub.rpncalculator.helpers.converters.Converter
import com.jakub.rpncalculator.helpers.converters.CurrencyConverter
import com.jakub.rpncalculator.helpers.converters.TemperatureConverter
import com.jakub.rpncalculator.helpers.getDecimalSeparator
import com.jakub.rpncalculator.views.ConverterView

class UnitConverterActivity : SimpleActivity(), ConverterView.OnUnitChangedListener {
    companion object {
        const val EXTRA_CONVERTER_ID = "converter_id"
    }

    private val binding by viewBinding(ActivityUnitConverterBinding::inflate)
    private var vibrateOnButtonPress = true
    private lateinit var converter: Converter

    private val pillDrawable by lazy {
        ResourcesCompat.getDrawable(
            resources, org.fossify.commons.R.drawable.pill_background, theme
        )?.mutate()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            binding.unitConverterToolbar.inflateMenu(R.menu.converter_menu)
            setupOptionsMenu()
        }

        setupEdgeToEdge(padBottomSystem = listOf(binding.nestedScrollview))
        setupMaterialScrollListener(binding.nestedScrollview, binding.unitConverterAppbar)

        val converter = Converter.ALL.getOrNull(intent.getIntExtra(EXTRA_CONVERTER_ID, 0))

        if (converter == null) {
            finish()
            return
        }
        this.converter = converter

        binding.viewUnitConverter.btnClear.setVibratingOnClickListener {
            binding.viewUnitConverter.viewConverter.root.deleteCharacter()
        }
        binding.viewUnitConverter.btnClear.setOnLongClickListener {
            binding.viewUnitConverter.viewConverter.root.clear()
            true
        }

        binding.viewUnitConverter.run {
            arrayOf(
                btnDecimal, btnPlusMinus, btn0, btn1, btn2, btn3, btn4, btn5, btn6, btn7, btn8, btn9
            ).forEach {
                it.setVibratingOnClickListener { view ->
                    binding.viewUnitConverter.viewConverter.root.numpadClicked(view.id)
                }
            }
        }

        binding.viewUnitConverter.viewConverter.root.setOnUnitChangedListener(this)
        binding.viewUnitConverter.viewConverter.root.setConverter(converter)
        binding.unitConverterToolbar.setTitle(converter.nameResId)

        if (converter.key == CurrencyConverter.key) {
            CurrencyRatesStore.ensureLoaded(this)
            binding.viewUnitConverter.currencyRefreshLayout.visibility = View.VISIBLE
            updateRatesUpdatedLabel()
            binding.viewUnitConverter.currencyRefreshButton.setOnClickListener {
                refreshCurrencyRates()
            }
        }

        if (savedInstanceState != null) {
            savedInstanceState.getBundle(CONVERTER_STATE)?.also {
                binding.viewUnitConverter.viewConverter.root.restoreFromSavedState(it)
            }
        } else {
            val storedState = config.getLastConverterUnits(converter)
            if (storedState != null) {
                binding.viewUnitConverter.viewConverter.root.updateUnits(
                    newTopUnit = storedState.topUnit,
                    newBottomUnit = storedState.bottomUnit
                )
            }
        }
    }

    private fun setupOptionsMenu() {
        binding.unitConverterToolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.swap_units -> binding.viewUnitConverter.viewConverter.root.switch()
                else -> return@setOnMenuItemClickListener false
            }
            return@setOnMenuItemClickListener true
        }
    }

    override fun onResume() {
        super.onResume()

        setupTopAppBar(binding.unitConverterAppbar, NavigationIcon.Arrow)
        binding.viewUnitConverter.viewConverter.root.updateColors()
        binding.viewUnitConverter.converterHolder.let {
            updateViewColors(it, getProperTextColor())
        }

        if (config.preventPhoneFromSleeping) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        setupDecimalButton()

        vibrateOnButtonPress = config.vibrateOnButtonPress

        binding.viewUnitConverter.apply {
            arrayOf(btnClear, btnDecimal).forEach {
                it.background = ResourcesCompat.getDrawable(
                    resources,
                    org.fossify.commons.R.drawable.pill_background,
                    theme
                )
                it.background?.alpha = MEDIUM_ALPHA_INT
            }

            arrayOf(btn0, btn1, btn2, btn3, btn4, btn5, btn6, btn7, btn8, btn9).forEach {
                it.background = ResourcesCompat.getDrawable(
                    resources,
                    org.fossify.commons.R.drawable.pill_background,
                    theme
                )
                it.background?.alpha = LOWER_ALPHA_INT
            }

            if (plusMinusLayout.isVisible) {
                updatePlusMinusButton()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (config.preventPhoneFromSleeping) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBundle(
            CONVERTER_STATE,
            binding.viewUnitConverter.viewConverter.root.saveState()
        )
    }

    override fun onUnitsChanged(topUnit: Converter.Unit?, bottomUnit: Converter.Unit?) {
        val isTemperatureConverter = converter.key == TemperatureConverter.key
        val shouldShowNegativeButton =
            isTemperatureConverter && when (topUnit?.key) {
                TemperatureConverter.Unit.Kelvin.key,
                TemperatureConverter.Unit.Rankine.key -> false

                else -> true
            }

        binding.viewUnitConverter.plusMinusLayout.beVisibleIf(shouldShowNegativeButton)
        if (shouldShowNegativeButton) updatePlusMinusButton()
    }

    private fun checkHaptic(view: View) {
        if (vibrateOnButtonPress) {
            view.performHapticFeedback()
        }
    }

    private fun View.setVibratingOnClickListener(callback: (view: View) -> Unit) {
        setOnClickListener {
            callback(it)
            checkHaptic(it)
        }
    }

    private fun setupDecimalButton() {
        binding.viewUnitConverter.btnDecimal.text = getDecimalSeparator()
    }

    private fun updatePlusMinusButton() {
        with(binding.viewUnitConverter) {
            btnPlusMinus.background = pillDrawable
            btnPlusMinus.background?.alpha = MEDIUM_ALPHA_INT
        }
    }

    private fun updateRatesUpdatedLabel() {
        val updatedAt = CurrencyRatesStore.lastUpdatedMillis()
        binding.viewUnitConverter.currencyLastUpdated.text = if (updatedAt == 0L) {
            getString(R.string.rates_never_updated)
        } else {
            val relativeTime = DateUtils.getRelativeTimeSpanString(
                updatedAt, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS
            )
            getString(R.string.rates_updated_format, relativeTime)
        }
    }

    private fun refreshCurrencyRates() {
        binding.viewUnitConverter.currencyRefreshButton.isEnabled = false
        CurrencyRatesStore.refresh { success ->
            binding.viewUnitConverter.currencyRefreshButton.isEnabled = true
            updateRatesUpdatedLabel()
            if (success) {
                binding.viewUnitConverter.viewConverter.root.recalculate()
                toast(R.string.rates_refresh_succeeded)
            } else {
                toast(R.string.rates_refresh_failed)
            }
        }
    }
}
