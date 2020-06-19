/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmarciset.androidmobiledatasync.viewmodel

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.qmarciset.androidmobileapi.network.ApiService
import com.qmarciset.androidmobileapi.repository.RestRepository
import com.qmarciset.androidmobiledatastore.dao.BaseDao
import com.qmarciset.androidmobiledatastore.repository.RoomRepository
import com.qmarciset.androidmobiledatasync.app.BaseApp

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
    val restRepository: RestRepository = RestRepository(tableName, apiService)

    /**
     * LiveData
     */

    val toastMessage = MutableLiveData<String>()
}
