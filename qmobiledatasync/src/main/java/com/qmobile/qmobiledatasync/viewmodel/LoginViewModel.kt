/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.qmobile.qmobileapi.auth.AuthenticationStateEnum
import com.qmobile.qmobileapi.model.auth.AuthResponse
import com.qmobile.qmobileapi.network.LoginApiService
import com.qmobile.qmobileapi.repository.AuthRepository
import com.qmobile.qmobileapi.utils.extractJSON
import com.qmobile.qmobileapi.utils.parseJsonToType
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.toast.MessageType
import com.qmobile.qmobiledatasync.toast.ToastMessage
import timber.log.Timber

class LoginViewModel(application: Application, loginApiService: LoginApiService) :
    AndroidViewModel(application) {

    init {
        Timber.v("LoginViewModel initializing...")
    }

    private var authRepository: AuthRepository = AuthRepository(loginApiService)

    /**
     * LiveData
     */

    private val _dataLoading = MutableLiveData<Boolean>().apply { value = false }
    val dataLoading: LiveData<Boolean> = _dataLoading

    private val _emailValid = MutableLiveData<Boolean>().apply { value = false }
    val emailValid: LiveData<Boolean> = _emailValid

    var statusMessage = ""

    private val _authenticationState: MutableLiveData<AuthenticationStateEnum> by lazy {
        MutableLiveData<AuthenticationStateEnum>(AuthenticationStateEnum.UNAUTHENTICATED)
    }
    val authenticationState: LiveData<AuthenticationStateEnum> = _authenticationState

    val toastMessage: ToastMessage = ToastMessage()

    /**
     * Authenticates
     */
    fun login(email: String = "", password: String = "", onResult: (success: Boolean) -> Unit) {
        _dataLoading.value = true
        // Builds the request body for $authenticate request
        val authRequestBody = BaseApp.sharedPreferencesHolder.buildAuthRequestBody(email, password)
        // Provides shouldRetryOnError to know if we should redirect the user to login page or
        // if we should retry silently
        val shouldRetryOnError = BaseApp.runtimeDataHolder.guestLogin
        authRepository.authenticate(
            authRequestBody,
            shouldRetryOnError
        ) { isSuccess, response, error ->
            _dataLoading.value = false
            if (isSuccess) {
                response?.body()?.let { responseBody ->

                    retrieveAuthResponse(responseBody.string())?.let { authResponse ->
//                        statusMessage.postValue(authResponse.statusText ?: "")
                        statusMessage = authResponse.statusText ?: ""
                        // Fill SharedPreferences with response details
                        if (BaseApp.sharedPreferencesHolder.handleLoginInfo(authResponse)) {
                            _authenticationState.postValue(AuthenticationStateEnum.AUTHENTICATED)
                            onResult(true)
                            return@authenticate
                        } else {
                            response.let {
                                toastMessage.showMessage(
                                    statusMessage,
                                    "LoginViewModel",
                                    MessageType.WARNING
                                )
                            }
                        }
                    }
                }
                onResult(false)
                _authenticationState.postValue(AuthenticationStateEnum.INVALID_AUTHENTICATION)
            } else {
                response?.let { toastMessage.showMessage(it, "LoginViewModel", MessageType.ERROR) }
                error?.let { toastMessage.showMessage(it, "LoginViewModel", MessageType.ERROR) }
                onResult(false)
                _authenticationState.postValue(AuthenticationStateEnum.INVALID_AUTHENTICATION)
            }
        }
    }

    /**
     * Decode auth response
     */
    private fun retrieveAuthResponse(jsonString: String): AuthResponse? {
        jsonString.extractJSON()?.let {
            return try {
                Gson().parseJsonToType(it)
            } catch (e: JsonSyntaxException) {
                Timber.w("Failed to decode auth response ${e.localizedMessage}: $jsonString")
                null
            }
        }
        return null
    }

    /**
     * Logs out
     */
    fun disconnectUser(onResult: (success: Boolean) -> Unit) {
        authRepository.logout { isSuccess, response, error ->
            _dataLoading.value = false
            _authenticationState.postValue(AuthenticationStateEnum.LOGOUT)
            BaseApp.sharedPreferencesHolder.sessionToken = ""
            if (isSuccess) {
                Timber.d("[ Logout request successful ]")
            } else {
                response?.let { toastMessage.showMessage(it, "LoginViewModel") }
                error?.let { toastMessage.showMessage(it, "LoginViewModel") }
            }
            onResult(isSuccess)
        }
    }

    fun refreshAuthRepository(loginApiService: LoginApiService) {
        authRepository = AuthRepository(loginApiService)
    }

    override fun onCleared() {
        super.onCleared()
        authRepository.disposable.dispose()
    }

    fun setEmailValidState(isValid: Boolean) {
        _emailValid.postValue(isValid)
    }

    fun setAuthenticationState(value: AuthenticationStateEnum) {
        _authenticationState.postValue(value)
    }
}
