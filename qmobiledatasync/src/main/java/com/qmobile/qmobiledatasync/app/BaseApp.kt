/*
 * Created by Quentin Marciset on 18/6/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.app

import android.app.Application
import androidx.multidex.MultiDexApplication
import com.qmobile.qmobiledatastore.db.AppDatabaseInterface
import com.qmobile.qmobiledatasync.utils.FromTableForViewModel
import com.qmobile.qmobiledatasync.utils.NavigationInterface
import com.qmobile.qmobiledatasync.utils.ViewDataBindingInterface

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
        lateinit var viewDataBindingInterface: ViewDataBindingInterface
    }

    override fun onCreate() {
        super.onCreate()

        // Sets Application instance
        instance = this
    }
}
