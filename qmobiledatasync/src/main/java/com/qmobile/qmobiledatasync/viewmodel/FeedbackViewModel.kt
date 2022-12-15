/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.viewmodel

import com.qmobile.qmobileapi.network.FeedbackApiService
import com.qmobile.qmobileapi.repository.FeedbackRepository
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import timber.log.Timber
import java.io.File

class FeedbackViewModel(feedbackApiService: FeedbackApiService) : BaseViewModel() {

    init {
        Timber.v("FeedbackViewModel initializing...")
    }

    private val feedbackRepository: FeedbackRepository = FeedbackRepository(feedbackApiService)

    fun sendCrashReport(zipFile: File, onResult: (isSuccess: Boolean) -> Unit) {
        val requestBody = zipFile.asRequestBody("multipart/form-data".toMediaType())

        feedbackRepository.sendCrashLogs(requestBody) { isSuccess, response, error ->
            if (isSuccess) {
                onResult(true)
            } else {
                response?.let { toastMessage.showMessage(it, "FeedbackViewModel") }
                error?.let { toastMessage.showMessage(it, "FeedbackViewModel") }
                onResult(false)
            }
        }
    }

    fun sendFeedback(feedback: JSONObject, onResult: (isSuccess: Boolean) -> Unit) {
        feedbackRepository.sendFeedback(feedback) { isSuccess, response, error ->
            if (isSuccess) {
                onResult(true)
            } else {
                response?.let { toastMessage.showMessage(it, "FeedbackViewModel") }
                error?.let { toastMessage.showMessage(it, "FeedbackViewModel") }
                onResult(false)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        feedbackRepository.disposable.dispose()
    }
}
