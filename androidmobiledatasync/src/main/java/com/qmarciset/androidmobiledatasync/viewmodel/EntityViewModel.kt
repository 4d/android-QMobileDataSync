/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmarciset.androidmobiledatasync.viewmodel

import android.app.Application
import androidx.lifecycle.LiveData
import com.qmarciset.androidmobileapi.network.ApiService
import com.qmarciset.androidmobiledatastore.db.AppDatabaseInterface
import timber.log.Timber

open class EntityViewModel<T>(
    application: Application,
    tableName: String,
    id: String,
    appDatabase: AppDatabaseInterface,
    apiService: ApiService
) :
    BaseViewModel<T>(application, tableName, appDatabase, apiService) {

    init {
        Timber.i("EntityViewModel initializing... $tableName")
    }

    /**
     * LiveData
     */

    open val entity: LiveData<T> = roomRepository.getOne(id)
}
