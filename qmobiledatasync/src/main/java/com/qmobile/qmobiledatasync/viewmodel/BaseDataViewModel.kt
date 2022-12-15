/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.viewmodel

import com.qmobile.qmobileapi.network.ApiService
import com.qmobile.qmobileapi.repository.RestRepository
import com.qmobile.qmobiledatastore.dao.BaseDao
import com.qmobile.qmobiledatastore.data.RoomData
import com.qmobile.qmobiledatastore.data.RoomEntity
import com.qmobile.qmobiledatastore.repository.RoomRepository
import com.qmobile.qmobiledatasync.app.BaseApp

/**
 * If you need to use context inside your viewmodel you should use AndroidViewModel, because it
 * contains the application context (to retrieve the context call getApplication() ), otherwise use
 * regular ViewModel.
 */
abstract class BaseDataViewModel<T : Any>(
    private val tableName: String,
    apiService: ApiService
) : BaseViewModel() {

    open fun getAssociatedTableName(): String = tableName
    private val originalAssociatedTableName = BaseApp.runtimeDataHolder.tableInfo[tableName]?.originalName ?: ""

    /**
     * DAO
     */

    var dao: BaseDao<RoomEntity, RoomData> = BaseApp.daoProvider.getDao(tableName)

    /**
     * Repositories
     */

    val roomRepository: RoomRepository<RoomData> = RoomRepository(dao)
    internal var restRepository: RestRepository =
        RestRepository(originalAssociatedTableName, apiService)

    fun refreshRestRepository(apiService: ApiService) {
        restRepository = RestRepository(originalAssociatedTableName, apiService)
    }

    override fun onCleared() {
        super.onCleared()
        restRepository.disposable.dispose()
    }
}
