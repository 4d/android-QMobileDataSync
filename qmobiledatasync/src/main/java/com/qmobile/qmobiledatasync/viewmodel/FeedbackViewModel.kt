/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.viewmodel

import com.qmobile.qmobileapi.network.FeedbackApiService
import com.qmobile.qmobileapi.repository.FeedbackRepository
import com.qmobile.qmobileapi.utils.getSafeBoolean
import com.qmobile.qmobileapi.utils.getSafeString
import com.qmobile.qmobileapi.utils.retrieveJSONObject
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.log.LogFileHelper
import com.qmobile.qmobiledatasync.toast.ToastMessage
import com.qmobile.qmobiledatasync.utils.FeedbackType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
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

    fun sendCrash(zipFile: File?, onResult: (isSuccess: Boolean, ticket: String?) -> Unit) {
        val map = buildRequest(type = FeedbackType.REPORT_PREVIOUS_CRASH, hasFile = zipFile != null)
        sendFeedbackAndLogs(map, zipFile, onResult)
    }

    fun sendCurrentLogs(zipFile: File?, onResult: (isSuccess: Boolean) -> Unit) {
        val map = buildRequest(type = FeedbackType.SHOW_CURRENT_LOG, hasFile = zipFile != null)
        sendFeedbackAndLogs(map, zipFile) { isSuccess, _ ->
            onResult(isSuccess)
        }
    }

    fun sendFeedback(
        type: FeedbackType,
        email: String,
        feedbackContent: String?,
        zipFile: File?,
        onResult: (isSuccess: Boolean) -> Unit
    ) {
        val map = buildRequest(type, email, feedbackContent, zipFile != null)
        sendFeedbackAndLogs(map, zipFile) { isSuccess, _ ->
            onResult(isSuccess)
        }
    }

    private fun sendFeedbackAndLogs(
        map: MutableMap<String, RequestBody>,
        zipFile: File?,
        onResult: (isSuccess: Boolean, ticket: String?) -> Unit
    ) {
        var filePart: MultipartBody.Part? = null
        zipFile?.let {
            val requestFile = it.asRequestBody("multipart/form-data".toMediaType())
            filePart = MultipartBody.Part.createFormData("file", it.name, requestFile)
        }

        feedbackRepository.send(map, filePart) { isSuccess, response, error ->
            if (isSuccess) {
                response?.body()?.let { responseBody ->
                    retrieveJSONObject(responseBody.string())?.let { responseJson ->
                        if (responseJson.getSafeBoolean("ok") == true) {
                            onResult(true, responseJson.getSafeString("ticket"))
                            return@send
                        }
                    }
                }
                onResult(false, null)
            } else {
                treatFailure(response, error, "FeedbackViewModel")
                onResult(false, null)
            }
        }
    }

    private fun createPartFromString(stringData: String): RequestBody {
        return stringData.toRequestBody("text/plain".toMediaTypeOrNull())
    }

    private fun buildRequest(
        type: FeedbackType,
        email: String? = null,
        feedbackContent: String? = null,
        hasFile: Boolean
    ): MutableMap<String, RequestBody> {
        return mutableMapOf<String, RequestBody>().apply {
            email?.let { put("email", createPartFromString(it)) }
            feedbackContent?.let { put("summary", createPartFromString(it)) }
            put("type", createPartFromString(type.key))
            put("fileName", createPartFromString(if (hasFile) LogFileHelper.zipFileName else ""))
            put("SendDate", createPartFromString(LogFileHelper.getCurrentDateTimeLogFormat()))
            put("isCrash", createPartFromString(if (type == FeedbackType.REPORT_PREVIOUS_CRASH) "1" else "0"))

            BaseApp.sharedPreferencesHolder.appInfo.getSafeString("id")
                ?.let { put("CFBundleIdentifier", createPartFromString(it)) }
            BaseApp.sharedPreferencesHolder.appInfo.getSafeString("name")
                ?.let { put("CFBundleName", createPartFromString(it)) }
            BaseApp.sharedPreferencesHolder.appInfo.getSafeString("version")
                ?.let { put("CFBundleShortVersionString", createPartFromString(it)) }
            BaseApp.sharedPreferencesHolder.device.getSafeString("version")
                ?.let { put("DTPlatformVersion", createPartFromString(it)) }
            BaseApp.sharedPreferencesHolder.device.getSafeString("id")?.let { put("uuid", createPartFromString(it)) }
            BaseApp.sharedPreferencesHolder.device.getSafeString("description")
                ?.let { put("device.description", createPartFromString(it)) }
            BaseApp.sharedPreferencesHolder.device.getSafeBoolean("simulator")
                ?.let { put("device.simulator", createPartFromString(it.toString())) }
            BaseApp.sharedPreferencesHolder.team.getSafeString("id")
                ?.let { put("AppIdentifierPrefix", createPartFromString(it)) }

            put("sdk", createPartFromString(BaseApp.runtimeDataHolder.sdkVersion))
        }
    }

    override fun onCleared() {
        super.onCleared()
        feedbackRepository.disposable.dispose()
    }
}
