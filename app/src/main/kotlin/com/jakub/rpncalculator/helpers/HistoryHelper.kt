package com.jakub.rpncalculator.helpers

import android.content.Context
import android.os.Handler
import android.os.Looper
import org.fossify.commons.helpers.ensureBackgroundThread
import com.jakub.rpncalculator.extensions.calculatorDB
import com.jakub.rpncalculator.models.History

class HistoryHelper(val context: Context) {
    fun getHistory(callback: (calculations: ArrayList<History>) -> Unit) {
        ensureBackgroundThread {
            val notes = context.calculatorDB.getHistory() as ArrayList<History>

            Handler(Looper.getMainLooper()).post {
                callback(notes)
            }
        }
    }

    fun insertOrUpdateHistoryEntry(entry: History) {
        ensureBackgroundThread {
            context.calculatorDB.insertOrUpdate(entry)
        }
    }
}
