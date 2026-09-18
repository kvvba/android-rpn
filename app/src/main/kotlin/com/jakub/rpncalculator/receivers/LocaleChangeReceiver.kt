package com.jakub.rpncalculator.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.jakub.rpncalculator.extensions.updateWidgets

class LocaleChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_LOCALE_CHANGED == intent.action) {
            context.updateWidgets()
        }
    }
}
