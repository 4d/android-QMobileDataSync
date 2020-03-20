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
import kotlin.math.max

open class DataSync(
    private val activity: AppCompatActivity,
    private val authInfoHelper: AuthInfoHelper
) {

    companion object {
        const val FACTOR_OF_MAX_SUCCESSIVE_SYNC = 3
    }

    var nbToReceive = 0
    private var numberOfRequestMaxLimit = 0

    open lateinit var mediatorLiveDataList: MutableList<MediatorLiveData<GlobalStampWithTable>>

    open lateinit var globalStampObserver: Observer<GlobalStampWithTable>

    open lateinit var received: AtomicInteger

    open var maxGlobalStamp = 0

    fun setObserver(entityViewModelIsToSyncList: List<EntityViewModelIsToSync>, alreadyRefreshedTable: String?) {

        val viewModelStillInitializing = AtomicBoolean(true)
        received = AtomicInteger(0)
        val requestPerformed = AtomicInteger(0)
        val nbToReceiveForInitializing = AtomicInteger(entityViewModelIsToSyncList.size)

        val receivedSyncedTableGS = mutableListOf<GlobalStampWithTable>()

//        setupMediatorLiveData(entityViewModelIsToSyncList)
        mediatorClosure(entityViewModelIsToSyncList)

        globalStampObserver = Observer<GlobalStampWithTable> { globalStampWithTable ->
            if (!viewModelStillInitializing.get()) {

                // For a forced data synchronization, we want to ignore the observation of the
                // received globalStamp for this table. It is well saved in the viewModel, but we
                // don't want to treat its reception here.
                if (globalStampWithTable.tableName == alreadyRefreshedTable) {
                    Timber.d("[Ignoring received observable for Table : ${globalStampWithTable.tableName} with GlobalStamp : ${globalStampWithTable.globalStamp}]")
                    println("[Ignoring received observable for Table : ${globalStampWithTable.tableName} with GlobalStamp : ${globalStampWithTable.globalStamp}]")
                    return@Observer
                }

                Timber.d("[NEW] [Table : ${globalStampWithTable.tableName}, GlobalStamp : ${globalStampWithTable.globalStamp}]")
                Timber.d("Current globalStamps list :")
                println("[NEW] [Table : ${globalStampWithTable.tableName}, GlobalStamp : ${globalStampWithTable.globalStamp}]")
                println("Current globalStamps list :")

                for (entityViewModelIsToSync in entityViewModelIsToSyncList) {
                    Timber.d(" - Table : ${entityViewModelIsToSync.vm.getAssociatedTableName()}, GlobalStamp : ${entityViewModelIsToSync.vm.globalStamp.value}")
                    println(" - Table : ${entityViewModelIsToSync.vm.getAssociatedTableName()}, GlobalStamp : ${entityViewModelIsToSync.vm.globalStamp.value}")
                }

                Timber.d("[GlobalStamps received : ${received.get() + 1}/$nbToReceive]")
                println("[GlobalStamps received : ${received.get() + 1}/$nbToReceive]")

                receivedSyncedTableGS.add(globalStampWithTable)

                if (received.incrementAndGet() == nbToReceive) {

                    // Get the max globalStamp between received ones, and stored one
                    maxGlobalStamp =
                        getMaxGlobalStamp(receivedSyncedTableGS, authInfoHelper.globalStamp)
                    Timber.d("[maxGlobalStamp = $maxGlobalStamp]")
                    println("[maxGlobalStamp = $maxGlobalStamp]")

                    val isAtLeastOneToSync =
                        checkIfAtLeastOneTableToSync(maxGlobalStamp, entityViewModelIsToSyncList)

                    if (isAtLeastOneToSync) {
                        Timber.d("[There is at least one table that requires data synchronization]")
                        println("[There is at least one table that requires data synchronization]")
                        if (canPerformNewSync(
                                received,
                                requestPerformed,
                                numberOfRequestMaxLimit
                            )
                        ) {
                            sync(entityViewModelIsToSyncList)
                        } else {
                            failureClosure(entityViewModelIsToSyncList)
                        }
                    } else {
                        successClosure(maxGlobalStamp, entityViewModelIsToSyncList)
                    }
                }
            } else {
                Timber.d("[INITIALIZING] [Table : ${globalStampWithTable.tableName}, GlobalStamp : ${globalStampWithTable.globalStamp}]")
                Timber.d("[GlobalStamps received for initializing : ${received.get() + 1}/${nbToReceiveForInitializing.get()}]")
//                println("[INITIALIZING] [Table : ${globalStampWithTable.tableName}, GlobalStamp : ${globalStampWithTable.globalStamp}]")
//                println("[GlobalStamps received for initializing : ${received.get() + 1}/${nbToReceiveForInitializing.get()}]")
                if (canStartSync(received, nbToReceiveForInitializing, viewModelStillInitializing)) {
                    // first sync
                    sync(entityViewModelIsToSyncList)
                }
            }
        }

        // observe merged LiveData
        mergedLiveDataClosure(globalStampObserver)
//        observeMergedLiveData(globalStampObserver)
//        for (mediatorLiveData in mediatorLiveDataList) {
//            mediatorLiveData.observe(activity, globalStampObserver)
//        }
    }

    private var defaultMergeLiveDataClosure: (Observer<GlobalStampWithTable>) -> Unit = { globalStampObserver ->
        observeMergedLiveData(globalStampObserver)
    }

    var mergedLiveDataClosure: (Observer<GlobalStampWithTable>) -> Unit = defaultMergeLiveDataClosure

    open fun observeMergedLiveData(globalStampObserver: Observer<GlobalStampWithTable>) {
        for (mediatorLiveData in mediatorLiveDataList) {
            mediatorLiveData.observe(activity, globalStampObserver)
        }
    }

    private var defaultMediatorClosure: (List<EntityViewModelIsToSync>) -> Unit = { entityViewModelIsToSyncList ->
        setupMediatorLiveData(entityViewModelIsToSyncList)
    }

    var mediatorClosure: (List<EntityViewModelIsToSync>) -> Unit = defaultMediatorClosure

    open fun setupMediatorLiveData(entityViewModelIsToSyncList: List<EntityViewModelIsToSync>): List<MediatorLiveData<GlobalStampWithTable>> {

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
        return mediatorLiveDataList
    }

    fun canStartSync(
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

    fun canPerformNewSync(
        received: AtomicInteger,
        requestPerformed: AtomicInteger,
        numberOfRequestMaxLimit: Int
    ): Boolean {
        received.set(0)
        requestPerformed.incrementAndGet()
        return requestPerformed.get() <= numberOfRequestMaxLimit
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
        return isAtLeastOneToSync
    }

    fun getMaxGlobalStamp(
        receivedSyncedTableGS: List<GlobalStampWithTable>,
        authInfoHelperGlobalStamp: Int
    ): Int = maxOf(receivedSyncedTableGS.map { it.globalStamp }.maxBy { it } ?: 0,
        authInfoHelperGlobalStamp)

    private var defaultSuccessClosure: (Int, List<EntityViewModelIsToSync>) -> Unit = { maxGlobalStamp, entityViewModelIsToSyncList ->
        validateSynchronization(maxGlobalStamp, entityViewModelIsToSyncList)
    }

    var successClosure: (Int, List<EntityViewModelIsToSync>) -> Unit = defaultSuccessClosure

    private var defaultFailureClosure: (List<EntityViewModelIsToSync>) -> Unit = { entityViewModelIsToSyncList ->
        Timber.e("[Number of request max limit has been reached. Data synchronization is ending with tables not synchronized]")
        println("[Number of request max limit has been reached. Data synchronization is ending with tables not synchronized]")
        for (mediatorLiveData in mediatorLiveDataList) {
            mediatorLiveData.removeObservers(activity)
        }
        for (entityViewModelIsToSync in entityViewModelIsToSyncList) {
            // notify data are synced
            entityViewModelIsToSync.vm.dataSynchronized.postValue(
                DataSyncState.UNSYNCHRONIZED
            )
        }
    }

    var failureClosure: (List<EntityViewModelIsToSync>) -> Unit = defaultFailureClosure

    private fun validateSynchronization(
        maxGlobalStamp: Int,
        entityViewModelIsToSyncList: List<EntityViewModelIsToSync>
    ) {
        Timber.i("[Synchronization performed, all tables are up-to-date]")
        println("[Synchronization performed, all tables are up-to-date]")
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

    private var defaultSyncClosure: (EntityViewModelIsToSync) -> Unit = { entityViewModelIsToSync ->
        entityViewModelIsToSync.sync()
    }
    // For unit tests
    var syncClosure: (EntityViewModelIsToSync) -> Unit = defaultSyncClosure

//    @Synchronized
    private fun sync(entityViewModelIsToSyncList: List<EntityViewModelIsToSync>) {
        this.nbToReceive = entityViewModelIsToSyncList.filter { it.isToSync }.size
        this.numberOfRequestMaxLimit = nbToReceive * FACTOR_OF_MAX_SUCCESSIVE_SYNC

        val syncRequiredList = entityViewModelIsToSyncList.filter { it.isToSync }

        for (syncRequired in syncRequiredList) {
            syncClosure(syncRequired)
        }
    }
}

fun EntityViewModelIsToSync.sync() {

    this.vm.dataSynchronized.postValue(DataSyncState.SYNCHRONIZING)

    Timber.d("[Sync] [Table : ${this.vm.getAssociatedTableName()}, isToSync : ${this.isToSync}]")

    if (this.isToSync) {
        this.isToSync = false
        this.vm.getData {
            Timber.v("Requesting data for ${vm.getAssociatedTableName()}")
        }
    }
}
