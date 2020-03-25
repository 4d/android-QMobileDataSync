/*
 * Created by Quentin Marciset on 24/3/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmarciset.androidmobiledatasync

import androidx.lifecycle.MediatorLiveData
import com.qmarciset.androidmobiledatasync.model.EntityViewModelIsToSync
import com.qmarciset.androidmobiledatasync.model.GlobalStampWithTable
import timber.log.Timber

fun EntityViewModelIsToSync.sync() {

    this.vm.dataSynchronized.postValue(DataSyncState.SYNCHRONIZING)

    Timber.d("[Sync] [Table : ${this.vm.getAssociatedTableName()}, isToSync : ${this.isToSync}]")

    if (this.isToSync) {
        this.isToSync = false
        this.vm.getData {
            Timber.v("Requesting data for ${vm.getAssociatedTableName()}")
        }
    }
}

fun List<EntityViewModelIsToSync>.createMediatorLiveData(
    mediatorLiveDataList: MutableList<MediatorLiveData<GlobalStampWithTable>>
) {
    for (entityViewModelIsToSync in this) {
        val mediatorLiveData = entityViewModelIsToSync.createMediatorLiveData()
        mediatorLiveDataList.add(mediatorLiveData)
    }
}

fun EntityViewModelIsToSync.createMediatorLiveData(): MediatorLiveData<GlobalStampWithTable> {

    val mediatorLiveData = MediatorLiveData<GlobalStampWithTable>()
    mediatorLiveData.addSource(this.vm.globalStamp) {
        if (it != null) {
            mediatorLiveData.value = GlobalStampWithTable(this.vm.getAssociatedTableName(), it)
        }
    }
    return mediatorLiveData
}

fun List<EntityViewModelIsToSync>.notifyDataSynced() {
    for (entityViewModelIsToSync in this) {
        entityViewModelIsToSync.notifyDataSynced()
    }
}

fun EntityViewModelIsToSync.notifyDataSynced() {
    this.vm.dataSynchronized.postValue(
        DataSyncState.SYNCHRONIZED
    )
}

fun List<EntityViewModelIsToSync>.notifyDataUnSynced() {
    for (entityViewModelIsToSync in this) {
        entityViewModelIsToSync.notifyDataUnSynced()
    }
}

fun EntityViewModelIsToSync.notifyDataUnSynced() {
    this.vm.dataSynchronized.postValue(
        DataSyncState.UNSYNCHRONIZED
    )
}
