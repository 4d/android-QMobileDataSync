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

class DataSync(
    private val activity: AppCompatActivity,
    private val authInfoHelper: AuthInfoHelper
) {

    companion object {
        const val FACTOR_OF_MAX_SUCCESSIVE_SYNC = 3
    }

    var nbToReceive = 0
    var maxGlobalStamp = 0
    private var numberOfRequestMaxLimit = 0
    lateinit var received: AtomicInteger

    lateinit var globalStampObserver: Observer<GlobalStampWithTable>

    private lateinit var mediatorLiveDataList: MutableList<MediatorLiveData<GlobalStampWithTable>>

    // Default closures
    lateinit var setupMediatorLiveDataClosure: (List<EntityViewModelIsToSync>) -> Unit
    lateinit var observeMergedLiveDataClosure: (Observer<GlobalStampWithTable>) -> Unit
    lateinit var syncClosure: (EntityViewModelIsToSync) -> Unit
    lateinit var successfulSyncClosure: (Int, List<EntityViewModelIsToSync>) -> Unit
    lateinit var unsuccessfulSyncClosure: (List<EntityViewModelIsToSync>) -> Unit

    init {
        initClosures()
    }

    fun setObserver(
        entityViewModelIsToSyncList: List<EntityViewModelIsToSync>,
        alreadyRefreshedTable: String?
    ) {

        received = AtomicInteger(0)
        val viewModelStillInitializing = AtomicBoolean(true)
        val requestPerformed = AtomicInteger(0)
        val nbToReceiveForInitializing = AtomicInteger(entityViewModelIsToSyncList.size)
        val receivedSyncedTableGS = mutableListOf<GlobalStampWithTable>()

        mediatorLiveDataList = mutableListOf()
        setupMediatorLiveDataClosure(entityViewModelIsToSyncList)

        globalStampObserver = Observer { globalStampWithTable ->
            if (!viewModelStillInitializing.get()) {

                // For a forced data synchronization, we want to ignore the observation of the
                // received globalStamp for this table. It is well saved in the viewModel, but we
                // don't want to treat its reception here.
                if (globalStampWithTable.tableName == alreadyRefreshedTable) {
                    Timber.d("[Ignoring received observable for Table : ${globalStampWithTable.tableName} with GlobalStamp : ${globalStampWithTable.globalStamp}]")
                    return@Observer
                }

                Timber.d("[NEW] [Table : ${globalStampWithTable.tableName}, GlobalStamp : ${globalStampWithTable.globalStamp}]")
                Timber.d("Current globalStamps list :")

                for (entityViewModelIsToSync in entityViewModelIsToSyncList) {
                    Timber.d(" - Table : ${entityViewModelIsToSync.vm.getAssociatedTableName()}, GlobalStamp : ${entityViewModelIsToSync.vm.globalStamp.value}")
                }

                Timber.d("[GlobalStamps received : ${received.get() + 1}/$nbToReceive]")

                receivedSyncedTableGS.add(globalStampWithTable)

                if (received.incrementAndGet() == nbToReceive) {

                    // Get the max globalStamp between received ones, and stored one
                    maxGlobalStamp =
                        getMaxGlobalStamp(receivedSyncedTableGS, authInfoHelper.globalStamp)
                    Timber.d("[maxGlobalStamp = $maxGlobalStamp]")

                    val isAtLeastOneToSync =
                        checkIfAtLeastOneTableToSync(maxGlobalStamp, entityViewModelIsToSyncList)

                    if (isAtLeastOneToSync) {
                        Timber.d("[There is at least one table that requires data synchronization]")
                        if (canPerformNewSync(received, requestPerformed, numberOfRequestMaxLimit)) {
                            syncTables(entityViewModelIsToSyncList)
                        } else {
                            unsuccessfulSyncClosure(entityViewModelIsToSyncList)
                        }
                    } else {
                        successfulSyncClosure(maxGlobalStamp, entityViewModelIsToSyncList)
                    }
                }
            } else {
                Timber.d("[INITIALIZING] [Table : ${globalStampWithTable.tableName}, GlobalStamp : ${globalStampWithTable.globalStamp}]")
                Timber.d("[GlobalStamps received for initializing : ${received.get() + 1}/${nbToReceiveForInitializing.get()}]")
                if (canStartSync(
                        received,
                        nbToReceiveForInitializing,
                        viewModelStillInitializing
                    )
                ) {
                    // first sync
                    syncTables(entityViewModelIsToSyncList)
                }
            }
        }

        // observe merged LiveData
        observeMergedLiveDataClosure(globalStampObserver)
    }

    private fun canStartSync(
        received: AtomicInteger,
        nbToReceiveForInitializing: AtomicInteger,
        viewModelStillInitializing: AtomicBoolean
    ): Boolean {
        if (received.incrementAndGet() == nbToReceiveForInitializing.get()) {
            viewModelStillInitializing.set(false)
            received.set(0)
            return true
        }
        return false
    }

    private fun canPerformNewSync(
        received: AtomicInteger,
        requestPerformed: AtomicInteger,
        numberOfRequestMaxLimit: Int
    ): Boolean {
        received.set(0)
        requestPerformed.incrementAndGet()
        return requestPerformed.get() <= numberOfRequestMaxLimit
    }

    private fun checkIfAtLeastOneTableToSync(
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
        return isAtLeastOneToSync
    }

    private fun getMaxGlobalStamp(
        receivedSyncedTableGS: List<GlobalStampWithTable>,
        authInfoHelperGlobalStamp: Int
    ): Int = maxOf(receivedSyncedTableGS.map { it.globalStamp }.maxBy { it } ?: 0,
        authInfoHelperGlobalStamp)

    //    @Synchronized
    private fun syncTables(entityViewModelIsToSyncList: List<EntityViewModelIsToSync>) {
        this.nbToReceive = entityViewModelIsToSyncList.filter { it.isToSync }.size
        this.numberOfRequestMaxLimit = nbToReceive * FACTOR_OF_MAX_SUCCESSIVE_SYNC

        val syncRequiredList = entityViewModelIsToSyncList.filter { it.isToSync }

        for (syncRequired in syncRequiredList) {
            syncClosure(syncRequired)
        }
    }

    // Closures are used to change the algorithm behavior in unit test
    private fun initClosures() {

        // Setup mediatorLiveData for custom observation
        val defaultMediatorClosure: (List<EntityViewModelIsToSync>) -> Unit = { entityViewModelIsToSyncList ->
            setupMediatorLiveData(entityViewModelIsToSyncList)
        }

        setupMediatorLiveDataClosure = defaultMediatorClosure

        // Set the observer for the every table
        val defaultMergeLiveDataClosure: (Observer<GlobalStampWithTable>) -> Unit = { globalStampObserver ->
            observeMergedLiveData(globalStampObserver)
        }

        observeMergedLiveDataClosure = defaultMergeLiveDataClosure

        // Synchronization api requests
        val defaultSyncClosure: (EntityViewModelIsToSync) -> Unit = { entityViewModelIsToSync ->
            entityViewModelIsToSync.sync()
        }

        syncClosure = defaultSyncClosure

        // Successful end of synchronization
        val defaultSuccessfulSyncClosure: (Int, List<EntityViewModelIsToSync>) -> Unit = { maxGlobalStamp, entityViewModelIsToSyncList ->
            successfulSynchronization(maxGlobalStamp, entityViewModelIsToSyncList)
        }

        successfulSyncClosure = defaultSuccessfulSyncClosure

        // Unsuccessful end of synchronization
        val defaultUnsuccessfulSyncClosure: (List<EntityViewModelIsToSync>) -> Unit = { entityViewModelIsToSyncList ->
            unsuccessfulSynchronization(entityViewModelIsToSyncList)
        }

        unsuccessfulSyncClosure = defaultUnsuccessfulSyncClosure
    }

    private fun setupMediatorLiveData(entityViewModelIsToSyncList: List<EntityViewModelIsToSync>) {
        entityViewModelIsToSyncList.createMediatorLiveData(mediatorLiveDataList)
    }

    private fun observeMergedLiveData(globalStampObserver: Observer<GlobalStampWithTable>) {
        mediatorLiveDataList.setObservers(activity, globalStampObserver)
    }

    private fun successfulSynchronization(
        maxGlobalStamp: Int,
        entityViewModelIsToSyncList: List<EntityViewModelIsToSync>
    ) {
        Timber.i("[Synchronization performed, all tables are up-to-date]")
        mediatorLiveDataList.removeObservers(activity)
        authInfoHelper.globalStamp = maxGlobalStamp
        entityViewModelIsToSyncList.notifyDataSynced()
    }

    private fun unsuccessfulSynchronization(
        entityViewModelIsToSyncList: List<EntityViewModelIsToSync>
    ) {
        Timber.e("[Number of request max limit has been reached. Data synchronization is ending with tables not synchronized]")
        mediatorLiveDataList.removeObservers(activity)
        entityViewModelIsToSyncList.notifyDataUnSynced()
    }
}
