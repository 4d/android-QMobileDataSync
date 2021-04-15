/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.viewmodel

import android.app.Application
import android.graphics.Color
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.qmobile.qmobileapi.auth.AuthInfoHelper
import com.qmobile.qmobileapi.auth.AuthenticationStateEnum
import com.qmobile.qmobileapi.model.auth.AuthResponse
import com.qmobile.qmobileapi.network.LoginApiService
import com.qmobile.qmobileapi.repository.AuthRepository
import com.qmobile.qmobileapi.utils.extractJSON
import com.qmobile.qmobileapi.utils.parseJsonToType
import com.qmobile.qmobiledatasync.ToastMessage
import timber.log.Timber

class LoginViewModel(application: Application, loginApiService: LoginApiService) :
    AndroidViewModel(application) {

    init {
        Timber.v("LoginViewModel initializing...")
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
                response?.body()?.let { responseBody ->

                    retrieveAuthResponse(responseBody.string())?.let { authResponse ->
                        showStatusText(authResponse)
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
                response?.let { toastMessage.showError(it, "LoginViewModel") }
                error?.let { toastMessage.showError(it, "LoginViewModel") }
                onResult(false)
                authenticationState.postValue(AuthenticationStateEnum.INVALID_AUTHENTICATION)
            }
        }
    }

    private fun showStatusText(authResponse: AuthResponse){
        Timber.d("Entered in to showStatusText")
        authResponse.statusText?.let {
            val message = it
            Timber.d("Entered in to showStatusText $message")
            val toast =
                Toast.makeText(getApplication(), message, Toast.LENGTH_SHORT)
            toast.view.apply {
                val toastBackGroundColor =
                    if (authResponse.success) Color.GREEN else Color.RED
                this.setBackgroundColor(toastBackGroundColor)
            }
            toast.show()
        }
    }

    /**
     * Decode auth response
     */
    private fun retrieveAuthResponse(jsonString: String): AuthResponse? {
        jsonString.extractJSON()?.let {
            return try {
                Gson().parseJsonToType<AuthResponse>(it)
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
            dataLoading.value = false
            authenticationState.postValue(AuthenticationStateEnum.LOGOUT)
            authInfoHelper.sessionToken = ""
            if (isSuccess) {
                Timber.d("[ Logout request successful ]")
            } else {
                response?.let { toastMessage.showError(it, "LoginViewModel") }
                error?.let { toastMessage.showError(it, "LoginViewModel") }
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
