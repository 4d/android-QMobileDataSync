/*
 * Created by Quentin Marciset on 31/3/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.viewmodel.factory

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.qmobile.qmobiledatastore.dao.ActionTaskDao
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.viewmodel.TaskViewModel

class TaskViewModelFactory(
    private val application: Application,
) : ViewModelProvider.NewInstanceFactory() {
    private lateinit var actionTaskDao: ActionTaskDao

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return TaskViewModel(
            application
        ) as T
    }
}

fun getTaskViewModel(
    viewModelStoreOwner: ViewModelStoreOwner?
): TaskViewModel {
    // We need this ViewModel to know when MainActivity has performed its $authenticate so that
    // we don't trigger the initial sync if we are not authenticated yet
    viewModelStoreOwner?.run {
        return ViewModelProvider(
            this,
            TaskViewModelFactory(BaseApp.instance)
        )[TaskViewModel::class.java]
    } ?: throw IllegalStateException("Invalid Activity")
}
