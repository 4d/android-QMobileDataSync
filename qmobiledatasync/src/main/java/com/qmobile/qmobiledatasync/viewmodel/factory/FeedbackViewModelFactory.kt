/*
 * Created by Quentin Marciset on 31/3/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.viewmodel.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.qmobile.qmobileapi.network.FeedbackApiService
import com.qmobile.qmobiledatasync.viewmodel.FeedbackViewModel

class FeedbackViewModelFactory(
    private val feedbackApiService: FeedbackApiService
) : ViewModelProvider.NewInstanceFactory() {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return FeedbackViewModel(feedbackApiService) as T
    }
}

fun getFeedbackViewModel(
    viewModelStoreOwner: ViewModelStoreOwner?,
    feedbackApiService: FeedbackApiService
): FeedbackViewModel {
    viewModelStoreOwner?.run {
        return ViewModelProvider(
            this,
            FeedbackViewModelFactory(feedbackApiService)
        )[FeedbackViewModel::class.java]
    } ?: throw IllegalStateException("Invalid Activity")
}
