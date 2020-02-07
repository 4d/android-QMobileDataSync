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
    fun <T> entityListViewModelFromTable(
        tableName: String,
        application: Application,
        appDatabase: AppDatabaseInterface,
        apiService: ApiService,
        fromTableForViewModel: FromTableForViewModel
    ): EntityListViewModel<T>
}
