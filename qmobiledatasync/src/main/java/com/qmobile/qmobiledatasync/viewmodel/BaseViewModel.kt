/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.viewmodel

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.qmobile.qmobileapi.network.ApiService
import com.qmobile.qmobileapi.repository.RestRepository
import com.qmobile.qmobiledatastore.dao.BaseDao
import com.qmobile.qmobiledatastore.repository.RoomRepository
import com.qmobile.qmobiledatasync.app.BaseApp

/**
 * If you need to use context inside your viewmodel you should use AndroidViewModel, because it
 * contains the application context (to retrieve the context call getApplication() ), otherwise use
 * regular ViewModel.
 */
abstract class BaseViewModel<T>(
    private val tableName: String,
    apiService: ApiService
) : AndroidViewModel(BaseApp.instance) {

    open fun getAssociatedTableName(): String = tableName

    /**
     * DAO
     */

    var dao: BaseDao<T> = BaseApp.appDatabaseInterface.getDao(tableName)

    /**
     * Repositories
     */

    val roomRepository: RoomRepository<T> = RoomRepository(dao)
    var restRepository: RestRepository = RestRepository(tableName, apiService)

    fun refreshRestRepository(apiService: ApiService) {
        restRepository = RestRepository(tableName, apiService)
    }

    /**
     * LiveData
     */

    val toastMessage = MutableLiveData<String>()
}