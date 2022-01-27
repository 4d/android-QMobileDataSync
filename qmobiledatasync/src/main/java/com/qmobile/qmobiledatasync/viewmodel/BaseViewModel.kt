/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.viewmodel

import androidx.lifecycle.AndroidViewModel
import com.qmobile.qmobileapi.model.action.ActionResponse
import com.qmobile.qmobileapi.network.ApiService
import com.qmobile.qmobileapi.repository.RestRepository
import com.qmobile.qmobileapi.utils.getSafeString
import com.qmobile.qmobileapi.utils.retrieveJSONObject
import com.qmobile.qmobileapi.utils.retrieveResponseObject
import com.qmobile.qmobiledatastore.dao.BaseDao
import com.qmobile.qmobiledatastore.repository.RoomRepository
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.toast.MessageType
import com.qmobile.qmobiledatasync.toast.ToastMessage
import okhttp3.RequestBody

/**
 * If you need to use context inside your viewmodel you should use AndroidViewModel, because it
 * contains the application context (to retrieve the context call getApplication() ), otherwise use
 * regular ViewModel.
 */
abstract class BaseViewModel<T : Any>(
    private val tableName: String,
    apiService: ApiService
) : AndroidViewModel(BaseApp.instance) {

    open fun getAssociatedTableName(): String = tableName
    private val originalAssociatedTableName =
        BaseApp.genericTableHelper.originalTableName(tableName)

    /**
     * DAO
     */

    var dao: BaseDao<T> = BaseApp.daoProvider.getDao(tableName)

    /**
     * Repositories
     */

    val roomRepository: RoomRepository<T> = RoomRepository(dao)
    var restRepository: RestRepository =
        RestRepository(originalAssociatedTableName, apiService)

    fun refreshRestRepository(apiService: ApiService) {
        restRepository = RestRepository(originalAssociatedTableName, apiService)
    }

    /**
     * LiveData
     */
    val toastMessage: ToastMessage = ToastMessage()

    fun sendAction(
        actionName: String,
        actionContent: MutableMap<String, Any>,
        onResult: (actionResponse: ActionResponse?) -> Unit
    ) {
        restRepository.sendAction(
            actionName,
            actionContent
        ) { isSuccess, response, error ->
            if (isSuccess) {
                response?.body()?.let { responseBody ->
                    retrieveResponseObject<ActionResponse>(
                        BaseApp.mapper,
                        responseBody.string()
                    )?.let { actionResponse ->
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
                    }
                }
            } else {
                response?.let {
                    toastMessage.showMessage(it, getAssociatedTableName(), MessageType.ERROR)
                }
                error?.let {
                    toastMessage.showMessage(it, getAssociatedTableName(), MessageType.ERROR)
                }
            }
        }
    }

    fun uploadImage(
        imagesToUpload: Map<String, RequestBody?>,
        onImageUploaded: (parameterName: String, receivedId: String) -> Unit,
        onAllUploadFinished: () -> Unit
    ) {
        restRepository.uploadImage(
            imagesToUpload,
            { isSuccess, parameterName, response, error ->
                if (isSuccess) {
                    response?.body()?.let { responseBody ->

                        retrieveJSONObject(responseBody.string())?.let { responseJson ->
                            responseJson.getSafeString("ID")?.let { id ->
                                onImageUploaded(parameterName, id)
                            }
                        }
                    }
                } else {
                    response?.let {
                        toastMessage.showMessage(it, getAssociatedTableName(), MessageType.ERROR)
                    }
                    error?.let {
                        toastMessage.showMessage(it, getAssociatedTableName(), MessageType.ERROR)
                    }
                }
            }
        ) {
            onAllUploadFinished()
        }
    }
}
