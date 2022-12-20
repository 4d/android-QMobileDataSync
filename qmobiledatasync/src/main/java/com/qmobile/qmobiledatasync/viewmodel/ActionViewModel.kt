/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.viewmodel

import com.qmobile.qmobileapi.model.action.ActionResponse
import com.qmobile.qmobileapi.network.ApiService
import com.qmobile.qmobileapi.repository.ActionRepository
import com.qmobile.qmobileapi.utils.getSafeString
import com.qmobile.qmobileapi.utils.retrieveJSONObject
import com.qmobile.qmobileapi.utils.retrieveResponseObject
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.toast.ToastMessage
import okhttp3.RequestBody
import timber.log.Timber

class ActionViewModel(apiService: ApiService) : BaseViewModel() {

    init {
        Timber.v("ActionViewModel initializing...")
    }

    private var actionRepository: ActionRepository = ActionRepository(apiService)

    fun sendAction(
        actionName: String,
        actionContent: MutableMap<String, Any>,
        onResult: (actionResponse: ActionResponse?) -> Unit
    ) {
        actionRepository.sendAction(
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
                                "ActionViewModel",
                                ToastMessage.Type.SUCCESS
                            )
                        } else {
                            toastMessage.showMessage(
                                actionResponse.statusText,
                                "ActionViewModel",
                                ToastMessage.Type.NEUTRAL
                            )
                        }
                        onResult(actionResponse)
                    }
                }
            } else {
                treatFailure(response, error, "ActionViewModel", ToastMessage.Type.ERROR)
                onResult(null)
            }
        }
    }

    fun uploadImage(
        imagesToUpload: Map<String, RequestBody?>,
        onImageUploaded: (parameterName: String, receivedId: String) -> Unit,
        onError: () -> Unit,
        onAllUploadFinished: () -> Unit
    ) {
        actionRepository.uploadImage(
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
                    treatFailure(response, error, "ActionViewModel", ToastMessage.Type.ERROR)
                    onError()
                }
            }
        ) {
            onAllUploadFinished()
        }
    }

    override fun onCleared() {
        super.onCleared()
        actionRepository.disposable.dispose()
    }

    fun refreshActionRepository(apiService: ApiService) {
        actionRepository = ActionRepository(apiService)
    }
}
