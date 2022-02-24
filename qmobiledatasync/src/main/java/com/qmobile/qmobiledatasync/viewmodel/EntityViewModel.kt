/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.viewmodel

import androidx.lifecycle.LiveData
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobileapi.network.ApiService
import com.qmobile.qmobiledatastore.data.RoomData
import timber.log.Timber

abstract class EntityViewModel<T : EntityModel>(
    tableName: String,
    id: String,
    apiService: ApiService
) :
    BaseViewModel<T>(tableName, apiService) {

    init {
        Timber.v("EntityViewModel initializing... $tableName")
    }

    /**
     * LiveData
     */

    open val entity: LiveData<T> = roomRepository.getOne(id) as LiveData<T>

    abstract fun setRelationToLayout(relationName: String, roomRelation: List<RoomData>)
}
