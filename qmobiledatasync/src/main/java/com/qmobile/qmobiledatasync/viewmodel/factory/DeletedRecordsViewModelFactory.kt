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
import com.qmobile.qmobiledatasync.viewmodel.DeletedRecordsViewModel

class DeletedRecordsViewModelFactory(
    private val apiService: ApiService
) : ViewModelProvider.NewInstanceFactory() {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return DeletedRecordsViewModel(apiService) as T
    }
}

fun getDeletedRecordsViewModel(
    viewModelStoreOwner: ViewModelStoreOwner?,
    apiService: ApiService
): DeletedRecordsViewModel {
    viewModelStoreOwner?.run {
        return ViewModelProvider(
            this,
            DeletedRecordsViewModelFactory(apiService)
        )[DeletedRecordsViewModel::class.java]
    } ?: throw IllegalStateException("Invalid Activity")
}
