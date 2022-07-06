/*
 * Created by Quentin Marciset on 31/3/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.sync

import androidx.lifecycle.LifecycleOwner
import com.qmobile.qmobileapi.utils.LoginRequiredCallback
import com.qmobile.qmobileapi.utils.SharedPreferencesHolder
import com.qmobile.qmobiledatasync.viewmodel.EntityListViewModel
import kotlinx.coroutines.CoroutineScope
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class DataSync(
    val lifecycleOwner: LifecycleOwner,
    val entityListViewModelList: List<EntityListViewModel<*>>,
    val sharedPreferencesHolder: SharedPreferencesHolder,
    private val loginRequiredCallback: LoginRequiredCallback? = null
) {

    enum class State {
        SYNCHRONIZED, UNSYNCHRONIZED, SYNCHRONIZING, RESYNC
    }

    companion object {
        const val FACTOR_OF_MAX_SUCCESSIVE_SYNC = 3
    }

    private var nbToReceive = 0
    var maxGlobalStamp = 0
    private var numberOfRequestMaxLimit = 0
    private lateinit var received: AtomicInteger
    private lateinit var requestPerformed: AtomicInteger
    private lateinit var receivedSyncedTableGS: MutableList<GlobalStamp>
    val loginRequired = AtomicBoolean(false)
    private var globalStampAlreadyObserved = false

    lateinit var globalStampObserver: suspend CoroutineScope.(value: GlobalStamp) -> Unit

    // Default closures
    lateinit var setupObservableClosure: (suspend CoroutineScope.(value: GlobalStamp) -> Unit) -> Unit
    lateinit var syncClosure: (EntityListViewModel<*>, Boolean) -> Unit
    lateinit var successfulSyncClosure: (Int) -> Unit
    lateinit var unsuccessfulSyncClosure: () -> Unit

    init {
        initClosures()
    }

    fun perform() {
        resetDataSyncVariables()

        globalStampObserver = { globalStamp: GlobalStamp ->

            val vmState =
                entityListViewModelList.find { it.getAssociatedTableName() == globalStamp.tableName }
                    ?.dataSynchronized?.value

            if (globalStamp.dataSyncProcess && vmState != State.SYNCHRONIZED) {
                Timber.d(
                    "[NEW] [Table : ${globalStamp.tableName}, " +
                        "GlobalStamp : ${globalStamp.stampValue}]"
                )

                entityListViewModelList.printGlobalStamp()

                Timber.d("[GlobalStamps received : ${received.get() + 1}/$nbToReceive]")

                receivedSyncedTableGS.add(globalStamp)

                if (received.incrementAndGet() == nbToReceive) {
                    analyzeGlobalStamps()
                }
            }
        }

        syncTables()

        if (!globalStampAlreadyObserved) {
            setupObservableClosure(globalStampObserver)
        }
        globalStampAlreadyObserved = true
    }

    private fun analyzeGlobalStamps() {
        if (loginRequired.getAndSet(false)) {
            loginRequiredCallback?.invoke()
        } else {
            // Get the max globalStamp between received ones, and stored one
            maxGlobalStamp = getMaxGlobalStamp(receivedSyncedTableGS)
            Timber.d("[maxGlobalStamp = $maxGlobalStamp]")

            val isAtLeastOneToSync = checkIfAtLeastOneTableToSync(maxGlobalStamp)

            if (isAtLeastOneToSync) {
                Timber.d("[There is at least one table that requires data synchronization]")
                if (canPerformNewSync()) {
                    syncTables(true)
                } else {
                    unsuccessfulSyncClosure()
                }
            } else {
                successfulSyncClosure(maxGlobalStamp)
            }
        }
    }

    private fun resetDataSyncVariables() {
        received = AtomicInteger(0)
        requestPerformed = AtomicInteger(0)
        receivedSyncedTableGS = mutableListOf()
    }

    private fun syncTables(reSync: Boolean = false) {
        this.nbToReceive = entityListViewModelList.filter { it.isToSync.get() }.size
        this.numberOfRequestMaxLimit = nbToReceive * FACTOR_OF_MAX_SUCCESSIVE_SYNC
        entityListViewModelList.filter { it.isToSync.get() }.forEach { syncRequired ->
            syncClosure(syncRequired, reSync)
        }
    }

    private fun getMaxGlobalStamp(
        receivedSyncedTableGS: List<GlobalStamp>
    ): Int = maxOf(
        receivedSyncedTableGS.map { it.stampValue }.maxByOrNull { it } ?: 0,
        sharedPreferencesHolder.globalStamp
    )

    private fun checkIfAtLeastOneTableToSync(
        maxGlobalStamp: Int
    ): Boolean {
        var isAtLeastOneToSync = false
        entityListViewModelList.forEach { entityListViewModel ->
            val vmGs = entityListViewModel.globalStamp.value.stampValue
            if (vmGs < maxGlobalStamp) {
                entityListViewModel.isToSync.set(true)
                isAtLeastOneToSync = true
            }
        }
        return isAtLeastOneToSync
    }

    private fun canPerformNewSync(): Boolean {
        received.set(0)
        requestPerformed.incrementAndGet()
        return requestPerformed.get() <= numberOfRequestMaxLimit
    }
}
