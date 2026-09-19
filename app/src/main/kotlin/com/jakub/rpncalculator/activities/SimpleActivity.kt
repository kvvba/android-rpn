package com.jakub.rpncalculator.activities

import org.fossify.commons.activities.BaseSimpleActivity
import com.jakub.rpncalculator.R

open class SimpleActivity : BaseSimpleActivity() {
    // This app ships a single launcher icon; icon customization is not offered.
    override fun getAppIconIDs() = arrayListOf(R.mipmap.ic_launcher)

    override fun getAppLauncherName() = getString(R.string.app_launcher_name)

    override fun getRepositoryName() = "Calculator"
}
