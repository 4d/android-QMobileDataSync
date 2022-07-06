/*
 * Created by Quentin Marciset on 31/3/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.sync

import androidx.lifecycle.Lifecycle
import com.qmobile.qmobileapi.network.ApiClient
import com.qmobile.qmobiledatasync.utils.launchAndCollectIn
import com.qmobile.qmobiledatasync.viewmodel.EntityListViewModel
import kotlinx.coroutines.CoroutineScope
import timber.log.Timber

fun DataSync.setupCollect(
    globalStampObserver: suspend CoroutineScope.(value: GlobalStamp) -> Unit
) {
    entityListViewModelList.map { it.globalStamp }.forEach { stateFlow ->
        stateFlow.launchAndCollectIn(lifecycleOwner, Lifecycle.State.STARTED, globalStampObserver)
    }
}

fun DataSync.successfulSynchronization(
    maxGlobalStamp: Int
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

fun DataSync.unsuccessfulSynchronization() {
    Timber.e(
        "[Number of request max limit has been reached. " +
            "Data synchronization is ending with tables not synchronized]"
    )
    entityListViewModelList.notifyDataUnSynced()
    ApiClient.dataSyncFinished()
}

// Closures are used to change the data sync algorithm behavior in unit test
fun DataSync.initClosures() {
    val defaultSetupObservableClosure: (suspend CoroutineScope.(value: GlobalStamp) -> Unit) -> Unit =
        { globalStampObserver ->
            setupCollect(globalStampObserver)
        }

    setupObservableClosure = defaultSetupObservableClosure

    // Synchronization api requests
    val defaultSyncClosure: (EntityListViewModel<*>, Boolean) -> Unit =
        { entityListViewModel, reSync ->
            val state = if (reSync) DataSync.State.RESYNC else DataSync.State.SYNCHRONIZING
            entityListViewModel.setDataSyncState(state)
        }

    syncClosure = defaultSyncClosure

    // Successful end of synchronization
    val defaultSuccessfulSyncClosure: (Int) -> Unit =
        { maxGlobalStamp ->
            successfulSynchronization(maxGlobalStamp)
        }

    successfulSyncClosure = defaultSuccessfulSyncClosure

    // Unsuccessful end of synchronization
    val defaultUnsuccessfulSyncClosure: () -> Unit =
        {
            unsuccessfulSynchronization()
        }

    unsuccessfulSyncClosure = defaultUnsuccessfulSyncClosure
}
