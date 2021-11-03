/*
 * Created by Quentin Marciset on 31/3/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.sync

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.lifecycleScope
import com.qmobile.qmobileapi.utils.getSafeString
import com.qmobile.qmobileapi.utils.retrieveJSONObject
import com.qmobile.qmobiledatasync.utils.ScheduleRefreshEnum
import com.qmobile.qmobiledatasync.viewmodel.EntityListViewModel
import com.qmobile.qmobiledatasync.viewmodel.deleteOne
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.json.JSONObject
import timber.log.Timber

data class EntityViewModelIsToSync(val vm: EntityListViewModel<*>, var isToSync: Boolean) {

    var job: Job? = null

    fun sync(activity: AppCompatActivity) {
        this.vm.setDataSyncState(DataSyncStateEnum.SYNCHRONIZING)

        Timber.d("[Sync] [Table : ${this.vm.getAssociatedTableName()}, isToSync : ${this.isToSync}]")

        if (this.isToSync) {
            this.isToSync = false
            job?.cancel()
            job = activity.lifecycleScope.launch {
                this@EntityViewModelIsToSync.vm.getEntities {
                    Timber.v("Requested data for ${vm.getAssociatedTableName()}")
                }
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
}

fun List<EntityViewModelIsToSync>.syncDeletedRecords() {
    // We pick first viewModel to perform a deletedRecords request, but it could be any viewModel.
    // The goal is to get a RestRepository to perform the request.
    this[0].vm.getDeletedRecords { entitiesList ->
        entitiesList.forEach { deletedRecordString ->
            this.deleteRecord(retrieveJSONObject(deletedRecordString))
        }
    }
}

fun List<EntityViewModelIsToSync>.deleteRecord(deletedRecordJson: JSONObject?) {
    deletedRecordJson?.getSafeString("__PrimaryKey")?.let { recordKey ->
        deletedRecordJson.getSafeString("__TableName")?.let { tableName ->
            this.findLast { tableName == it.vm.getAssociatedTableName() }?.vm?.deleteOne(recordKey)
        }
    }
}

fun List<EntityViewModelIsToSync>.createMediatorLiveData(
    mediatorLiveDataList: MutableList<MediatorLiveData<GlobalStampWithTable>>
) {
    this.forEach { entityViewModelIsToSync ->
        val mediatorLiveData = entityViewModelIsToSync.createMediatorLiveData()
        mediatorLiveDataList.add(mediatorLiveData)
    }
}

fun List<EntityViewModelIsToSync>.notifyDataSynced() {
    this.forEach { entityViewModelIsToSync ->
        entityViewModelIsToSync.vm.setDataSyncState(DataSyncStateEnum.SYNCHRONIZED)
    }
}

fun List<EntityViewModelIsToSync>.notifyDataUnSynced() {
    this.forEach { entityViewModelIsToSync ->
        entityViewModelIsToSync.vm.setDataSyncState(DataSyncStateEnum.UNSYNCHRONIZED)
    }
}

fun List<EntityViewModelIsToSync>.startDataLoading() {
    this.forEach { entityViewModelIsToSync ->
        entityViewModelIsToSync.vm.setDataLoadingState(true)
    }
}

fun List<EntityViewModelIsToSync>.stopDataLoading() {
    this.forEach { entityViewModelIsToSync ->
        entityViewModelIsToSync.vm.setDataLoadingState(false)
    }
}

fun List<EntityViewModelIsToSync>.scheduleRefresh() {
    this.forEach { entityViewModelIsToSync ->
        if (entityViewModelIsToSync.vm.scheduleRefresh.value == ScheduleRefreshEnum.SCHEDULE) {
            entityViewModelIsToSync.vm.setScheduleRefreshState(ScheduleRefreshEnum.PERFORM)
        }
    }
}
