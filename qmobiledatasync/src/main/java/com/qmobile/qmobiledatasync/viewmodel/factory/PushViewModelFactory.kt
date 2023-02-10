/*
 * Created by Quentin Marciset on 31/3/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.viewmodel.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.qmobile.qmobileapi.network.ApiService
import com.qmobile.qmobiledatasync.viewmodel.PushViewModel

class PushViewModelFactory(
    private val apiService: ApiService
) : ViewModelProvider.NewInstanceFactory() {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return PushViewModel(apiService) as T
    }
}

fun getPushViewModel(
    viewModelStoreOwner: ViewModelStoreOwner?,
    apiService: ApiService
): PushViewModel {
    viewModelStoreOwner?.run {
        return ViewModelProvider(
            this,
            PushViewModelFactory(apiService)
        )[PushViewModel::class.java]
    } ?: throw IllegalStateException("Invalid Activity")
}
