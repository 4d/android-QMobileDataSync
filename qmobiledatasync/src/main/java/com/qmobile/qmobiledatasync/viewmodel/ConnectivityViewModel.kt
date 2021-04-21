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
import com.qmobile.qmobileapi.connectivity.NetworkStateEnum
import com.qmobile.qmobileapi.connectivity.NetworkStateMonitor
import com.qmobile.qmobileapi.connectivity.isConnected
import com.qmobile.qmobileapi.network.AccessibilityApiService
import com.qmobile.qmobileapi.repository.AccessibilityRepository
import com.qmobile.qmobiledatasync.toast.ToastMessage
import timber.log.Timber

open class ConnectivityViewModel(
    application: Application,
    private val connectivityManager: ConnectivityManager,
    accessibilityApiService: AccessibilityApiService
) :
    AndroidViewModel(application) {

    init {
        Timber.v("ConnectivityViewModel initializing...")
    }

    private val accessibilityRepository = AccessibilityRepository(accessibilityApiService)

    /**
     * LiveData
     */

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    open val networkStateMonitor: LiveData<NetworkStateEnum> =
        NetworkStateMonitor(
            connectivityManager
        )

    val toastMessage: ToastMessage = ToastMessage()

    /**
     * Request to server to check if it is accessible or not.
     * Performed before data sync and in settings.
     */
    fun isServerConnectionOk(
        onResult: (success: Boolean) -> Unit
    ) {
        accessibilityRepository.checkAccessibility { isSuccess, response, error ->
            if (isSuccess) {
                Timber.d("Server ping successful")
                onResult(true)
            } else {
                Timber.d("Server ping unsuccessful")
                response?.let { toastMessage.showMessage(it, "ConnectivityViewModel") }
                error?.let { toastMessage.showMessage(it, "ConnectivityViewModel") }
                onResult(false)
            }
        }
    }

    fun isConnected(): Boolean =
        connectivityManager.isConnected(networkStateMonitor.value)

    override fun onCleared() {
        super.onCleared()
        accessibilityRepository.disposable.dispose()
    }
}
