/*
 * Created by Quentin Marciset on 31/3/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmarciset.androidmobiledatasync.sync

import androidx.lifecycle.Observer
import com.qmarciset.androidmobileapi.network.ApiClient
import timber.log.Timber

fun DataSync.setupObservable(
    entityViewModelIsToSyncList: List<EntityViewModelIsToSync>,
    globalStampObserver: Observer<GlobalStampWithTable>
) {
    mediatorLiveDataList = mutableListOf()
    entityViewModelIsToSyncList.createMediatorLiveData(mediatorLiveDataList)
    // observe merged LiveData
    mediatorLiveDataList.setObservers(activity, globalStampObserver)
}

fun DataSync.successfulSynchronization(
    maxGlobalStamp: Int,
    entityViewModelIsToSyncList: List<EntityViewModelIsToSync>
) {
    Timber.i("[Synchronization performed, all tables are up-to-date]")
    mediatorLiveDataList.removeObservers(activity)
    authInfoHelper.globalStamp = maxGlobalStamp
    entityViewModelIsToSyncList.notifyDataSynced()
    entityViewModelIsToSyncList.syncDeletedRecords()
    ApiClient.dataSyncFinished()
}

fun DataSync.unsuccessfulSynchronization(
    entityViewModelIsToSyncList: List<EntityViewModelIsToSync>
) {
    Timber.e(
        "[Number of request max limit has been reached. " +
                "Data synchronization is ending with tables not synchronized]"
    )
    mediatorLiveDataList.removeObservers(activity)
    entityViewModelIsToSyncList.notifyDataUnSynced()
    ApiClient.dataSyncFinished()
}

// Stop observing before going back to login page
fun DataSync.unsuccessfulSynchronizationNeedsLogin(
    entityViewModelIsToSyncList: List<EntityViewModelIsToSync>
) {
    mediatorLiveDataList.removeObservers(activity)
    entityViewModelIsToSyncList.notifyDataUnSynced()
}

// Closures are used to change the data sync algorithm behavior in unit test
fun DataSync.initClosures() {

    // Set mediatorLiveData to observe every table
    val defaultSetupObservableClosure: (List<EntityViewModelIsToSync>, Observer<GlobalStampWithTable>) -> Unit =
        { entityViewModelIsToSyncList, globalStampObserver ->
            setupObservable(entityViewModelIsToSyncList, globalStampObserver)
        }

    setupObservableClosure = defaultSetupObservableClosure

    // Synchronization api requests
    val defaultSyncClosure: (EntityViewModelIsToSync) -> Unit = { entityViewModelIsToSync ->
        entityViewModelIsToSync.sync()
    }

    syncClosure = defaultSyncClosure

    // Successful end of synchronization
    val defaultSuccessfulSyncClosure: (Int, List<EntityViewModelIsToSync>) -> Unit =
        { maxGlobalStamp, entityViewModelIsToSyncList ->
            successfulSynchronization(maxGlobalStamp, entityViewModelIsToSyncList)
        }

    successfulSyncClosure = defaultSuccessfulSyncClosure

    // Unsuccessful end of synchronization
    val defaultUnsuccessfulSyncClosure: (List<EntityViewModelIsToSync>) -> Unit =
        { entityViewModelIsToSyncList ->
            unsuccessfulSynchronization(entityViewModelIsToSyncList)
        }

    unsuccessfulSyncClosure = defaultUnsuccessfulSyncClosure
}
