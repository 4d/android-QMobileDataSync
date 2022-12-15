/*
 * Created by qmarciset on 22/11/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.viewmodel

import androidx.lifecycle.AndroidViewModel
import com.qmobile.qmobileapi.utils.RequestErrorHelper.isUnauthorized
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.toast.ToastMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.ResponseBody
import retrofit2.Response

abstract class BaseViewModel : AndroidViewModel(BaseApp.instance) {

    val toastMessage: ToastMessage = ToastMessage()

    private val _isUnauthorized = MutableStateFlow(false)
    val isUnauthorized: StateFlow<Boolean> = _isUnauthorized

    fun setIsUnauthorizedState(isUnauthorized: Boolean) {
        _isUnauthorized.value = isUnauthorized
    }

    internal fun treatFailure(
        response: Response<ResponseBody>?,
        error: Any?,
        info: String?,
        type: ToastMessage.Type = ToastMessage.Type.NEUTRAL
    ) {
        if (response.isUnauthorized()) {
            setIsUnauthorizedState(true)
        } else {
            setIsUnauthorizedState(false)
            response?.let { toastMessage.showMessage(it, info, type) }
            error?.let { toastMessage.showMessage(it, info, type) }
        }
    }
}
