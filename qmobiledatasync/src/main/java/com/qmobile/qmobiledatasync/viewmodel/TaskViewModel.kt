/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.viewmodel

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.qmobile.qmobiledatastore.dao.ActionTask
import com.qmobile.qmobiledatastore.repository.TaskRepository
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.utils.getViewModelScope
import kotlinx.coroutines.launch

class TaskViewModel : AndroidViewModel(BaseApp.instance) {

    private val coroutineScope = getViewModelScope()
    private val dao = BaseApp.daoProvider.getActionTaskDao()
    private val taskRepository = TaskRepository(dao)

    val pendingTasks: LiveData<List<ActionTask>> = taskRepository.getAllPending()
    val allTasks: LiveData<List<ActionTask>> = taskRepository.getAll()

    fun getTask(id: String): LiveData<ActionTask> = taskRepository.getOne(id)

    fun deleteOne(id: String) = coroutineScope.launch {
        taskRepository.deleteOne(id)
    }

    fun deleteAll() = coroutineScope.launch {
        taskRepository.deleteAll()
    }

    fun insert(actionTask: ActionTask) = coroutineScope.launch {
        taskRepository.insert(actionTask)
    }

    // As we need to show only the 10 latest History Items, so we need this function to clean oldest items
    fun deleteList(idList: List<String>) = coroutineScope.launch {
        taskRepository.deleteList(idList)
    }
}
