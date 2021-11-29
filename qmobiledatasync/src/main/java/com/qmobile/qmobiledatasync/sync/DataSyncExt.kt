/*
 * Created by Quentin Marciset on 31/3/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.sync

import androidx.lifecycle.Observer
import com.qmobile.qmobileapi.network.ApiClient
import com.qmobile.qmobiledatasync.viewmodel.EntityListViewModel
import timber.log.Timber

fun DataSync.setupObservable(
    entityListViewModelList: List<EntityListViewModel<*>>,
    globalStampObserver: Observer<GlobalStampWithTable>
) {
    mediatorLiveDataList = mutableListOf()
    entityListViewModelList.createMediatorLiveData(mediatorLiveDataList)
    // observe merged LiveData
    mediatorLiveDataList.setObservers(activity, globalStampObserver)
}

fun DataSync.successfulSynchronization(
    maxGlobalStamp: Int,
    entityListViewModelList: List<EntityListViewModel<*>>
) {
    Timber.i("[Synchronization performed, all tables are up-to-date]")
    mediatorLiveDataList.removeObservers(activity)
    sharedPreferencesHolder.globalStamp = maxGlobalStamp
    entityListViewModelList.notifyDataSynced()
    entityListViewModelList.startDataLoading()
    entityListViewModelList.syncDeletedRecords()
    entityListViewModelList.stopDataLoading()
    entityListViewModelList.scheduleRefresh()
    ApiClient.dataSyncFinished()
}

fun DataSync.unsuccessfulSynchronization(
    entityListViewModelList: List<EntityListViewModel<*>>
) {
    Timber.e(
        "[Number of request max limit has been reached. " +
            "Data synchronization is ending with tables not synchronized]"
    )
    mediatorLiveDataList.removeObservers(activity)
    entityListViewModelList.notifyDataUnSynced()
    ApiClient.dataSyncFinished()
}

// Stop observing before going back to login page
fun DataSync.unsuccessfulSynchronizationNeedsLogin(
    entityListViewModelList: List<EntityListViewModel<*>>
) {
    mediatorLiveDataList.removeObservers(activity)
    entityListViewModelList.notifyDataUnSynced()
}

// Closures are used to change the data sync algorithm behavior in unit test
fun DataSync.initClosures() {

    // Set mediatorLiveData to observe every table
    val defaultSetupObservableClosure: (List<EntityListViewModel<*>>, Observer<GlobalStampWithTable>) -> Unit =
        { entityListViewModelList, globalStampObserver ->
            setupObservable(entityListViewModelList, globalStampObserver)
        }

    setupObservableClosure = defaultSetupObservableClosure

    // Synchronization api requests
    val defaultSyncClosure: (EntityListViewModel<*>) -> Unit = { entityListViewModel ->
//        entityListViewModel.sync(this.activity)
        entityListViewModel.setDataSyncState(DataSyncStateEnum.SYNCHRONIZING)
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
