/*
 * Created by Quentin Marciset on 31/3/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.viewmodel.factory

import android.app.Application
import android.net.ConnectivityManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.qmobile.qmobileapi.network.AccessibilityApiService
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.viewmodel.ConnectivityViewModel

class ConnectivityViewModelFactory(
    private val application: Application,
    private val connectivityManager: ConnectivityManager,
    private val accessibilityApiService: AccessibilityApiService
) : ViewModelProvider.NewInstanceFactory() {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return ConnectivityViewModel(
            application,
            connectivityManager,
            accessibilityApiService
        ) as T
    }
}

fun getConnectivityViewModel(
    viewModelStoreOwner: ViewModelStoreOwner?,
    connectivityManager: ConnectivityManager,
    accessibilityApiService: AccessibilityApiService
): ConnectivityViewModel {
    viewModelStoreOwner?.run {
        return ViewModelProvider(
            this,
            ConnectivityViewModelFactory(
                BaseApp.instance,
                connectivityManager,
                accessibilityApiService
            )
        )[ConnectivityViewModel::class.java]
    } ?: throw IllegalStateException("Invalid Activity")
}
