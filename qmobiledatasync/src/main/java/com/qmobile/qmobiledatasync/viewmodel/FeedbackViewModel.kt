/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.viewmodel

import com.qmobile.qmobileapi.network.FeedbackApiService
import com.qmobile.qmobileapi.repository.FeedbackRepository
import com.qmobile.qmobileapi.utils.APP_JSON
import com.qmobile.qmobileapi.utils.UTF8_CHARSET
import com.qmobile.qmobiledatasync.toast.ToastMessage
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import timber.log.Timber
import java.io.File

class FeedbackViewModel(feedbackApiService: FeedbackApiService) : BaseViewModel() {

    init {
        Timber.v("FeedbackViewModel initializing...")
    }

    private val feedbackRepository: FeedbackRepository = FeedbackRepository(feedbackApiService)

    fun isFeedbackServerConnectionOk(
        toastError: Boolean = true,
        onResult: (success: Boolean) -> Unit
    ) {
        feedbackRepository.checkAccessibility { isSuccess, response, error ->
            if (isSuccess) {
                Timber.d("Server ping successful")
                onResult(true)
            } else {
                Timber.d("Server ping unsuccessful")
                response?.let {
                    onResult(true)
                    return@checkAccessibility
                }
                if (toastError) {
                    error?.let { toastMessage.showMessage(it, "FeedbackViewModel", ToastMessage.Type.ERROR) }
                }
                onResult(false)
            }
        }
    }

    fun sendCrashReport(jsonBody: JSONObject, zipFile: File, onResult: (isSuccess: Boolean) -> Unit) {
        val body = jsonBody.toString().toRequestBody("$APP_JSON; $UTF8_CHARSET".toMediaTypeOrNull())
        val requestFile = zipFile.asRequestBody("multipart/form-data".toMediaType())
        val filePart: MultipartBody.Part = MultipartBody.Part.createFormData("file", zipFile.name, requestFile)

        feedbackRepository.sendCrashLogs(body, filePart) { isSuccess, response, error ->
            if (isSuccess) {
                onResult(true)
            } else {
                treatFailure(response, error, "FeedbackViewModel")
                onResult(false)
            }
        }
    }

    fun sendFeedback(feedback: JSONObject, onResult: (isSuccess: Boolean) -> Unit) {
        feedbackRepository.sendFeedback(feedback) { isSuccess, response, error ->
            if (isSuccess) {
                onResult(true)
            } else {
                treatFailure(response, error, "FeedbackViewModel")
                onResult(false)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        feedbackRepository.disposable.dispose()
    }
}
