/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.qmobile.qmobiledatastore.dao.ActionTask
import com.qmobile.qmobiledatastore.dao.ActionTaskDao
import com.qmobile.qmobiledatasync.app.BaseApp
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class TaskViewModel(application: Application) :
    AndroidViewModel(application) {

    var disposable: CompositeDisposable = CompositeDisposable()

    /**
     * LiveData
     */

    private val _dataLoading = MutableStateFlow(false)
    val dataLoading: StateFlow<Boolean> = _dataLoading

    fun setLoading(isLoading: Boolean) {
        _dataLoading.value = isLoading
    }

    fun sendPendingTasks(
        tasksToSend: List<ActionTask>,
        uploadImagesCallBack: (ActionTask) -> Unit,
        sendTaskCallBack: (ActionTask) -> Unit
    ) {
        disposable.add(
            Observable.fromIterable(tasksToSend)
                .subscribeOn(Schedulers.io())
                .subscribe { task ->
                    if (task.actionInfo.imagesToUpload.isNullOrEmpty()) {
                        sendTaskCallBack(task)
                    } else {
                        uploadImagesCallBack(task)
                    }
                }
        )
    }

    override fun onCleared() {
        super.onCleared()
        disposable.dispose()
    }
}
