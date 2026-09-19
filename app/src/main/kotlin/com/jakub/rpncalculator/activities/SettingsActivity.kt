package com.jakub.rpncalculator.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.fossify.commons.activities.CustomizationActivity
import org.fossify.commons.compose.extensions.enableEdgeToEdgeSimple
import org.fossify.commons.compose.theme.AppThemeSurface
import org.fossify.commons.helpers.IS_CUSTOMIZING_COLORS
import org.fossify.commons.helpers.isTiramisuPlus
import com.jakub.rpncalculator.compose.SettingsScreen
import com.jakub.rpncalculator.extensions.config
import com.jakub.rpncalculator.extensions.launchChangeAppLanguageIntent
import java.util.Locale
import kotlin.system.exitProcess

class SettingsActivity : AppCompatActivity() {

    private val preferences by lazy { config }

    @SuppressLint("NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdgeSimple()
        setContent {
            AppThemeSurface {
                val preventPhoneFromSleeping by preferences.preventPhoneFromSleepingFlow
                    .collectAsStateWithLifecycle(preferences.preventPhoneFromSleeping)
                val vibrateOnButtonPressFlow by preferences.vibrateOnButtonPressFlow
                    .collectAsStateWithLifecycle(preferences.vibrateOnButtonPress)
                val wasUseEnglishToggledFlow by preferences.wasUseEnglishToggledFlow
                    .collectAsStateWithLifecycle(preferences.wasUseEnglishToggled)
                val useEnglishFlow by preferences.useEnglishFlow
                    .collectAsStateWithLifecycle(preferences.useEnglish)
                val showCheckmarksOnSwitches by config.showCheckmarksOnSwitchesFlow
                    .collectAsStateWithLifecycle(initialValue = config.showCheckmarksOnSwitches)
                val isUseEnglishEnabled by remember(wasUseEnglishToggledFlow) {
                    derivedStateOf {
                        (wasUseEnglishToggledFlow || Locale.getDefault().language != "en") && !isTiramisuPlus()
                    }
                }
                val displayLanguage = remember { Locale.getDefault().displayLanguage }
                SettingsScreen(
                    displayLanguage = displayLanguage,
                    goBack = ::finish,
                    customizeColors = ::startCustomizationActivity,
                    customizeWidgetColors = ::setupCustomizeWidgetColors,
                    preventPhoneFromSleeping = preventPhoneFromSleeping,
                    onPreventPhoneFromSleeping = preferences::preventPhoneFromSleeping::set,
                    vibrateOnButtonPressFlow = vibrateOnButtonPressFlow,
                    onVibrateOnButtonPressFlow = preferences::vibrateOnButtonPress::set,
                    isUseEnglishEnabled = isUseEnglishEnabled,
                    isUseEnglishChecked = useEnglishFlow,
                    onUseEnglishPress = { isChecked ->
                        preferences.useEnglish = isChecked
                        exitProcess(0)
                    },
                    onSetupLanguagePress = ::launchChangeAppLanguageIntent,
                    showCheckmarksOnSwitches = showCheckmarksOnSwitches,
                )
            }
        }
    }

    private fun startCustomizationActivity() {
        startActivity(Intent(applicationContext, CustomizationActivity::class.java))
    }

    private fun setupCustomizeWidgetColors() {
        Intent(this, WidgetConfigureActivity::class.java).apply {
            putExtra(IS_CUSTOMIZING_COLORS, true)
            startActivity(this)
        }
    }
}
