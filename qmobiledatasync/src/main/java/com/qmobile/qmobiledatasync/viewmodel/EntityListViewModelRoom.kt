/*
 * Created by Quentin Marciset on 26/6/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

@file:Suppress("UNCHECKED_CAST")

package com.qmobile.qmobiledatasync.viewmodel

import androidx.lifecycle.viewModelScope
import com.qmobile.qmobileapi.model.entity.EntityModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * Room database
 */

fun <T : EntityModel> EntityListViewModel<T>.insert(item: EntityModel) = viewModelScope.launch {
    roomRepository.insert(item as T)
}

fun <T : EntityModel> EntityListViewModel<T>.insertAll(items: List<EntityModel>) = viewModelScope.launch {
    roomRepository.insertAll(items as List<T>)
}

fun <T : EntityModel> EntityListViewModel<T>.deleteOne(id: String) = coroutineScope.launch {
    roomRepository.deleteOne(id)
}

/*fun <T : EntityModel> EntityListViewModel<T>.delete(item: EntityModel) = viewModelScope.launch {
    roomRepository.delete(item as T)
}*/

/*fun <T : EntityModel> EntityListViewModel<T>.deleteAll() = viewModelScope.launch {
    roomRepository.deleteAll()
}*/
