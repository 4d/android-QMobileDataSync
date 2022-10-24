/*
 * Created by Quentin Marciset on 31/3/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.viewmodel.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobileapi.network.ApiService
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.viewmodel.EntityListViewModel

class EntityListViewModelFactory(
    private val tableName: String,
    private val apiService: ApiService,
    private val newInstance: Boolean
) : ViewModelProvider.NewInstanceFactory() {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val key = tableName + LIST_VIEWMODEL_BASENAME

        return when {
            newInstance -> BaseApp.genericTableHelper.entityListViewModelFromTable(tableName, apiService) as T
            viewModelMap.containsKey(key) -> viewModelMap[key] as T
            else -> {
                addViewModel(key, BaseApp.genericTableHelper.entityListViewModelFromTable(tableName, apiService))
                viewModelMap[key] as T
            }
        }
    }

    companion object {

        const val LIST_VIEWMODEL_BASENAME = "EntityListViewModel"

        // The HashMap is here to ensure that fragments use the same viewModel instance which
        // the activity already created. Without this HashMap, I experienced unwanted behaviour
        // such as creation of a new viewModel instance every time we land on the fragment view
        val viewModelMap = HashMap<String, ViewModel>()

        fun addViewModel(key: String, viewModel: ViewModel) {
            viewModelMap[key] = viewModel
        }
    }
}

fun getEntityListViewModel(
    viewModelStoreOwner: ViewModelStoreOwner?,
    tableName: String,
    apiService: ApiService,
    newInstance: Boolean = false
): EntityListViewModel<EntityModel> {
    val clazz = BaseApp.genericTableHelper.entityListViewModelClassFromTable(tableName)
    viewModelStoreOwner?.run {
        return ViewModelProvider(this, EntityListViewModelFactory(tableName, apiService, newInstance))[clazz]
    } ?: throw IllegalStateException("Invalid Activity")
}
