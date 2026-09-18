package com.jakub.rpncalculator.activities

import android.content.Context
import android.content.Intent
import org.fossify.commons.activities.BaseSplashActivity
import org.fossify.commons.helpers.SIDELOADING_FALSE
import com.jakub.rpncalculator.extensions.config

class SplashActivity : BaseSplashActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase)
        // Skips BaseSplashActivity's "fake/corrupt app" sideloading dialog, which is meant to
        // deter unofficial Fossify clones and doesn't apply to this independent fork.
        config.appSideloadingStatus = SIDELOADING_FALSE
    }

    override fun initActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
