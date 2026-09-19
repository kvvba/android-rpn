package com.jakub.rpncalculator.activities

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import org.fossify.commons.extensions.getProperTextColor
import org.fossify.commons.extensions.viewBinding
import org.fossify.commons.helpers.NavigationIcon
import com.jakub.rpncalculator.R
import com.jakub.rpncalculator.adapters.FormulasAdapter
import com.jakub.rpncalculator.adapters.UnitTypesAdapter
import com.jakub.rpncalculator.databinding.ActivityUnitConverterPickerBinding
import com.jakub.rpncalculator.extensions.config
import com.jakub.rpncalculator.helpers.converters.Converter
import com.jakub.rpncalculator.helpers.formulas.Formula

class UnitConverterPickerActivity : SimpleActivity() {
    private val binding by viewBinding(ActivityUnitConverterPickerBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setupEdgeToEdge(padBottomSystem = listOf(binding.unitConverterPickerScrollview))
        setupMaterialScrollListener(
            binding.unitConverterPickerScrollview,
            binding.unitConverterPickerAppbar
        )

        binding.unitTypesGrid.layoutManager = GridLayoutManager(this, 2)
        binding.unitTypesGrid.adapter = UnitTypesAdapter(this, Converter.ALL) {
            Intent(this, UnitConverterActivity::class.java).apply {
                putExtra(UnitConverterActivity.EXTRA_CONVERTER_ID, it)
                startActivity(this)
            }
        }

        binding.formulasList.layoutManager = LinearLayoutManager(this)
        binding.formulasList.adapter = FormulasAdapter(this, Formula.ALL) {
            Intent(this, FormulaActivity::class.java).apply {
                putExtra(FormulaActivity.EXTRA_FORMULA_ID, it)
                startActivity(this)
            }
        }

        binding.unitConverterPickerToolbar.setTitle(R.string.tools_title)
        binding.formulasHeader.setTextColor(getProperTextColor())
    }

    override fun onResume() {
        super.onResume()

        setupTopAppBar(binding.unitConverterPickerAppbar, NavigationIcon.Arrow)

        if (config.preventPhoneFromSleeping) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    override fun onPause() {
        super.onPause()
        if (config.preventPhoneFromSleeping) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}
