/*
 * Created by Quentin Marciset on 20/4/2021.
 * 4D SAS
 * Copyright (c) 2021 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.toast

import com.qmobile.qmobileapi.utils.HttpCode
import com.qmobile.qmobileapi.utils.getSafeString
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Response
import timber.log.Timber
import java.net.HttpURLConnection
import java.net.SocketTimeoutException

class ToastMessage {

    enum class Type {
        NEUTRAL, SUCCESS, WARNING, ERROR
    }

    data class Holder(
        val message: String,
        val type: Type
    )

    private val _message = MutableSharedFlow<Event<Holder>>(replay = 1)
    val message: SharedFlow<Event<Holder>> = _message

    fun emitMessage(message: String, type: Type) {
        _message.tryEmit(Event(Holder(message, type)))
    }

    fun showMessage(entry: Any?, info: String?, type: Type = Type.NEUTRAL) {
        Timber.d("Message for $info: $entry")
        when (entry) {
            is String -> {
                emitMessage(entry, type)
            }
            is Response<*> -> {
                handleErrorResponse(entry, info, type)
            }
            is Throwable -> {
                handleErrorThrowable(entry, type)
            }
        }
    }

    private fun handleErrorResponse(entry: Response<*>, info: String?, type: Type) {
        // val errorbody = InputStreamReader(error.errorBody()!!.byteStream())
        // errorbody.readLines()

        val code = entry.code()
        var message = ""

        // Trying to check if there is a missing On Mobile App Authentication method
        entry.errorBody()?.let {
            // must be copy into a variable and used as many times as required
            val errorBody = it.string()
            Timber.e("Response errorBody for $info ::$errorBody")
            if (code == HttpURLConnection.HTTP_UNAUTHORIZED) {
                message = try {
                    JSONObject(errorBody).getSafeString("statusText") ?: ""
                } catch (e: JSONException) {
                    ""
                }
            }
        }

        if (message.isNotEmpty()) {
            emitMessage(message, Type.WARNING)
        } else {
            HttpCode.reason(code)?.let { reason ->
                message = "$reason ($code)"
            } ?: kotlin.run {
                message = "${HttpCode.message(code)} ($code)"
            }
            emitMessage(message, type)
        }
    }

    private fun handleErrorThrowable(entry: Throwable, type: Type) {
        if (entry is SocketTimeoutException) {
            emitMessage(HttpCode.message(HttpCode.requestTimeout), type)
        } else {
            entry.localizedMessage?.let { message ->
                emitMessage(message, type)
            }
        }
    }
}
