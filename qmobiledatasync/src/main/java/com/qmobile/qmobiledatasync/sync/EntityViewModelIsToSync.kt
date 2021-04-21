/*
 * Created by Quentin Marciset on 31/3/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.sync

import androidx.lifecycle.MediatorLiveData
import com.qmobile.qmobileapi.utils.getSafeString
import com.qmobile.qmobileapi.utils.retrieveJSONObject
import com.qmobile.qmobiledatasync.viewmodel.EntityListViewModel
import com.qmobile.qmobiledatasync.viewmodel.deleteOne
import org.json.JSONObject
import timber.log.Timber

data class EntityViewModelIsToSync(val vm: EntityListViewModel<*>, var isToSync: Boolean) {

    fun sync() {
        this.vm.dataSynchronized.postValue(DataSyncStateEnum.SYNCHRONIZING)

        Timber.d("[Sync] [Table : ${this.vm.getAssociatedTableName()}, isToSync : ${this.isToSync}]")

        if (this.isToSync) {
            this.isToSync = false
            this.vm.getEntities {
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
            DataSyncStateEnum.SYNCHRONIZED
        )
    }

    fun notifyDataUnSynced() {
        this.vm.dataSynchronized.postValue(
            DataSyncStateEnum.UNSYNCHRONIZED
        )
    }

    fun startDataLoading() {
        this.vm.dataLoading.value = true
    }

    fun stopDataLoading() {
        this.vm.dataLoading.value = false
    }
}

fun List<EntityViewModelIsToSync>.syncDeletedRecords() {
    // We pick first viewModel to perform a deletedRecords request, but it could be any viewModel.
    // The goal is to get a RestRepository to perform the request.
    this[0].vm.getDeletedRecords { entitiesList ->
        for (deletedRecordString in entitiesList) {
            this.deleteRecord(retrieveJSONObject(deletedRecordString))
        }
    }
}

fun List<EntityViewModelIsToSync>.deleteRecord(deletedRecordJson: JSONObject?) {
    deletedRecordJson?.getSafeString("__PrimaryKey")?.let { recordKey ->
        deletedRecordJson.getSafeString("__TableName")?.let { tableName ->
            for (entityViewModel in this) {
                if (tableName == entityViewModel.vm.getAssociatedTableName()) {
                    entityViewModel.vm.deleteOne(recordKey)
                    break
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

fun List<EntityViewModelIsToSync>.startDataLoading() {
    for (entityViewModelIsToSync in this) {
        entityViewModelIsToSync.startDataLoading()
    }
}

fun List<EntityViewModelIsToSync>.stopDataLoading() {
    for (entityViewModelIsToSync in this) {
        entityViewModelIsToSync.stopDataLoading()
    }
}
