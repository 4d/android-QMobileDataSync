/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmarciset.androidmobiledatasync.utils

import android.app.Application
import com.qmarciset.androidmobileapi.model.entity.EntityModel
import com.qmarciset.androidmobileapi.network.ApiService
import com.qmarciset.androidmobiledatastore.db.AppDatabaseInterface
import com.qmarciset.androidmobiledatasync.viewmodel.EntityListViewModel
import com.qmarciset.androidmobiledatasync.viewmodel.EntityViewModel

/**
 * Interface implemented by MainActivity to provide different elements depending of the generated type
 */
interface FromTableForViewModel {

    /**
     * Provides the appropriate Entity
     */
    fun parseEntityFromTable(tableName: String, jsonString: String): EntityModel

    /**
     * Checks if entity has a __GlobalStamp property
     */
    fun hasGlobalStampPropertyFromTable(tableName: String): Boolean

    /**
     * Provides the appropriate EntityListViewModel
     */
    fun entityListViewModelFromTable(
        application: Application,
        tableName: String,
        appDatabase: AppDatabaseInterface,
        apiService: ApiService,
        fromTableForViewModel: FromTableForViewModel
    ): EntityListViewModel<*>

    /**
     * Provides the appropriate EntityViewModel
     */
    fun entityViewModelFromTable(
        application: Application,
        tableName: String,
        id: String,
        appDatabase: AppDatabaseInterface,
        apiService: ApiService
    ): EntityViewModel<*>
}
