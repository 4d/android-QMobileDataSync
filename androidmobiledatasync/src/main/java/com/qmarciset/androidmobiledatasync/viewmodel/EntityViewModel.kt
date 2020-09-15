/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmarciset.androidmobiledatasync.viewmodel

import androidx.lifecycle.LiveData
import com.qmarciset.androidmobileapi.model.entity.EntityModel
import com.qmarciset.androidmobileapi.network.ApiService
import com.qmarciset.androidmobiledatastore.data.RoomRelation
import timber.log.Timber

abstract class EntityViewModel<T : EntityModel>(
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

    abstract fun getManyToOneRelationKeysFromEntity(entity: EntityModel): Map<String, LiveData<List<RoomRelation>>>

    abstract fun setRelationToLayout(relationName: String, roomRelation: RoomRelation)
}
