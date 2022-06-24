/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.qmobile.qmobileapi.auth.AuthenticationState
import com.qmobile.qmobileapi.model.auth.AuthResponse
import com.qmobile.qmobileapi.network.LoginApiService
import com.qmobile.qmobileapi.repository.AuthRepository
import com.qmobile.qmobileapi.utils.retrieveResponseObject
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.toast.ToastMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

    private val _dataLoading = MutableStateFlow(false)
    val dataLoading: StateFlow<Boolean> = _dataLoading

    private val _emailValid = MutableStateFlow(false)
    val emailValid: StateFlow<Boolean> = _emailValid

    var statusMessage = ""

    private val _authenticationState = MutableStateFlow(AuthenticationState.UNAUTHENTICATED)
    val authenticationState: StateFlow<AuthenticationState> = _authenticationState

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

                    retrieveResponseObject<AuthResponse>(
                        BaseApp.mapper,
                        responseBody.string()
                    )?.let { authResponse ->

                        statusMessage = authResponse.statusText ?: ""
                        // Fill SharedPreferences with response details
                        if (BaseApp.sharedPreferencesHolder.handleLoginInfo(authResponse)) {
                            BaseApp.sharedPreferencesHolder.storeCookies(response)
                            _authenticationState.value = AuthenticationState.AUTHENTICATED
                            onResult(true)
                            return@authenticate
                        } else {
                            response.let {
                                toastMessage.showMessage(
                                    statusMessage,
                                    "LoginViewModel",
                                    ToastMessage.Type.WARNING
                                )
                            }
                        }
                    }
                }
                onResult(false)
                _authenticationState.value = AuthenticationState.INVALID_AUTHENTICATION
            } else {
                response?.let { toastMessage.showMessage(it, "LoginViewModel", ToastMessage.Type.ERROR) }
                error?.let { toastMessage.showMessage(it, "LoginViewModel", ToastMessage.Type.ERROR) }
                onResult(false)
                _authenticationState.value = AuthenticationState.INVALID_AUTHENTICATION
            }
        }
    }

    /**
     * Logs out
     */
    fun disconnectUser(onResult: (success: Boolean) -> Unit) {
        authRepository.logout { isSuccess, response, error ->
            _dataLoading.value = false
            _authenticationState.value = AuthenticationState.LOGOUT
            BaseApp.sharedPreferencesHolder.sessionToken = ""
            BaseApp.sharedPreferencesHolder.clearCookies()
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
        _emailValid.value = isValid
    }

    fun setAuthenticationState(value: AuthenticationState) {
        _authenticationState.value = value
    }
}
