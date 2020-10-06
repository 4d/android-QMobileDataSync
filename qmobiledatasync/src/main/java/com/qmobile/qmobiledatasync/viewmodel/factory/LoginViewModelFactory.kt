/*
 * Created by Quentin Marciset on 31/3/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.viewmodel.factory

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.qmobile.qmobileapi.network.LoginApiService
import com.qmobile.qmobiledatasync.viewmodel.LoginViewModel

class LoginViewModelFactory(
    private val application: Application,
    private val apiService: LoginApiService
) : ViewModelProvider.NewInstanceFactory() {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return LoginViewModel(
            application,
            apiService
        ) as T
    }
}
