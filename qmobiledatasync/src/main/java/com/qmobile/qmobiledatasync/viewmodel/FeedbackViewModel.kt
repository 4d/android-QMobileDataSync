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
import com.qmobile.qmobileapi.utils.getSafeBoolean
import com.qmobile.qmobileapi.utils.getSafeString
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.log.LogFileHelper
import com.qmobile.qmobiledatasync.toast.ToastMessage
import com.qmobile.qmobiledatasync.utils.FeedbackType
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

    fun sendCrash(zipFile: File?, onResult: (isSuccess: Boolean, ticket: String?) -> Unit) {
        val jsonBody = buildRequestJson(type = FeedbackType.REPORT_PREVIOUS_CRASH, hasFile = zipFile != null)
        sendFeedbackAndLogs(jsonBody, zipFile, onResult)
    }

    fun sendCurrentLogs(zipFile: File?, onResult: (isSuccess: Boolean) -> Unit) {
        val jsonBody = buildRequestJson(type = FeedbackType.SHOW_CURRENT_LOG, hasFile = zipFile != null)
        sendFeedbackAndLogs(jsonBody, zipFile) { isSuccess, _ ->
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
        val jsonBody = buildRequestJson(type, email, feedbackContent, zipFile != null)
        sendFeedbackAndLogs(jsonBody, zipFile) { isSuccess, _ ->
            onResult(isSuccess)
        }
    }

    private fun sendFeedbackAndLogs(
        jsonBody: JSONObject,
        zipFile: File?,
        onResult: (isSuccess: Boolean, ticket: String?) -> Unit
    ) {
        if (zipFile == null) {
            Timber.e("Cannot send crash log : zip file is null !")
            return
        }
        val body = jsonBody.toString().toRequestBody("$APP_JSON; $UTF8_CHARSET".toMediaTypeOrNull())
        val requestFile = zipFile.asRequestBody("multipart/form-data".toMediaType())
        val filePart = MultipartBody.Part.createFormData("file", zipFile.name, requestFile)

        feedbackRepository.sendFeedbackAndLogs(body, filePart) { isSuccess, response, error ->
            if (isSuccess) {
                onResult(true, response?.body()?.string())
            } else {
                treatFailure(response, error, "FeedbackViewModel")
                onResult(false, null)
            }
        }
    }

    private fun buildRequestJson(
        type: FeedbackType,
        email: String? = null,
        feedbackContent: String? = null,
        hasFile: Boolean
    ): JSONObject {
        return JSONObject().apply {
            /*
                build ?
                component ?
                ide ?

                "Build": "100058",
                "Component": "100058",
                "IDE": "1980",
            */

            email?.let { put("email", it) }
            feedbackContent?.let { put("summary", it) }
            put("type", type.key)
            put("fileName", if (hasFile) LogFileHelper.zipFileName else "")
            put("SendDate", LogFileHelper.getCurrentDateTimeLogFormat())
            put("isCrash", if (type == FeedbackType.REPORT_PREVIOUS_CRASH) "1" else "0")

            BaseApp.sharedPreferencesHolder.appInfo.getSafeString("id")?.let { put("CFBundleIdentifier", it) }
            BaseApp.sharedPreferencesHolder.appInfo.getSafeString("name")?.let { put("CFBundleName", it) }
            BaseApp.sharedPreferencesHolder.appInfo.getSafeString("version")
                ?.let { put("CFBundleShortVersionString", it) }
            BaseApp.sharedPreferencesHolder.device.getSafeString("version")?.let { put("DTPlatformVersion", it) }
            BaseApp.sharedPreferencesHolder.device.getSafeString("id")?.let { put("uuid", it) }
            BaseApp.sharedPreferencesHolder.device.getSafeString("description")?.let { put("device.description", it) }
            BaseApp.sharedPreferencesHolder.device.getSafeBoolean("simulator")?.let { put("device.simulator", it) }
            BaseApp.sharedPreferencesHolder.team.getSafeString("id")?.let { put("AppIdentifierPrefix", it) }

            put("sdk", BaseApp.runtimeDataHolder.sdkVersion)
        }
    }

    override fun onCleared() {
        super.onCleared()
        feedbackRepository.disposable.dispose()
    }
}
