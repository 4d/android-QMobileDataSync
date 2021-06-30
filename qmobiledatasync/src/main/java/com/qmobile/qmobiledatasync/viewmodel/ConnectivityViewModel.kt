/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.viewmodel

import android.app.Application
import android.net.ConnectivityManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.qmobile.qmobileapi.connectivity.NetworkStateEnum
import com.qmobile.qmobileapi.connectivity.NetworkStateMonitor
import com.qmobile.qmobileapi.network.AccessibilityApiService
import com.qmobile.qmobileapi.repository.AccessibilityRepository
import com.qmobile.qmobiledatasync.toast.MessageType
import com.qmobile.qmobiledatasync.toast.ToastMessage
import timber.log.Timber

open class ConnectivityViewModel(
    application: Application,
    connectivityManager: ConnectivityManager,
    accessibilityApiService: AccessibilityApiService
) :
    AndroidViewModel(application) {

    init {
        Timber.v("ConnectivityViewModel initializing...")
    }

    private var accessibilityRepository = AccessibilityRepository(accessibilityApiService)

    /**
     * LiveData
     */

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
        toastError: Boolean = true,
        onResult: (success: Boolean) -> Unit
    ) {
        accessibilityRepository.checkAccessibility { isSuccess, response, error ->
            if (isSuccess) {
                Timber.d("Server ping successful")
                onResult(true)
            } else {
                Timber.d("Server ping unsuccessful")
                response?.let {
                    onResult(true)
                    return@checkAccessibility
                }
                if (toastError)
                    error?.let { toastMessage.showMessage(it, "ConnectivityViewModel", MessageType.ERROR) }
                onResult(false)
            }
        }
    }

    fun isConnected(): Boolean = networkStateMonitor.value == NetworkStateEnum.CONNECTED

    override fun onCleared() {
        super.onCleared()
        accessibilityRepository.disposable.dispose()
    }

    fun refreshAccessibilityRepository(accessibilityApiService: AccessibilityApiService) {
        accessibilityRepository = AccessibilityRepository(accessibilityApiService)
    }
}
