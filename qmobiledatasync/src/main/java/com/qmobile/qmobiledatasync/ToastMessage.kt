/*
 * Created by Quentin Marciset on 4/2/2021.
 * 4D SAS
 * Copyright (c) 2021 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.qmobile.qmobileapi.utils.HttpCode
import okhttp3.ResponseBody
import retrofit2.Response
import timber.log.Timber
import java.io.BufferedReader
import java.io.InputStreamReader

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

    fun showError(error: Any?, info: String?) {
        Timber.e("Error for $info: $error")
        when (error) {
            is Response<*> -> {
                val errorbody  = InputStreamReader(error.errorBody()!!.byteStream())
                //errorbody.readLines()
                Timber.e("Response errorBody for $info ::${error.errorBody()!!.string()}")
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
