/*
 * Created by Quentin Marciset on 26/6/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

@file:Suppress("UNCHECKED_CAST")

package com.qmobile.qmobiledatasync.viewmodel

import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobiledatastore.data.RoomData
import kotlinx.coroutines.launch

/**
 * Room database
 */

fun <T : EntityModel> EntityListViewModel<T>.insert(item: EntityModel) = coroutineScope.launch {
    roomRepository.insert(item as RoomData)
}

fun <T : EntityModel> EntityListViewModel<T>.insertAll(items: List<EntityModel>) = coroutineScope.launch {
    roomRepository.insertAll(items as List<RoomData>)
}

fun <T : EntityModel> EntityListViewModel<T>.deleteOne(id: String) = coroutineScope.launch {
    roomRepository.deleteOne(id)
}

fun <T : EntityModel> EntityListViewModel<T>.deleteAll() = coroutineScope.launch {
    roomRepository.deleteAll()
}
