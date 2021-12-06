/*
 * Created by Quentin Marciset on 31/3/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.sync

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer
import com.qmobile.qmobileapi.utils.LoginRequiredCallback
import com.qmobile.qmobileapi.utils.SharedPreferencesHolder
import com.qmobile.qmobiledatasync.utils.collectWhenStarted
import com.qmobile.qmobiledatasync.viewmodel.EntityListViewModel
import kotlinx.coroutines.flow.StateFlow
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
    val loginRequired = AtomicBoolean(false)

    lateinit var globalStampObserver: suspend (value: GlobalStamp) -> Unit

//    lateinit var mediatorLiveDataList: MutableList<MediatorLiveData<GlobalStamp>>

    // Default closures
    lateinit var setupObservableClosure: (List<EntityListViewModel<*>>, suspend (value: GlobalStamp) -> Unit) -> Unit
    lateinit var syncClosure: (EntityListViewModel<*>) -> Unit
    lateinit var successfulSyncClosure: (Int, List<EntityListViewModel<*>>) -> Unit
    lateinit var unsuccessfulSyncClosure: (List<EntityListViewModel<*>>) -> Unit

    init {
        initClosures()
    }

    @Suppress("LongMethod")
    fun setObserver(
        entityListViewModelList: List<EntityListViewModel<*>>,
        alreadyRefreshedTable: String?
    ) {

        received = AtomicInteger(0)
        val viewModelStillInitializing = AtomicBoolean(true)
        val requestPerformed = AtomicInteger(0)
        val nbToReceiveForInitializing = AtomicInteger(entityListViewModelList.size)
        val receivedSyncedTableGS = mutableListOf<GlobalStamp>()

        globalStampObserver = { globalStamp: GlobalStamp ->
//            if (!viewModelStillInitializing.get()) {

                // For a forced data synchronization, we want to ignore the observation of the
                // received globalStamp for this table. It is well saved in the viewModel, but we
                // don't want to treat its reception here.
                if (globalStamp.tableName == alreadyRefreshedTable) {
                    Timber.d(
                        "[Ignoring received observable for Table : " +
                                "${globalStamp.tableName} with GlobalStamp : " +
                                "${globalStamp.stampValue}]"
                    )
                } else {

                    Timber.d(
                        "[NEW] [Table : ${globalStamp.tableName}, " +
                                "GlobalStamp : ${globalStamp.stampValue}]"
                    )
                    Timber.d("Current globalStamps list :")

                    entityListViewModelList.forEach { entityListViewModel ->
                        Timber.d(
                            " - Table : ${entityListViewModel.getAssociatedTableName()}, " +
                                    "GlobalStamp : ${entityListViewModel.globalStamp.value.stampValue}"
                        )
                    }

                    Timber.d("[GlobalStamps received : ${received.get() + 1}/$nbToReceive]")

                    receivedSyncedTableGS.add(globalStamp)

                    if (received.incrementAndGet() == nbToReceive) {

                        if (loginRequired.getAndSet(false)) {
                            loginRequiredCallback?.invoke()
                        } else {

                            // Get the max globalStamp between received ones, and stored one
                            maxGlobalStamp =
                                DataSyncUtils.getMaxGlobalStamp(
                                    receivedSyncedTableGS,
                                    sharedPreferencesHolder.globalStamp
                                )
                            Timber.d("[maxGlobalStamp = $maxGlobalStamp]")

                            val isAtLeastOneToSync =
                                DataSyncUtils.checkIfAtLeastOneTableToSync(
                                    maxGlobalStamp,
                                    entityListViewModelList
                                )

                            if (isAtLeastOneToSync) {
                                Timber.d("[There is at least one table that requires data synchronization]")
                                if (DataSyncUtils.canPerformNewSync(
                                        received,
                                        requestPerformed,
                                        numberOfRequestMaxLimit
                                    )
                                ) {
                                    syncTables(entityListViewModelList)
                                } else {
                                    unsuccessfulSyncClosure(entityListViewModelList)
                                }
                            } else {
                                successfulSyncClosure(maxGlobalStamp, entityListViewModelList)
                            }
                        }
                    }
                }
//            } else {
//                Timber.d(
//                    "[INITIALIZING] [Table : ${globalStamp.tableName}, " +
//                            "GlobalStamp : ${globalStamp.stampValue}]"
//                )
//                Timber.d(
//                    "[GlobalStamps received for initializing : " +
//                            "${received.get() + 1}/${nbToReceiveForInitializing.get()}]"
//                )
//                if (DataSyncUtils.canStartSync(received, nbToReceiveForInitializing, viewModelStillInitializing)) {
//                    // first sync
//                        Timber.d("syncTables")
//                    syncTables(entityListViewModelList)
//                }
//            }
        }

//        setupObservableClosure(entityListViewModelList, globalStampObserver)
//        entityListViewModelList.map { it.globalStamp }.forEach { stateFlow ->
//            lifecycleOwner.collectWhenStarted(flow = stateFlow, action = globalStampObserver)
//        }

        Timber.d("syncTables")
        syncTables(entityListViewModelList)
    }

    fun observe(entityListViewModel: EntityListViewModel<*>) {
        lifecycleOwner.collectWhenStarted(flow = entityListViewModel.globalStamp, action = globalStampObserver)
    }
    fun unObserve(entityListViewModel: EntityListViewModel<*>) {
//        lifecycleOwner.collectWhenStarted(flow = entityListViewModel.globalStamp, action = {})
    }

    //    @Synchronized + isToSync: AtomicBoolean ?
    private fun syncTables(entityListViewModelList: List<EntityListViewModel<*>>) {
        this.nbToReceive = entityListViewModelList.filter { it.isToSync.get() }.size
        this.numberOfRequestMaxLimit = nbToReceive * FACTOR_OF_MAX_SUCCESSIVE_SYNC
        entityListViewModelList.filter { it.isToSync.get() }.forEach { syncRequired ->
            syncClosure(syncRequired)
        }
    }
}
