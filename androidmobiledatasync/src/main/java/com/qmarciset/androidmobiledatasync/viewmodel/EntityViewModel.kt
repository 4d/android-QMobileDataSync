/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmarciset.androidmobiledatasync.viewmodel

import androidx.lifecycle.LiveData
import com.qmarciset.androidmobileapi.network.ApiService
import timber.log.Timber

open class EntityViewModel<T>(
    tableName: String,
    id: String,
    apiService: ApiService
) :
    BaseViewModel<T>(tableName, apiService) {

    init {
        Timber.i("EntityViewModel initializing... $tableName")
    }

    /**
     * LiveData
     */

    open val entity: LiveData<T> = roomRepository.getOne(id)
}
