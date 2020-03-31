/*
 * Created by Quentin Marciset on 31/3/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmarciset.androidmobiledatasync.sync

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

object DataSyncUtils {

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
}
