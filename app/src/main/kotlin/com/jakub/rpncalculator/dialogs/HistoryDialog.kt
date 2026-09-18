package com.jakub.rpncalculator.dialogs

import androidx.appcompat.app.AlertDialog
import org.fossify.commons.extensions.getAlertDialogBuilder
import org.fossify.commons.extensions.setupDialogStuff
import org.fossify.commons.extensions.toast
import org.fossify.commons.helpers.ensureBackgroundThread
import com.jakub.rpncalculator.R
import com.jakub.rpncalculator.activities.SimpleActivity
import com.jakub.rpncalculator.adapters.HistoryAdapter
import com.jakub.rpncalculator.databinding.DialogHistoryBinding
import com.jakub.rpncalculator.extensions.calculatorDB
import com.jakub.rpncalculator.helpers.CalculatorImpl
import com.jakub.rpncalculator.models.History

class HistoryDialog(activity: SimpleActivity, items: List<History>, calculator: CalculatorImpl) {
    private var dialog: AlertDialog? = null

    init {

        val view = DialogHistoryBinding.inflate(activity.layoutInflater, null, false)

        activity.getAlertDialogBuilder()
            .setPositiveButton(org.fossify.commons.R.string.ok, null)
            .setNeutralButton(R.string.clear_history) { _, _ ->
                ensureBackgroundThread {
                    activity.applicationContext.calculatorDB.deleteHistory()
                    activity.toast(R.string.history_cleared)
                }
            }.apply {
                activity.setupDialogStuff(view.root, this, R.string.history) { alertDialog ->
                    dialog = alertDialog
                }
            }

        view.historyList.adapter = HistoryAdapter(activity, items, calculator) {
            dialog?.dismiss()
        }
    }
}
