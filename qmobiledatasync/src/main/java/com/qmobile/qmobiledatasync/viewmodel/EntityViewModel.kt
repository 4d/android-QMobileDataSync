/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.viewmodel

import androidx.lifecycle.LiveData
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobileapi.network.ApiService
import com.qmobile.qmobiledatastore.data.RoomEntity
import timber.log.Timber

class EntityViewModel<T : EntityModel>(
    tableName: String,
    id: String,
    apiService: ApiService
) :
    BaseDataViewModel<T>(tableName, apiService) {

    init {
        Timber.v("EntityViewModel initializing... $tableName")
    }

    /**
     * LiveData
     */

    val entity: LiveData<RoomEntity> = roomRepository.getOne(id)

    val doesEntityExist: LiveData<Boolean> = roomRepository.doesEntityExist(id)
}
