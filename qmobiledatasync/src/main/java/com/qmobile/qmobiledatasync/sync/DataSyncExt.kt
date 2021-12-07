/*
 * Created by Quentin Marciset on 31/3/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.sync

import com.qmobile.qmobileapi.network.ApiClient
import com.qmobile.qmobiledatasync.utils.collectWhenStarted
import com.qmobile.qmobiledatasync.viewmodel.EntityListViewModel
import timber.log.Timber

fun DataSync.setupObservable(
    entityListViewModelList: List<EntityListViewModel<*>>,
    globalStampObserver: suspend (value: GlobalStamp) -> Unit
) {
    entityListViewModelList.map { it.globalStamp }.forEach { stateFlow ->
        lifecycleOwner.collectWhenStarted(flow = stateFlow, action = globalStampObserver)
    }
}

fun DataSync.successfulSynchronization(
    maxGlobalStamp: Int,
    entityListViewModelList: List<EntityListViewModel<*>>
) {
    Timber.i("[Synchronization performed, all tables are up-to-date]")
    sharedPreferencesHolder.globalStamp = maxGlobalStamp
    entityListViewModelList.notifyDataSynced()
    entityListViewModelList.startDataLoading()
    entityListViewModelList.syncDeletedRecords()
    entityListViewModelList.stopDataLoading()
    entityListViewModelList.scheduleRefresh()
    ApiClient.dataSyncFinished()
}

fun unsuccessfulSynchronization(
    entityListViewModelList: List<EntityListViewModel<*>>
) {
    Timber.e(
        "[Number of request max limit has been reached. " +
            "Data synchronization is ending with tables not synchronized]"
    )
    entityListViewModelList.notifyDataUnSynced()
    ApiClient.dataSyncFinished()
}

// Closures are used to change the data sync algorithm behavior in unit test
fun DataSync.initClosures() {

    // Set mediatorLiveData to observe every table
    val defaultSetupObservableClosure: (List<EntityListViewModel<*>>, suspend (value: GlobalStamp) -> Unit) -> Unit =
        { entityListViewModelList, globalStampObserver ->
            setupObservable(entityListViewModelList, globalStampObserver)
        }

    setupObservableClosure = defaultSetupObservableClosure

    // Synchronization api requests
    val defaultSyncClosure: (EntityListViewModel<*>, Boolean) -> Unit =
        { entityListViewModel, reSync ->
            val state = if (reSync) DataSyncStateEnum.RESYNC else DataSyncStateEnum.SYNCHRONIZING
            entityListViewModel.setDataSyncState(state)
        }

    syncClosure = defaultSyncClosure

    // Successful end of synchronization
    val defaultSuccessfulSyncClosure: (Int, List<EntityListViewModel<*>>) -> Unit =
        { maxGlobalStamp, entityListViewModelList ->
            successfulSynchronization(maxGlobalStamp, entityListViewModelList)
        }

    successfulSyncClosure = defaultSuccessfulSyncClosure

    // Unsuccessful end of synchronization
    val defaultUnsuccessfulSyncClosure: (List<EntityListViewModel<*>>) -> Unit =
        { entityListViewModelList ->
            unsuccessfulSynchronization(entityListViewModelList)
        }

    unsuccessfulSyncClosure = defaultUnsuccessfulSyncClosure
}
