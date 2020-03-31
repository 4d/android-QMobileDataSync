/*
 * Created by Quentin Marciset on 31/3/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmarciset.androidmobiledatasync.viewmodel.factory

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.qmarciset.androidmobileapi.network.ApiService
import com.qmarciset.androidmobiledatastore.db.AppDatabaseInterface
import com.qmarciset.androidmobiledatasync.utils.FromTableForViewModel

class EntityViewModelFactory(
    private val application: Application,
    private val tableName: String,
    private val id: String,
    private val appDatabase: AppDatabaseInterface,
    private val apiService: ApiService,
    private val fromTableForViewModel: FromTableForViewModel
) : ViewModelProvider.NewInstanceFactory() {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return fromTableForViewModel.entityViewModelFromTable(
            application,
            tableName,
            id,
            appDatabase,
            apiService
        ) as T
    }
}
