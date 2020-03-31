/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmarciset.androidmobiledatasync.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.qmarciset.androidmobileapi.auth.AuthInfoHelper
import com.qmarciset.androidmobileapi.auth.AuthenticationState
import com.qmarciset.androidmobileapi.model.auth.AuthResponse
import com.qmarciset.androidmobileapi.network.LoginApiService
import com.qmarciset.androidmobileapi.repository.AuthRepository
import com.qmarciset.androidmobileapi.utils.RequestErrorHelper
import com.qmarciset.androidmobileapi.utils.parseJsonToType
import timber.log.Timber

class LoginViewModel(application: Application, loginApiService: LoginApiService) :
    AndroidViewModel(application) {

    init {
        Timber.i("LoginViewModel initializing...")
    }

    private val authRepository: AuthRepository = AuthRepository(loginApiService)
    val authInfoHelper = AuthInfoHelper.getInstance(application.applicationContext)

    /**
     * LiveData
     */

    val dataLoading = MutableLiveData<Boolean>().apply { value = false }

    val emailValid = MutableLiveData<Boolean>().apply { value = false }

    val authenticationState: MutableLiveData<AuthenticationState> by lazy {
        MutableLiveData<AuthenticationState>(AuthenticationState.UNAUTHENTICATED)
    }

    /**
     * Authenticates
     */
    fun login(email: String = "", password: String = "") {
        dataLoading.value = true
        // Builds the request body for $authenticate request
        val authRequest = authInfoHelper.buildAuthRequestBody(email, password)
        // Provides shouldRetryOnError to know if we should redirect the user to login page or
        // if we should retry silently
        val shouldRetryOnError = authInfoHelper.guestLogin
        authRepository.authenticate(authRequest, shouldRetryOnError) { isSuccess, response, error ->
            dataLoading.value = false
            if (isSuccess) {
                response?.let {
                    val responseBody = response.body()
                    val json = responseBody?.string()
                    val authResponse: AuthResponse? = Gson().parseJsonToType(json)
                    authResponse?.let {
                        // Fill SharedPreferences with response details
                        if (authInfoHelper.handleLoginInfo(authResponse)) {
                            authenticationState.postValue(AuthenticationState.AUTHENTICATED)
                            return@authenticate
                        }
                    }
                }
                authenticationState.postValue(AuthenticationState.INVALID_AUTHENTICATION)
            } else {
                RequestErrorHelper.handleError(error)
                authenticationState.postValue(AuthenticationState.INVALID_AUTHENTICATION)
            }
        }
    }

    /**
     * Logs out
     */
    fun disconnectUser() {
        authRepository.logout { isSuccess, _, error ->
            dataLoading.value = false
            authenticationState.postValue(AuthenticationState.LOGOUT)
            authInfoHelper.sessionToken = ""
            if (isSuccess) {
                Timber.d("[ Logout request successful ]")
            } else {
                RequestErrorHelper.handleError(error)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        authRepository.disposable.dispose()
    }
}
