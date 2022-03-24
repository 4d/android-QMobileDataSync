/*
 * Created by Quentin Marciset on 18/6/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.app

import android.app.Application
import android.content.res.Configuration
import androidx.multidex.MultiDexApplication
import com.fasterxml.jackson.databind.ObjectMapper
import com.qmobile.qmobileapi.utils.SharedPreferencesHolder
import com.qmobile.qmobiledatastore.db.DaoProvider
import com.qmobile.qmobiledatasync.utils.GenericNavigationResolver
import com.qmobile.qmobiledatasync.utils.GenericRelationHelper
import com.qmobile.qmobiledatasync.utils.GenericTableFragmentHelper
import com.qmobile.qmobiledatasync.utils.GenericTableHelper
import com.qmobile.qmobiledatasync.utils.RuntimeDataHolder
import java.util.Date

const val MILLISECONDS_IN_SECONDS = 1000
const val SECONDS_IN_MINUTE = 60


open class BaseApp : MultiDexApplication() {

    companion object {

        // Provides Application instance
        lateinit var instance: Application

        lateinit var sharedPreferencesHolder: SharedPreferencesHolder

        lateinit var runtimeDataHolder: RuntimeDataHolder

        lateinit var mapper: ObjectMapper

        // Provides the drawable resource id for login page logo
        var loginLogoDrawable: Int? = null

        // Provides the menu resource id for bottom navigation
        var bottomNavigationMenu: Int? = null

        // Provides navigation graphs id list for navigation
        lateinit var navGraphIds: List<Int>

        // Provides interfaces to get data coming from outside the SDK
        lateinit var daoProvider: DaoProvider
        lateinit var genericTableHelper: GenericTableHelper
        lateinit var genericRelationHelper: GenericRelationHelper
        lateinit var genericTableFragmentHelper: GenericTableFragmentHelper
        lateinit var genericNavigationResolver: GenericNavigationResolver

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

        fun shouldShowActionError(): Boolean {
            var sharedPreferencesHolder = BaseApp.sharedPreferencesHolder
            val lastDisplayErrorTime =
                Date(sharedPreferencesHolder.lastTimeActionErrorDisplayed).time
            val diffInSeconds = (Date().time - lastDisplayErrorTime) / MILLISECONDS_IN_SECONDS

            if (diffInSeconds >= SECONDS_IN_MINUTE) {
                // reset lastTimeActionErrorDisplayed with the currentTime
                sharedPreferencesHolder.lastTimeActionErrorDisplayed = Date().time
                return true
            }
            return false
        }

    }

    override fun onCreate() {
        super.onCreate()

        // Sets Application instance
        instance = this
    }
}
