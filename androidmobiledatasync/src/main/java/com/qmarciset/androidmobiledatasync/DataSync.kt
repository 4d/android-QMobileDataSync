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
import java.util.concurrent.atomic.AtomicBoolean
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

    private lateinit var mediatorLiveDataList: MutableList<MediatorLiveData<GlobalStampWithTable>>

    fun setObserver(entityViewModelIsToSyncList: List<EntityViewModelIsToSync>) {

        val viewModelStillInitializing = AtomicBoolean(true)
        val received = AtomicInteger(0)
        val requestPerformed = AtomicInteger(0)
        nbToReceive = entityViewModelIsToSyncList.filter { it.isToSync }.size
        NUMBER_OF_REQUEST_MAX_LIMIT = nbToReceive * FACTOR_OF_MAX_SUCCESSIVE_SYNC

        val receivedSyncedTableGS = mutableListOf<GlobalStampWithTable>()

        mediatorLiveDataList = mutableListOf()

        for (entityViewModelIsToSync in entityViewModelIsToSyncList) {
            val mediatorLiveData = MediatorLiveData<GlobalStampWithTable>()
            mediatorLiveData.addSource(entityViewModelIsToSync.vm.globalStamp) {
                if (it != null) {
                    mediatorLiveData.value =
                        GlobalStampWithTable(
                            entityViewModelIsToSync.vm.getAssociatedTableName(),
                            it
                        )
                }
            }
            mediatorLiveDataList.add(mediatorLiveData)
        }

        val globalStampObserver = Observer<GlobalStampWithTable> { globalStampWithTable ->

            Timber.d("[globalStampObserver] [Table : ${globalStampWithTable.tableName}, GlobalStamp : ${globalStampWithTable.globalStamp}]")

            for (entityViewModelIsToSync in entityViewModelIsToSyncList)
                Timber.d("[Table : ${entityViewModelIsToSync.vm.getAssociatedTableName()}, GlobalStamp : ${entityViewModelIsToSync.vm.globalStamp.value}")

            if (!viewModelStillInitializing.get()) {

                receivedSyncedTableGS.add(globalStampWithTable)

                Timber.d("nbToReceive = $nbToReceive, received = ${received.get() + 1}")
                if (received.incrementAndGet() == nbToReceive) {

                    // Get the max globalStamp between received ones, and stored one
                    val maxGlobalStamp =
                        getMaxGlobalStamp(receivedSyncedTableGS, authInfoHelper.globalStamp)
                    Timber.d("maxGlobalStamp = $maxGlobalStamp")

                    val isAtLeastOneToSync =
                        checkIfAtLeastOneTableToSync(maxGlobalStamp, entityViewModelIsToSyncList)

                    if (isAtLeastOneToSync) {
                        Timber.d("isAtLeastOneToSync true")
                        if (canPerformNewSync(
                                received,
                                requestPerformed,
                                NUMBER_OF_REQUEST_MAX_LIMIT
                            )
                        ) {
                            sync(entityViewModelIsToSyncList)
                        } else {
                            Timber.d("Number of request max limit has been reached")
                        }
                    } else {
                        validateSynchronization(maxGlobalStamp, entityViewModelIsToSyncList)
                    }
                }
            } else {

                if (canStartSync(received, nbToReceive, viewModelStillInitializing)) {
                    // first sync
                    sync(entityViewModelIsToSyncList)
                }
            }
        }

        // observe merged LiveData
        for (mediatorLiveData in mediatorLiveDataList) {
            mediatorLiveData.observe(activity, globalStampObserver)
        }
    }

    fun canStartSync(
        received: AtomicInteger,
        nbToReceive: Int,
        viewModelStillInitializing: AtomicBoolean
    ): Boolean {
        println("nbToReceive = $nbToReceive, received = ${received.get() + 1}")
        if (received.incrementAndGet() == nbToReceive) {
            viewModelStillInitializing.set(false)
            received.set(0)
            return true
        }
        return false
    }

    fun canPerformNewSync(
        received: AtomicInteger,
        requestPerformed: AtomicInteger,
        NUMBER_OF_REQUEST_MAX_LIMIT: Int
    ): Boolean {
        received.set(0)
        requestPerformed.incrementAndGet()
        return requestPerformed.get() <= NUMBER_OF_REQUEST_MAX_LIMIT
    }

    fun checkIfAtLeastOneTableToSync(
        maxGlobalStamp: Int,
        entityViewModelIsToSyncList: List<EntityViewModelIsToSync>
    ): Boolean {
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

    fun getMaxGlobalStamp(
        receivedSyncedTableGS: List<GlobalStampWithTable>,
        authInfoHelperGlobalStamp: Int
    ): Int = maxOf(receivedSyncedTableGS.map { it.globalStamp }.maxBy { it } ?: 0,
        authInfoHelperGlobalStamp)

    private fun validateSynchronization(
        maxGlobalStamp: Int,
        entityViewModelIsToSyncList: List<EntityViewModelIsToSync>
    ) {
        Timber.d("Synchronization performed, all tables are up-to-date")
        for (mediatorLiveData in mediatorLiveDataList) {
            mediatorLiveData.removeObservers(activity)
        }
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

            Timber.d("Sync : tableName = ${entityViewModelIsToSync.vm.getAssociatedTableName()}, isToSync : ${entityViewModelIsToSync.isToSync}")

            if (entityViewModelIsToSync.isToSync) {
                entityViewModelIsToSync.isToSync = false
                entityViewModelIsToSync.vm.getData {
                }
            }
        }
    }
}
