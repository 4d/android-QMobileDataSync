/*
 * Created by qmarciset on 8/12/2021.
 * 4D SAS
 * Copyright (c) 2021 qmarciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.utils

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope

/**
 * Configure CoroutineScope injection for production and testing.
 *
 * @receiver ViewModel provides viewModelScope for production
 * @param coroutineScope null for production, injects TestCoroutineScope for unit tests
 * @return CoroutineScope to launch coroutines on
 */
fun AndroidViewModel.getViewModelScope(coroutineScope: CoroutineScope? = null) =
    coroutineScope ?: this.viewModelScope
