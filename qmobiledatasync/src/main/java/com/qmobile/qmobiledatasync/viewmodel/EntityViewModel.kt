/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.qmobile.qmobileapi.model.action.ActionContent
import com.qmobile.qmobileapi.model.action.ActionResponse
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobileapi.network.ApiService
import com.qmobile.qmobileapi.utils.parseToType
import com.qmobile.qmobileapi.utils.retrieveJSONObject
import com.qmobile.qmobiledatastore.data.RoomRelation
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.toast.MessageType
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

    fun sendAction(
        actionName: String,
        selectedActionId: String?,
        onResult: (actionResponse: ActionResponse?) -> Unit
    ) {
        restRepository.sendAction(
            actionName,
            selectedActionId
        ) { isSuccess, response, error ->
            if (isSuccess) {
                response?.body()?.let { responseBody ->
                    retrieveJSONObject(responseBody.string())?.let { responseJson ->
                        val actionResponse =  try {
                            BaseApp.mapper.parseToType<ActionResponse>(responseJson.toString())
                        } catch (e: JsonSyntaxException) {
                            Timber.w("Failed to decode auth response ${e.localizedMessage}: $responseJson")
                            null
                        }
                        if (actionResponse != null) {
                            if (actionResponse.success) {
                                toastMessage.showMessage(
                                    actionResponse.statusText,
                                    getAssociatedTableName(),
                                    MessageType.SUCCESS
                                )
                            } else {
                                toastMessage.showMessage(
                                    actionResponse.statusText,
                                    getAssociatedTableName(),
                                    MessageType.ERROR
                                )
                            }
                            onResult(actionResponse)
                        } else {
                            Log.e("EntityViewModel:", "cannot decode ActionResponse from json")
                        }
                    }
                }
            } else {
                response?.let {
                    toastMessage.showMessage(it, getAssociatedTableName(), MessageType.ERROR)
                }
                error?.let {
                    toastMessage.showMessage(
                        it,
                        getAssociatedTableName(),
                        MessageType.ERROR
                    )
                }
            }
        }
    }
}
