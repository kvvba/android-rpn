package com.jakub.rpncalculator.activities

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import org.fossify.commons.extensions.viewBinding
import org.fossify.commons.helpers.NavigationIcon
import com.jakub.rpncalculator.R
import com.jakub.rpncalculator.adapters.FormulasAdapter
import com.jakub.rpncalculator.databinding.ActivityFormulaPickerBinding
import com.jakub.rpncalculator.extensions.config
import com.jakub.rpncalculator.helpers.formulas.Formula

class FormulaPickerActivity : SimpleActivity() {
    private val binding by viewBinding(ActivityFormulaPickerBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setupEdgeToEdge(padBottomSystem = listOf(binding.formulasGrid))
        setupMaterialScrollListener(binding.formulasGrid, binding.formulaPickerAppbar)

        binding.formulasGrid.layoutManager = LinearLayoutManager(this)
        binding.formulasGrid.adapter = FormulasAdapter(this, Formula.ALL) {
            Intent(this, FormulaActivity::class.java).apply {
                putExtra(FormulaActivity.EXTRA_FORMULA_ID, it)
                startActivity(this)
            }
        }

        binding.formulaPickerToolbar.setTitle(R.string.formulae)

        binding.converterFormulaTabs.getTabAt(1)!!.select()
        binding.converterFormulaTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                if (tab.position == 0) {
                    startActivity(Intent(this@FormulaPickerActivity, UnitConverterPickerActivity::class.java))
                    finish()
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    override fun onResume() {
        super.onResume()

        setupTopAppBar(binding.formulaPickerAppbar, NavigationIcon.Arrow)

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
