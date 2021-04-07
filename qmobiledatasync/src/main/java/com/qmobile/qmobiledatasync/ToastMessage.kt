/*
 * Created by Quentin Marciset on 4/2/2021.
 * 4D SAS
 * Copyright (c) 2021 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.qmobile.qmobileapi.utils.HttpCode
import retrofit2.Response
import timber.log.Timber

class ToastMessage {

    // Mutable/LiveData of String resource reference Event
    private val _message = MutableLiveData<Event<String>>()
    val message: LiveData<Event<String>>
        get() = _message

    // Post in background thread
    fun postMessage(message: String) {
        _message.postValue(Event(message))
    }

    // Post in main thread
    fun setMessage(message: String) {
        _message.value = Event(message)
    }

    fun showError(error: Any?) {
        Timber.e("Error: $error")
        when (error) {
            is Response<*> -> {
                var message = ""
                val code = error.code()
                HttpCode.reason(code)?.let { reason ->
                    message = "$reason ($code)"
                } ?: kotlin.run {
                    message = "${HttpCode.message(code)} ($code)"
                }
                setMessage(message)
            }
            is Throwable -> {
                error.localizedMessage?.let { message ->
                    setMessage(message)
                }
            }
        }
    }
}
