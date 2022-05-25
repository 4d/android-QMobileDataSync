/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.qmobile.qmobiledatastore.dao.ActionTask
import com.qmobile.qmobiledatastore.repository.PendingTaskRepository
import com.qmobile.qmobiledatasync.app.BaseApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TaskViewModel(application: Application) :
    AndroidViewModel(application) {

    private val _pendingTasks = MutableLiveData<List<ActionTask>>().apply {
        dao.getAllPending()
    }
    val pendingTasks: LiveData<List<ActionTask>> = _pendingTasks

    private val _allTasks = MutableLiveData<List<ActionTask>>().apply {
        dao.getAll()
    }
    val allTasks: LiveData<List<ActionTask>> = _allTasks

    fun getTask(id: Long): LiveData<ActionTask> = dao.getOne(id)

    private val _dataLoading = MutableStateFlow(false)
    val dataLoading: StateFlow<Boolean> = _dataLoading

    private val dao = BaseApp.daoProvider.getActionTaskDao()
    private val pendingTaskRepository = PendingTaskRepository(dao)

    fun setLoading(isLoading: Boolean) {
        _dataLoading.value = isLoading
    }

    fun deleteOne(id: Long) = viewModelScope.launch {
        pendingTaskRepository.deleteOne(id)
    }

    fun deleteAll() = viewModelScope.launch {
        pendingTaskRepository.deleteAll()
    }

    fun insert(actionTask: ActionTask) = viewModelScope.launch {
        pendingTaskRepository.insert(actionTask)
    }
}
