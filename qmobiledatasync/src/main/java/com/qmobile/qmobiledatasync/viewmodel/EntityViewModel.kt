/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.viewmodel

import androidx.lifecycle.LiveData
import com.qmobile.qmobileapi.model.action.ActionContent
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobileapi.network.ApiService
import com.qmobile.qmobiledatastore.data.RoomRelation
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

    open val entity: LiveData<T> = roomRepository.getOne(id)

    abstract fun setRelationToLayout(relationName: String, roomRelation: RoomRelation)

    fun sendAction(actionName: String, actionContent: ActionContent) {
        restRepository.sendAction(
            actionName,
            actionContent
        ) { isSuccess, response, error ->
        }
    }
}
