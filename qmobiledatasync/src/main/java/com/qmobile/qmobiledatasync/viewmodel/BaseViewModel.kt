/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.viewmodel

import androidx.lifecycle.AndroidViewModel
import com.google.gson.JsonSyntaxException
import com.qmobile.qmobileapi.model.action.ActionContent
import com.qmobile.qmobileapi.model.action.ActionResponse
import com.qmobile.qmobileapi.network.ApiService
import com.qmobile.qmobileapi.repository.RestRepository
import com.qmobile.qmobileapi.utils.parseToType
import com.qmobile.qmobileapi.utils.retrieveJSONObject
import com.qmobile.qmobiledatastore.dao.BaseDao
import com.qmobile.qmobiledatastore.repository.RoomRepository
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.toast.MessageType
import com.qmobile.qmobiledatasync.toast.ToastMessage
import timber.log.Timber

/**
 * If you need to use context inside your viewmodel you should use AndroidViewModel, because it
 * contains the application context (to retrieve the context call getApplication() ), otherwise use
 * regular ViewModel.
 */
abstract class BaseViewModel<T>(
    private val tableName: String,
    apiService: ApiService
) : AndroidViewModel(BaseApp.instance) {

    open fun getAssociatedTableName(): String = tableName
    private val originalAssociatedTableName = BaseApp.genericTableHelper.originalTableName(tableName)

    /**
     * DAO
     */

    var dao: BaseDao<T> = BaseApp.daoProvider.getDao(tableName)

    /**
     * Repositories
     */

    open val roomRepository: RoomRepository<T> = RoomRepository(dao)
    var restRepository: RestRepository =
        RestRepository(originalAssociatedTableName, apiService)

    fun refreshRestRepository(apiService: ApiService) {
        restRepository = RestRepository(originalAssociatedTableName, apiService)
    }

    /**
     * LiveData
     */
    val toastMessage: ToastMessage = ToastMessage()

    override fun onCleared() {
        super.onCleared()
        restRepository.disposable.dispose()
    }

    fun sendAction(
        actionName: String,
        actionContent: ActionContent,
        onResult: (actionResponse: ActionResponse?) -> Unit
    ) {
        restRepository.sendAction(
            actionName,
            actionContent
        ) { isSuccess, response, error ->
            if (isSuccess) {
                response?.body()?.let { responseBody ->
                    retrieveJSONObject(responseBody.string())?.let { responseJson ->

                        val actionResponse =
                            try {
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
                            Timber.e("cannot decode ActionResponse from json")
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
