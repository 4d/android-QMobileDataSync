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
import org.json.JSONException
import timber.log.Timber

class ActionViewModel(apiService: ApiService) : BaseViewModel() {

    init {
        Timber.v("ActionViewModel initializing...")
    }

    private var actionRepository: ActionRepository = ActionRepository(apiService)

    fun sendAction(
        actionName: String,
        actionContent: MutableMap<String, Any>,
        onResult: (isSuccess: Boolean, actionResponse: ActionResponse?) -> Unit
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
                        onResult(true, actionResponse)
                    }?: {
                        // Failed to decode
                        //   treatFailure(response, error, "ActionViewModel", ToastMessage.Type.ERROR)
                        onResult( false, null)
                    }
                }
            } else {
                treatFailure(response, error, "ActionViewModel", ToastMessage.Type.ERROR)
                onResult(false, null)
            }
        }
    }

    fun uploadImage(
        imagesToUpload: Map<String, Result<RequestBody>>,
        onImageUploaded: (parameterName: String, receivedId: String) -> Unit,
        onImageFailed: (parameterName: String, throwable: Throwable) -> Unit,
        onAllUploadFinished: () -> Unit
    ) {
        actionRepository.uploadImage(
            imagesToUpload,
            { isSuccess, parameterName, response, error ->
                if (isSuccess) {
                    when(val responseBody = response?.body()) {
                        null -> onImageFailed(parameterName, JSONException("Failed to get image upload ID from null server response"))
                        else -> {
                            val responseBodyString = responseBody.string()
                            when (val responseJson = retrieveJSONObject(responseBodyString)) {
                                null -> onImageFailed(parameterName, JSONException("Failed to get image upload ID from expected JSON response $responseBodyString"))
                                else -> {
                                    when(val id = responseJson.getSafeString("ID")) {
                                        null -> onImageFailed(parameterName, JSONException("Failed to get image upload ID from response $responseJson"))
                                        else -> onImageUploaded(parameterName, id)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    onImageFailed(parameterName, error ?: UnknownError("Failed to upload image without any known error"))
                    treatFailure(response, error, "ActionViewModel", ToastMessage.Type.ERROR)
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
