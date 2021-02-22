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
import com.qmobile.qmobileapi.connectivity.sdkNewerThanKitKat
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.viewmodel.ConnectivityViewModel

class ConnectivityViewModelFactory(
    private val application: Application,
    private val connectivityManager: ConnectivityManager
) : ViewModelProvider.NewInstanceFactory() {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return ConnectivityViewModel(
            application,
            connectivityManager
        ) as T
    }
}

fun getConnectivityViewModel(
    viewModelStoreOwner: ViewModelStoreOwner?,
    connectivityManager: ConnectivityManager
): ConnectivityViewModel? {
    viewModelStoreOwner?.run {
        return if (sdkNewerThanKitKat) {
            ViewModelProvider(
                this,
                ConnectivityViewModelFactory(
                    BaseApp.instance,
                    connectivityManager
                )
            )[ConnectivityViewModel::class.java]
        } else
            null
    } ?: throw IllegalStateException("Invalid Activity")
}
