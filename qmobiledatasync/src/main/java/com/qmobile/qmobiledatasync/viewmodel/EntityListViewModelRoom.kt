/*
 * Created by Quentin Marciset on 26/6/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

@file:Suppress("UNCHECKED_CAST")

package com.qmobile.qmobiledatasync.viewmodel

import com.qmobile.qmobileapi.model.entity.EntityModel

/**
 * Room database
 */

fun <T : EntityModel> EntityListViewModel<T>.insert(item: EntityModel) {
    roomRepository.insert(item as T)
}

fun <T : EntityModel> EntityListViewModel<T>.insertAll(items: List<EntityModel>) {
    roomRepository.insertAll(items as List<T>)
}

fun <T : EntityModel> EntityListViewModel<T>.delete(item: EntityModel) {
    roomRepository.delete(item as T)
}

fun <T : EntityModel> EntityListViewModel<T>.deleteOne(id: String) {
    roomRepository.deleteOne(id)
}

fun <T : EntityModel> EntityListViewModel<T>.deleteAll() {
    roomRepository.deleteAll()
}
