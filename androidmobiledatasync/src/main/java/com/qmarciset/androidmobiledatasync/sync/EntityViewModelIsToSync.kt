/*
 * Created by Quentin Marciset on 31/3/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmarciset.androidmobiledatasync.sync

import androidx.lifecycle.MediatorLiveData
import com.qmarciset.androidmobiledatasync.viewmodel.EntityListViewModel
import timber.log.Timber

data class EntityViewModelIsToSync(val vm: EntityListViewModel<*>, var isToSync: Boolean) {

    fun sync() {
        this.vm.dataSynchronized.postValue(DataSyncState.SYNCHRONIZING)

        Timber.d("[Sync] [Table : ${this.vm.getAssociatedTableName()}, isToSync : ${this.isToSync}]")

        if (this.isToSync) {
            this.isToSync = false
            this.vm.getData {
                Timber.v("Requesting data for ${vm.getAssociatedTableName()}")
            }
        }
    }

    fun createMediatorLiveData(): MediatorLiveData<GlobalStampWithTable> {

        val mediatorLiveData = MediatorLiveData<GlobalStampWithTable>()
        mediatorLiveData.addSource(this.vm.globalStamp) {
            if (it != null) {
                mediatorLiveData.value =
                    GlobalStampWithTable(
                        this.vm.getAssociatedTableName(),
                        it
                    )
            }
        }
        return mediatorLiveData
    }

    fun notifyDataSynced() {
        this.vm.dataSynchronized.postValue(
            DataSyncState.SYNCHRONIZED
        )
    }

    fun notifyDataUnSynced() {
        this.vm.dataSynchronized.postValue(
            DataSyncState.UNSYNCHRONIZED
        )
    }
}

fun List<EntityViewModelIsToSync>.syncDeletedRecords() {
    // We pick first viewModel to perform a deletedRecords request, but it could be any viewModel.
    // The goal is to get a RestRepository to perform the request.
    this[0].vm.getDeletedRecords { deletedRecordList ->
        for (deletedRecord in deletedRecordList) {
            deletedRecord.__PrimaryKey?.let { recordKey ->
                for (entityViewModel in this) {
                    if (deletedRecord.__TableName == entityViewModel.vm.getAssociatedTableName()) {
                        entityViewModel.vm.deleteOne(recordKey)
                        break
                    }
                }
            }
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

fun List<EntityViewModelIsToSync>.notifyDataSynced() {
    for (entityViewModelIsToSync in this) {
        entityViewModelIsToSync.notifyDataSynced()
    }
}

fun List<EntityViewModelIsToSync>.notifyDataUnSynced() {
    for (entityViewModelIsToSync in this) {
        entityViewModelIsToSync.notifyDataUnSynced()
    }
}