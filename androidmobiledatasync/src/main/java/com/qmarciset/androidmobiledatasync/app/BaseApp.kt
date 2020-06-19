/*
 * Created by Quentin Marciset on 18/6/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmarciset.androidmobiledatasync.app

import android.app.Application
import androidx.multidex.MultiDexApplication
import com.qmarciset.androidmobiledatastore.db.AppDatabaseInterface
import com.qmarciset.androidmobiledatasync.utils.FromTableForViewModel
import com.qmarciset.androidmobiledatasync.utils.NavigationInterface
import com.qmarciset.androidmobiledatasync.utils.ViewDataBindingInterface

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
