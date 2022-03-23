/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber

class TaskViewModel(application: Application) :
    AndroidViewModel(application) {

    init {
        Timber.v("LoginViewModel initializing...")
    }

    /**
     * LiveData
     */

    private val _dataLoading = MutableStateFlow(false)
    val dataLoading: StateFlow<Boolean> = _dataLoading

    fun setLoading(isLoading: Boolean) {
        _dataLoading.value = isLoading
    }
}
