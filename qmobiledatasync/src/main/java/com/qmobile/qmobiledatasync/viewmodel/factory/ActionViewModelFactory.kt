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
import com.qmobile.qmobiledatasync.viewmodel.ActionViewModel

class ActionViewModelFactory(
    private val apiService: ApiService
) : ViewModelProvider.NewInstanceFactory() {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ActionViewModel(apiService) as T
    }
}

fun getActionViewModel(
    viewModelStoreOwner: ViewModelStoreOwner?,
    apiService: ApiService
): ActionViewModel {
    viewModelStoreOwner?.run {
        return ViewModelProvider(
            this,
            ActionViewModelFactory(apiService)
        )[ActionViewModel::class.java]
    } ?: throw IllegalStateException("Invalid Activity")
}
