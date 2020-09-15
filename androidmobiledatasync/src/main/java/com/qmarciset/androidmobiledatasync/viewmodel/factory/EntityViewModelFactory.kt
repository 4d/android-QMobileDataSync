/*
 * Created by Quentin Marciset on 31/3/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmarciset.androidmobiledatasync.viewmodel.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.qmarciset.androidmobileapi.network.ApiService
import com.qmarciset.androidmobiledatasync.app.BaseApp

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
                BaseApp.fromTableForViewModel.entityViewModelFromTable(
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
