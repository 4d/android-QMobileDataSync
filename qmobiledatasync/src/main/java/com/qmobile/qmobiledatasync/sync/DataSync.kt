/*
 * Created by Quentin Marciset on 31/3/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.sync

import android.annotation.SuppressLint
import androidx.lifecycle.LifecycleOwner
import com.qmobile.qmobileapi.utils.LoginRequiredCallback
import com.qmobile.qmobileapi.utils.SharedPreferencesHolder
import com.qmobile.qmobiledatasync.viewmodel.EntityListViewModel
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

@SuppressLint("BinaryOperationInTimber")
class DataSync(
    val lifecycleOwner: LifecycleOwner,
    val sharedPreferencesHolder: SharedPreferencesHolder,
    private val loginRequiredCallback: LoginRequiredCallback? = null
) {

    companion object {
        const val FACTOR_OF_MAX_SUCCESSIVE_SYNC = 3
    }

    var nbToReceive = 0
    var maxGlobalStamp = 0
    private var numberOfRequestMaxLimit = 0
    lateinit var received: AtomicInteger
    private lateinit var requestPerformed: AtomicInteger
    private lateinit var receivedSyncedTableGS: MutableList<GlobalStamp>
    val loginRequired = AtomicBoolean(false)
    private var globalStampAlreadyObserved = false

    lateinit var globalStampObserver: suspend (value: GlobalStamp) -> Unit

    // Default closures
    lateinit var setupObservableClosure: (List<EntityListViewModel<*>>, suspend (value: GlobalStamp) -> Unit) -> Unit
    lateinit var syncClosure: (EntityListViewModel<*>, Boolean) -> Unit
    lateinit var successfulSyncClosure: (Int, List<EntityListViewModel<*>>) -> Unit
    lateinit var unsuccessfulSyncClosure: (List<EntityListViewModel<*>>) -> Unit

    init {
        initClosures()
    }

    @Suppress("LongMethod")
    fun setObserver(
        entityListViewModelList: List<EntityListViewModel<*>>
    ) {
        resetDataSyncVariables()

        globalStampObserver = { globalStamp: GlobalStamp ->

            val vmState =
                entityListViewModelList.find { it.getAssociatedTableName() == globalStamp.tableName }
                    ?.dataSynchronized?.value

            if (globalStamp.dataSyncProcess && vmState != DataSyncStateEnum.SYNCHRONIZED) {

                Timber.d(
                    "[NEW] [Table : ${globalStamp.tableName}, " +
                        "GlobalStamp : ${globalStamp.stampValue}]"
                )

                entityListViewModelList.printGlobalStamp()

                Timber.d("[GlobalStamps received : ${received.get() + 1}/$nbToReceive]")

                receivedSyncedTableGS.add(globalStamp)

                if (received.incrementAndGet() == nbToReceive) {

                    if (loginRequired.getAndSet(false)) {
                        loginRequiredCallback?.invoke()
                    } else {

                        // Get the max globalStamp between received ones, and stored one
                        maxGlobalStamp = getMaxGlobalStamp(receivedSyncedTableGS)
                        Timber.d("[maxGlobalStamp = $maxGlobalStamp]")

                        val isAtLeastOneToSync = checkIfAtLeastOneTableToSync(maxGlobalStamp, entityListViewModelList)

                        if (isAtLeastOneToSync) {
                            Timber.d("[There is at least one table that requires data synchronization]")
                            if (canPerformNewSync()) {
                                syncTables(entityListViewModelList, true)
                            } else {
                                unsuccessfulSyncClosure(entityListViewModelList)
                            }
                        } else {
                            successfulSyncClosure(maxGlobalStamp, entityListViewModelList)
                        }
                    }
                }
            }
        }

        syncTables(entityListViewModelList)

        if (!globalStampAlreadyObserved) {
            setupObservableClosure(entityListViewModelList, globalStampObserver)
        }
        globalStampAlreadyObserved = true
    }

    private fun resetDataSyncVariables() {
        received = AtomicInteger(0)
        requestPerformed = AtomicInteger(0)
        receivedSyncedTableGS = mutableListOf()
    }

    private fun syncTables(
        entityListViewModelList: List<EntityListViewModel<*>>,
        reSync: Boolean = false
    ) {
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
        maxGlobalStamp: Int,
        entityListViewModelList: List<EntityListViewModel<*>>
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
