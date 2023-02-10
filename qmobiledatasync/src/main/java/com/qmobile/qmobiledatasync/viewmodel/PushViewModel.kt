/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.viewmodel

import com.qmobile.qmobileapi.network.ApiService
import com.qmobile.qmobileapi.repository.PushRepository
import com.qmobile.qmobileapi.utils.getSafeBoolean
import com.qmobile.qmobileapi.utils.retrieveJSONObject
import com.qmobile.qmobiledatasync.toast.ToastMessage
import timber.log.Timber

class PushViewModel(apiService: ApiService) : BaseViewModel() {

    init {
        Timber.v("PushViewModel initializing...")
    }

    private var pushRepository: PushRepository = PushRepository(apiService)

    fun sendToken(
        token: String,
        onResult: (isSuccess: Boolean) -> Unit
    ) {
        val userInfo = mutableMapOf<String, Any>().apply {
            put("device", mapOf<String, Any>("token" to token))
        }

        pushRepository.sendUserInfo(
            userInfo
        ) { isSuccess, response, error ->
            if (isSuccess) {
                response?.body()?.let { responseBody ->
                    retrieveJSONObject(responseBody.string())?.let { responseJson ->
                        if (responseJson.getSafeBoolean("ok") == true) {
                            onResult(true)
                            return@sendUserInfo
                        }
                    }
                    onResult(false)
                }
            } else {
                treatFailure(response, error, "PushViewModel", ToastMessage.Type.ERROR)
                onResult(false)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        pushRepository.disposable.dispose()
    }

    fun refreshPushRepository(apiService: ApiService) {
        pushRepository = PushRepository(apiService)
    }
}
