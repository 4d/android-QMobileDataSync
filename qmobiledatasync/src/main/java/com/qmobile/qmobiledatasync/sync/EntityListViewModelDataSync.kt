/*
 * Created by qmarciset on 26/11/2021.
 * 4D SAS
 * Copyright (c) 2021 qmarciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.sync

import com.qmobile.qmobileapi.utils.getSafeString
import com.qmobile.qmobileapi.utils.retrieveJSONObject
import com.qmobile.qmobiledatasync.utils.ScheduleRefreshEnum
import com.qmobile.qmobiledatasync.viewmodel.EntityListViewModel
import com.qmobile.qmobiledatasync.viewmodel.deleteOne
import org.json.JSONObject
import timber.log.Timber

fun List<EntityListViewModel<*>>.syncDeletedRecords() {
    // We pick first viewModel to perform a deletedRecords request, but it could be any viewModel.
    // The goal is to get a RestRepository to perform the request.
    this[0].getDeletedRecords { entitiesList ->
        entitiesList.forEach { deletedRecordString ->
            this.deleteRecord(retrieveJSONObject(deletedRecordString))
        }
    }
}

fun List<EntityListViewModel<*>>.printGlobalStamp() {
    Timber.d("Current globalStamps list :")
    this.forEach { entityListViewModel ->
        Timber.d(
            " - Table : ${entityListViewModel.getAssociatedTableName()}, " +
                "GlobalStamp : ${entityListViewModel.globalStamp.value.stampValue}"
        )
    }
}

fun List<EntityListViewModel<*>>.deleteRecord(deletedRecordJson: JSONObject?) {
    deletedRecordJson?.getSafeString("__PrimaryKey")?.let { recordKey ->
        deletedRecordJson.getSafeString("__TableName")?.let { tableName ->
            this.findLast { tableName == it.getAssociatedTableName() }?.deleteOne(recordKey)
        }
    }
}

fun List<EntityListViewModel<*>>.notifyDataSynced() {
    this.forEach { entityListViewModel ->
        entityListViewModel.setDataSyncState(DataSyncStateEnum.SYNCHRONIZED)
    }
}

fun List<EntityListViewModel<*>>.notifyDataUnSynced() {
    this.forEach { entityListViewModel ->
        entityListViewModel.setDataSyncState(DataSyncStateEnum.UNSYNCHRONIZED)
    }
}

fun List<EntityListViewModel<*>>.startDataLoading() {
    this.forEach { entityListViewModel ->
        entityListViewModel.setDataLoadingState(true)
    }
}

fun List<EntityListViewModel<*>>.stopDataLoading() {
    this.forEach { entityListViewModel ->
        entityListViewModel.setDataLoadingState(false)
    }
}

fun List<EntityListViewModel<*>>.scheduleRefresh() {
    this.forEach { entityListViewModel ->
        if (entityListViewModel.scheduleRefresh.value == ScheduleRefreshEnum.SCHEDULE) {
            entityListViewModel.setScheduleRefreshState(ScheduleRefreshEnum.PERFORM)
        }
    }
}

fun List<EntityListViewModel<*>>.resetIsToSync() {
    this.forEach { entityListViewModel ->
        entityListViewModel.isToSync.set(true)
    }
}
