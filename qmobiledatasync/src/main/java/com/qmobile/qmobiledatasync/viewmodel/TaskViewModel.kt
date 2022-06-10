/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.qmobile.qmobiledatastore.dao.ActionTask
import com.qmobile.qmobiledatastore.repository.PendingTaskRepository
import com.qmobile.qmobiledatasync.app.BaseApp
import kotlinx.coroutines.launch

class TaskViewModel(application: Application) :
    AndroidViewModel(application) {

    private val dao = BaseApp.daoProvider.getActionTaskDao()
    private val pendingTaskRepository = PendingTaskRepository(dao)

    val pendingTasks: LiveData<List<ActionTask>> = pendingTaskRepository.getAllPending()
    val allTasks: LiveData<List<ActionTask>> = pendingTaskRepository.getAll()

    fun getTask(id: String): LiveData<ActionTask> = pendingTaskRepository.getOne(id)

    fun deleteOne(id: String) = viewModelScope.launch {
        pendingTaskRepository.deleteOne(id)
    }

    fun deleteAll() = viewModelScope.launch {
        pendingTaskRepository.deleteAll()
    }

    fun insert(actionTask: ActionTask) = viewModelScope.launch {
        pendingTaskRepository.insert(actionTask)
    }

    // As we need to show only the 10 latest History Items, so we need this function to clean oldest items
    fun deleteList(idList: List<String>) = viewModelScope.launch {
        pendingTaskRepository.deleteList(idList)
    }
}