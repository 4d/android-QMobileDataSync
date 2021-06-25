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
import com.qmobile.qmobiledatasync.viewmodel.EntityViewModel

class EntityViewModelFactory(
    private val tableName: String,
    private val id: String,
    private val apiService: ApiService
) : ViewModelProvider.NewInstanceFactory() {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {

        val key = tableName + VIEWMODEL_BASENAME + id

        return if (viewModelMap.containsKey(key)) {
            viewModelMap[key] as T
        } else {
            addViewModel(
                key,
                BaseApp.genericTableHelper.entityViewModelFromTable(
                    tableName,
                    id,
                    apiService
                )
            )
            viewModelMap[key] as T
        }
    }

    companion object {

        const val VIEWMODEL_BASENAME = "EntityViewModel"

        // The HashMap is here to ensure that fragments use the same viewModel instance which
        // the activity already created. Without this HashMap, I experienced unwanted behaviour
        // such as creation of a new viewModel instance every time we land on the fragment view
        val viewModelMap = HashMap<String, ViewModel>()

        fun addViewModel(key: String, viewModel: ViewModel) {
            viewModelMap[key] = viewModel
        }
    }
}

fun getEntityViewModel(
    viewModelStoreOwner: ViewModelStoreOwner?,
    tableName: String,
    itemId: String,
    apiService: ApiService
): EntityViewModel<EntityModel> {
    val clazz = BaseApp.genericTableHelper.entityViewModelClassFromTable(tableName)
    viewModelStoreOwner?.run {
        return ViewModelProvider(
            this,
            EntityViewModelFactory(
                tableName,
                itemId,
                apiService
            )
        )[clazz]
    } ?: throw IllegalStateException("Invalid Activity")
}
