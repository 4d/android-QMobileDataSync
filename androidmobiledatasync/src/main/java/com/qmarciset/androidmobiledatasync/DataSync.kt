/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmarciset.androidmobiledatasync

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer
import com.qmarciset.androidmobileapi.auth.AuthInfoHelper
import com.qmarciset.androidmobiledatasync.model.EntityViewModelIsToSync
import com.qmarciset.androidmobiledatasync.model.GlobalStampWithTable
import java.util.concurrent.atomic.AtomicInteger
import timber.log.Timber

open class DataSync(
    private val activity: AppCompatActivity,
    private val authInfoHelper: AuthInfoHelper
) {

    companion object {
        var NUMBER_OF_REQUEST_MAX_LIMIT = 0
        const val FACTOR_OF_MAX_SUCCESSIVE_SYNC = 3
    }

    private var nbToReceive = 0

    private lateinit var liveDataMerger: MediatorLiveData<GlobalStampWithTable>

    fun setObserver(entityViewModelIsToSyncList: List<EntityViewModelIsToSync>) {

        var viewModelStillInitializing = true
        val received = AtomicInteger(0)
        var requestPerformed = 0
        nbToReceive = entityViewModelIsToSyncList.filter { it.isToSync }.size
        NUMBER_OF_REQUEST_MAX_LIMIT = nbToReceive * FACTOR_OF_MAX_SUCCESSIVE_SYNC

        val receivedSyncedTableGS = mutableListOf<GlobalStampWithTable>()

        liveDataMerger = MediatorLiveData()

        // merge LiveData
        for (entityViewModelIsToSync in entityViewModelIsToSyncList) {
            liveDataMerger.addSource(entityViewModelIsToSync.vm.globalStamp) {
                if (it != null) {
                    liveDataMerger.value =
                        GlobalStampWithTable(
                            entityViewModelIsToSync.vm.getAssociatedTableName(),
                            it
                        )
                }
            }
        }

        val globalStampObserver = Observer<GlobalStampWithTable> { globalStampWithTable ->
            if (!viewModelStillInitializing) {
                Timber.d("[globalStampObserver] [Table : ${globalStampWithTable.tableName}, value : ${globalStampWithTable.globalStamp}]")

                receivedSyncedTableGS.add(globalStampWithTable)

                Timber.d("nbToReceive = $nbToReceive, received = ${received.get() + 1}")
                if (received.incrementAndGet() == nbToReceive) {

                    // Get the max globalStamp between received ones, and stored one
                    val maxGlobalStamp = getMaxGlobalStamp(receivedSyncedTableGS, authInfoHelper.globalStamp)

                    val isAtLeastOneToSync = checkIfAtLeastOneTableToSync(maxGlobalStamp, entityViewModelIsToSyncList)

                    if (isAtLeastOneToSync) {
                        received.set(0)
                        requestPerformed++
                        if (requestPerformed <= NUMBER_OF_REQUEST_MAX_LIMIT)
                            sync(entityViewModelIsToSyncList)
                    } else {
                        validateSynchronization(maxGlobalStamp, entityViewModelIsToSyncList)
                    }
                }
            } else {
                Timber.d("nbToReceive = $nbToReceive, received = ${received.get() + 1}")
                if (received.incrementAndGet() == nbToReceive) {
                    viewModelStillInitializing = false
                    received.set(0)
                    // first sync
                    sync(entityViewModelIsToSyncList)
                }
            }
        }

        // observe merged LiveData
        liveDataMerger.observe(activity, globalStampObserver)
    }

    fun checkIfAtLeastOneTableToSync(maxGlobalStamp: Int, entityViewModelIsToSyncList: List<EntityViewModelIsToSync>): Boolean {
        var isAtLeastOneToSync = false
        for (entityViewModelIsToSync in entityViewModelIsToSyncList) {
            val vmGs = entityViewModelIsToSync.vm.globalStamp.value ?: 0
            if (vmGs < maxGlobalStamp) {
                entityViewModelIsToSync.isToSync = true
                isAtLeastOneToSync = true
            }
        }
        Timber.d("isAtLeastOneToSync = $isAtLeastOneToSync")
        return isAtLeastOneToSync
    }

    fun getMaxGlobalStamp(receivedSyncedTableGS: List<GlobalStampWithTable>, authInfoHelperGlobalStamp: Int): Int = maxOf(receivedSyncedTableGS.map { it.globalStamp }.maxBy { it } ?: 0, authInfoHelperGlobalStamp)

    private fun validateSynchronization(maxGlobalStamp: Int, entityViewModelIsToSyncList: List<EntityViewModelIsToSync>) {
        Timber.d("Synchronization performed, all tables are up-to-date")
        liveDataMerger.removeObservers(activity)
        authInfoHelper.globalStamp = maxGlobalStamp
        for (entityViewModelIsToSync in entityViewModelIsToSyncList) {
            // notify data are synced
            entityViewModelIsToSync.vm.dataSynchronized.postValue(
                DataSyncState.SYNCHRONIZED
            )
        }
    }

    private fun sync(entityViewModelIsToSyncList: List<EntityViewModelIsToSync>) {

        nbToReceive = entityViewModelIsToSyncList.filter { it.isToSync }.size
        NUMBER_OF_REQUEST_MAX_LIMIT = nbToReceive * FACTOR_OF_MAX_SUCCESSIVE_SYNC

        for (entityViewModelIsToSync in entityViewModelIsToSyncList) {

            entityViewModelIsToSync.vm.dataSynchronized.postValue(DataSyncState.SYNCHRONIZING)

            Timber.d(
                "Sync : tableName = ${entityViewModelIsToSync.vm.getAssociatedTableName()}, " +
                        "isToSync : ${entityViewModelIsToSync.isToSync}"
            )

            if (entityViewModelIsToSync.isToSync) {
                entityViewModelIsToSync.isToSync = false
                entityViewModelIsToSync.vm.getData {
                }
            }
        }
    }
}
