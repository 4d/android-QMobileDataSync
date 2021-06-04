/*
 * Created by Quentin Marciset on 18/6/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.app

import android.app.Application
import android.content.res.Configuration
import androidx.multidex.MultiDexApplication
import com.qmobile.qmobiledatastore.db.AppDatabaseInterface
import com.qmobile.qmobiledatasync.utils.FragmentUtil
import com.qmobile.qmobiledatasync.utils.FromTableForViewModel
import com.qmobile.qmobiledatasync.utils.NavigationInterface

open class BaseApp : MultiDexApplication() {

    companion object {

        // Provides Application instance
        lateinit var instance: Application

        // Provides the drawable resource id for login page logo
        var loginLogoDrawable: Int? = null

        // Provides the menu resource id for bottom navigation
        var bottomNavigationMenu: Int? = null

        // Provides navigation graphs id list for navigation
        lateinit var navGraphIds: List<Int>

        // Provides the embedded images
        lateinit var embeddedFiles: List<String>

        // Provides interfaces to get data coming from outside the SDK
        lateinit var appDatabaseInterface: AppDatabaseInterface
        lateinit var fromTableForViewModel: FromTableForViewModel
        lateinit var navigationInterface: NavigationInterface
        lateinit var fragmentUtil: FragmentUtil

        // Provides if dark mode is enabled
        fun nightMode(): Boolean = if (::instance.isInitialized) {
            when (instance.resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK)) {
                Configuration.UI_MODE_NIGHT_YES -> true
                Configuration.UI_MODE_NIGHT_NO -> false
                Configuration.UI_MODE_NIGHT_UNDEFINED -> false
                else -> false
            }
        } else {
            false
        }
    }

    override fun onCreate() {
        super.onCreate()

        // Sets Application instance
        instance = this
    }
}
