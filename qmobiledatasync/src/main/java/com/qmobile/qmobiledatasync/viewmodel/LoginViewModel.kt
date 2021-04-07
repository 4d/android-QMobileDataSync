/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.qmobile.qmobileapi.auth.AuthInfoHelper
import com.qmobile.qmobileapi.auth.AuthenticationStateEnum
import com.qmobile.qmobileapi.model.auth.AuthResponse
import com.qmobile.qmobileapi.network.LoginApiService
import com.qmobile.qmobileapi.repository.AuthRepository
import com.qmobile.qmobileapi.utils.parseJsonToType
import com.qmobile.qmobiledatasync.ToastMessage
import timber.log.Timber

class LoginViewModel(application: Application, loginApiService: LoginApiService) :
    AndroidViewModel(application) {

    init {
        Timber.i("LoginViewModel initializing...")
    }

    private var authRepository: AuthRepository = AuthRepository(loginApiService)
    val authInfoHelper = AuthInfoHelper.getInstance(application.applicationContext)

    /**
     * LiveData
     */

    val dataLoading = MutableLiveData<Boolean>().apply { value = false }

    val emailValid = MutableLiveData<Boolean>().apply { value = false }

    val authenticationState: MutableLiveData<AuthenticationStateEnum> by lazy {
        MutableLiveData<AuthenticationStateEnum>(AuthenticationStateEnum.UNAUTHENTICATED)
    }

    val toastMessage: ToastMessage = ToastMessage()

    /**
     * Authenticates
     */
    fun login(email: String = "", password: String = "", onResult: (success: Boolean) -> Unit) {
        dataLoading.value = true
        // Builds the request body for $authenticate request
        val authRequestBody = authInfoHelper.buildAuthRequestBody(email, password)
        // Provides shouldRetryOnError to know if we should redirect the user to login page or
        // if we should retry silently
        val shouldRetryOnError = authInfoHelper.guestLogin
        authRepository.authenticate(
            authRequestBody,
            shouldRetryOnError
        ) { isSuccess, response, error ->
            dataLoading.value = false
            if (isSuccess) {
                response?.let {
                    val responseBody = response.body()
                    val json = responseBody?.string()
                    val authResponse: AuthResponse? = Gson().parseJsonToType(json)
                    authResponse?.let {
                        // Fill SharedPreferences with response details
                        if (authInfoHelper.handleLoginInfo(authResponse)) {
                            authenticationState.postValue(AuthenticationStateEnum.AUTHENTICATED)
                            onResult(true)
                            return@authenticate
                        }
                    }
                }
                onResult(false)
                authenticationState.postValue(AuthenticationStateEnum.INVALID_AUTHENTICATION)
            } else {
                response?.let { toastMessage.showError(it) }
                error?.let { toastMessage.showError(it) }
                onResult(false)
                authenticationState.postValue(AuthenticationStateEnum.INVALID_AUTHENTICATION)
            }
        }
    }

    /**
     * Logs out
     */
    fun disconnectUser(onResult: (success: Boolean) -> Unit) {
        authRepository.logout { isSuccess, response, error ->
            dataLoading.value = false
            authenticationState.postValue(AuthenticationStateEnum.LOGOUT)
            authInfoHelper.sessionToken = ""
            if (isSuccess) {
                Timber.d("[ Logout request successful ]")
            } else {
                response?.let { toastMessage.showError(it) }
                error?.let { toastMessage.showError(it) }
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
}
