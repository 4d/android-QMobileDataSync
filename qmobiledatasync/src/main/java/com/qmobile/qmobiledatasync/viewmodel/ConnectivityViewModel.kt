/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.viewmodel

import android.app.Application
import android.net.ConnectivityManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.qmobile.qmobileapi.connectivity.NetworkStateEnum
import com.qmobile.qmobileapi.connectivity.NetworkStateMonitor
import com.qmobile.qmobileapi.connectivity.ServerAccessibility
import com.qmobile.qmobileapi.utils.PING_TIMEOUT
import timber.log.Timber
import java.net.URL

open class ConnectivityViewModel(
    application: Application,
    connectivityManager: ConnectivityManager
) :
    AndroidViewModel(application) {

    init {
        Timber.i("ConnectivityViewModel initializing...")
    }

    open val serverAccessibility = ServerAccessibility()

    /**
     * LiveData
     */

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    open val networkStateMonitor: LiveData<NetworkStateEnum> =
        NetworkStateMonitor(
            connectivityManager
        )

    open val serverAccessible = MutableLiveData<Boolean>().apply { value = false }

    /**
     * Pings server to check if it is accessible or not
     */
    open fun checkAccessibility(remoteUrl: String) {
        val url = URL(remoteUrl)
        serverAccessibility.pingServer(
            url.host,
            url.port,
            PING_TIMEOUT
        ) { isAccessible, throwable ->
            throwable?.let {
                Timber.e("Error occurred while pinging server with url [$url] : ${throwable.message}")
            }

            serverAccessible.postValue(isAccessible ?: false)
        }
    }

    override fun onCleared() {
        super.onCleared()
        serverAccessibility.disposable.dispose()
    }
}
