package com.jakub.rpncalculator.adapters

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.fossify.commons.extensions.copyToClipboard
import org.fossify.commons.extensions.getProperTextColor
import com.jakub.rpncalculator.activities.SimpleActivity
import com.jakub.rpncalculator.databinding.HistoryViewBinding
import com.jakub.rpncalculator.helpers.CalculatorImpl
import com.jakub.rpncalculator.helpers.NumberFormatHelper
import com.jakub.rpncalculator.models.History

class HistoryAdapter(
    val activity: SimpleActivity,
    val items: List<History>,
    val calc: CalculatorImpl,
    val itemClick: () -> Unit
) :
    RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    private var textColor = activity.getProperTextColor()
    private val formatter = NumberFormatHelper()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(HistoryViewBinding.inflate(activity.layoutInflater, parent, false))


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bindView(item)
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(private val binding: HistoryViewBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bindView(item: History): View {
            itemView.apply {
                binding.itemFormula.text = item.formula
                binding.itemResult.text = item.result
                binding.itemFormula.setTextColor(textColor)
                binding.itemResult.setTextColor(textColor)

                setOnClickListener {
                    calc.addNumberToFormula(item.result)
                    itemClick()
                }

                setOnLongClickListener {
                    activity.baseContext.copyToClipboard(formatter.removeThousandsSeparator(item.result))
                    true
                }
            }

            return itemView
        }
    }
}
