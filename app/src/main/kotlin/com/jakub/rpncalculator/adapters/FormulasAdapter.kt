package com.jakub.rpncalculator.adapters

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.fossify.commons.extensions.getProperTextColor
import com.jakub.rpncalculator.activities.SimpleActivity
import com.jakub.rpncalculator.databinding.ItemFormulaRowBinding
import com.jakub.rpncalculator.databinding.ItemFormulaSectionHeaderBinding
import com.jakub.rpncalculator.helpers.formulas.Formula
import com.jakub.rpncalculator.helpers.formulas.FormulaSection

private sealed class FormulaListItem {
    data class Header(val section: FormulaSection) : FormulaListItem()
    data class Row(val formula: Formula, val formulaIndex: Int) : FormulaListItem()
}

class FormulasAdapter(
    val activity: SimpleActivity,
    formulas: List<Formula>,
    val itemClick: (id: Int) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ROW = 1
    }

    private val items: List<FormulaListItem> = FormulaSection.entries
        .mapNotNull { section ->
            val formulasInSection = formulas.withIndex().filter { it.value.section == section }
            if (formulasInSection.isEmpty()) {
                null
            } else {
                listOf(FormulaListItem.Header(section)) +
                    formulasInSection.map { FormulaListItem.Row(it.value, it.index) }
            }
        }
        .flatten()

    override fun getItemViewType(position: Int) = when (items[position]) {
        is FormulaListItem.Header -> TYPE_HEADER
        is FormulaListItem.Row -> TYPE_ROW
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        if (viewType == TYPE_HEADER) {
            HeaderViewHolder(ItemFormulaSectionHeaderBinding.inflate(activity.layoutInflater, parent, false))
        } else {
            RowViewHolder(ItemFormulaRowBinding.inflate(activity.layoutInflater, parent, false))
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is FormulaListItem.Header -> (holder as HeaderViewHolder).bindView(item.section)
            is FormulaListItem.Row -> (holder as RowViewHolder).bindView(item.formula, item.formulaIndex)
        }
    }

    override fun getItemCount() = items.size

    inner class HeaderViewHolder(private val binding: ItemFormulaSectionHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bindView(section: FormulaSection) {
            binding.formulaSectionHeaderLabel.setText(section.nameResId)
            binding.formulaSectionHeaderLabel.setTextColor(activity.getProperTextColor())
        }
    }

    inner class RowViewHolder(private val binding: ItemFormulaRowBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bindView(formula: Formula, id: Int) {
            binding.formulaRowLabel.setText(formula.nameResId)
            binding.formulaRowLabel.setTextColor(activity.getProperTextColor())
            binding.root.setOnClickListener { itemClick(id) }
        }
    }
}
