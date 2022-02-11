/*
 * Created by qmarciset on 9/12/2021.
 * 4D SAS
 * Copyright (c) 2021 qmarciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.network

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.lifecycle.LiveData
import timber.log.Timber

open class NetworkStateMonitor(private val connectivityManager: ConnectivityManager) :
    LiveData<NetworkStateEnum>() {

    private val networkStateObject = object : ConnectivityManager.NetworkCallback() {
        override fun onLost(network: Network) {
            super.onLost(network)
            postValue(NetworkStateEnum.CONNECTION_LOST)
        }

        override fun onUnavailable() {
            super.onUnavailable()
            postValue(NetworkStateEnum.DISCONNECTED)
        }

        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            postValue(NetworkStateEnum.CONNECTED)
        }
    }

    override fun onActive() {
        super.onActive()
        connectivityManager.registerNetworkCallback(networkRequestBuilder(), networkStateObject)
    }

    override fun onInactive() {
        super.onInactive()
        try {
            connectivityManager.unregisterNetworkCallback(networkStateObject)
        } catch (e: IllegalArgumentException) {
            Timber.d(e.message.orEmpty())
            Timber.d("NetworkCallback for Wi-fi was not registered or already unregistered")
        }
    }

    private fun networkRequestBuilder(): NetworkRequest {
        return NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .build()
    }
}
