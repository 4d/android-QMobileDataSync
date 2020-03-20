/*
 * Created by Quentin Marciset on 20/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmarciset.androidmobiledatasync

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.qmarciset.androidmobileapi.auth.AuthInfoHelper
import com.qmarciset.androidmobiledatasync.model.EntityViewModelIsToSync
import com.qmarciset.androidmobiledatasync.model.GlobalStampWithTable
import com.qmarciset.androidmobiledatasync.viewmodel.EntityListViewModel
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class DataSyncTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    companion object {
        const val FACTOR_OF_MAX_SUCCESSIVE_SYNC = 3

        const val EMPLOYEE_TABLE = "Employee"
        const val SERVICE_TABLE = "Service"
        const val OFFICE_TABLE = "Office"
    }

    @Mock
    lateinit var entityListViewModelEmployee: EntityListViewModel<Employee>
    @Mock
    lateinit var entityListViewModelService: EntityListViewModel<Service>
    @Mock
    lateinit var entityListViewModelOffice: EntityListViewModel<Office>
    @Mock
    lateinit var dataSync: DataSync
    @Mock
    lateinit var authInfoHelper: AuthInfoHelper

    private lateinit var dataSyncAlt: DataSync

    @Mock
    lateinit var activity: AppCompatActivity


    var iteration = 1

    // MutableLiveData for mocked ViewModels
    private lateinit var sourceIntEmployee: MutableLiveData<Int>
    private lateinit var sourceIntService: MutableLiveData<Int>
    private lateinit var sourceIntOffice: MutableLiveData<Int>

    // MediatorLiveData
    private lateinit var liveDataMergerEmployee: MediatorLiveData<GlobalStampWithTable>
    private lateinit var liveDataMergerService: MediatorLiveData<GlobalStampWithTable>
    private lateinit var liveDataMergerOffice: MediatorLiveData<GlobalStampWithTable>

    // GlobalStamp observer
    private lateinit var globalStampObserver: Observer<GlobalStampWithTable>
    //    private var nbToReceive = 0
    private val sharedPreferencesGlobalStamp = 120
    private lateinit var entityViewModelIsToSyncList: MutableList<EntityViewModelIsToSync>

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)

        // Initializing and filling EntityViewModelIsToSync list
        entityViewModelIsToSyncList = mutableListOf()

        entityViewModelIsToSyncList.add(
            EntityViewModelIsToSync(entityListViewModelEmployee, true)
        )
        entityViewModelIsToSyncList.add(
            EntityViewModelIsToSync(entityListViewModelService, true)
        )
        entityViewModelIsToSyncList.add(
            EntityViewModelIsToSync(entityListViewModelOffice, true)
        )

        sourceIntEmployee = MutableLiveData()
        sourceIntService = MutableLiveData()
        sourceIntOffice = MutableLiveData()

        // Initializing MediatorLiveData
        liveDataMergerEmployee = MediatorLiveData()
        liveDataMergerService = MediatorLiveData()
        liveDataMergerOffice = MediatorLiveData()

        // Pairing every globalStamp emitted with its tableName
        liveDataMergerEmployee.addSource(sourceIntEmployee) {
            if (it != null) {
                liveDataMergerEmployee.value =
                    GlobalStampWithTable(
                        EMPLOYEE_TABLE,
                        it
                    )
            }
        }
        liveDataMergerService.addSource(sourceIntService) {
            if (it != null) {
                liveDataMergerService.value =
                    GlobalStampWithTable(
                        SERVICE_TABLE,
                        it
                    )
            }
        }
        liveDataMergerOffice.addSource(sourceIntOffice) {
            if (it != null) {
                liveDataMergerOffice.value =
                    GlobalStampWithTable(
                        OFFICE_TABLE,
                        it
                    )
            }
        }

        liveDataMergerEmployee.observeForever {}
        liveDataMergerService.observeForever {}
        liveDataMergerOffice.observeForever {}
    }

    @Test
    fun testMediatorLiveData() {

        val testValueSet = listOf(123, 456, 789)

        sourceIntEmployee.value = testValueSet[0]
        sourceIntService.value = testValueSet[1]
        sourceIntOffice.value = testValueSet[2]

        assertEquals(testValueSet[0], liveDataMergerEmployee.value?.globalStamp)
        assertEquals(EMPLOYEE_TABLE, liveDataMergerEmployee.value?.tableName)

        assertEquals(testValueSet[1], liveDataMergerService.value?.globalStamp)
        assertEquals(SERVICE_TABLE, liveDataMergerService.value?.tableName)

        assertEquals(testValueSet[2], liveDataMergerOffice.value?.globalStamp)
        assertEquals(OFFICE_TABLE, liveDataMergerOffice.value?.tableName)
    }

//    @Test
    fun testDataSyncCoreObserver() {

        mockForDataSync()

        // We use multiple Atomic variables to avoid their values to be changed by another test

        val viewModelStillInitializing = AtomicBoolean(true)
        val requestPerformed = AtomicInteger(0)
        val received = AtomicInteger(0)
        val nbToReceive = AtomicInteger(entityViewModelIsToSyncList.filter { it.isToSync }.size)
        val nbToReceiveForInitializing = AtomicInteger(entityViewModelIsToSyncList.size)
        val receivedSyncedTableGS = mutableListOf<GlobalStampWithTable>()
        val numberOfRequestMaxLimit = AtomicInteger(0)

        assertEquals(3, nbToReceive.get())

        globalStampObserver = Observer { globalStampWithTable ->

            if (!viewModelStillInitializing.get()) {

                println("[NEW] [Table : ${globalStampWithTable.tableName}, GlobalStamp : ${globalStampWithTable.globalStamp}]")
                println("Current globalStamps list :")

                for (entityViewModelIsToSync in entityViewModelIsToSyncList)
                    println(" - Table : ${entityViewModelIsToSync.vm.getAssociatedTableName()}, GlobalStamp : ${entityViewModelIsToSync.vm.globalStamp.value}")

                println("[GlobalStamps received : ${received.get() + 1}/${nbToReceive.get()}]")

                receivedSyncedTableGS.add(globalStampWithTable)

                if (received.incrementAndGet() == nbToReceive.get()) {

                    val maxGlobalStamp = dataSync.getMaxGlobalStamp(
                        receivedSyncedTableGS,
                        authInfoHelper.globalStamp
                    )
                    println("[maxGlobalStamp = $maxGlobalStamp]")

                    val isAtLeastOneToSync = dataSync.checkIfAtLeastOneTableToSync(
                        maxGlobalStamp,
                        entityViewModelIsToSyncList
                    )

                    if (isAtLeastOneToSync) {
                        println("[There is at least one table that requires data synchronization]")
                        if (dataSync.canPerformNewSync(
                                received,
                                requestPerformed,
                                numberOfRequestMaxLimit.get()
                            )
                        ) {
                            println("[requestPerformed : $requestPerformed]")
                            if (requestPerformed.get() == 1) {
                                sync(2, nbToReceive, numberOfRequestMaxLimit)
                            } else {
                                sync(3, nbToReceive, numberOfRequestMaxLimit)
                            }
                        } else {
                            println("[Number of request max limit has been reached. Data synchronization is ending with tables not synchronized]")
                            fail()
                        }
                    } else {
                        println("[Synchronization performed, all tables are up-to-date]")
                        assertSuccess()
                    }
                }
            } else {
                println("[INITIALIZING] [Table : ${globalStampWithTable.tableName}, GlobalStamp : ${globalStampWithTable.globalStamp}]")
                println("[GlobalStamps received for initializing : ${received.get() + 1}/${nbToReceiveForInitializing.get()}]")
                if (dataSync.canStartSync(
                        received,
                        nbToReceiveForInitializing,
                        viewModelStillInitializing
                    )
                ) {
                    // first sync
                    sync(1, nbToReceive, numberOfRequestMaxLimit)
                }
            }
        }

        liveDataMergerEmployee.observeForever(globalStampObserver)
        liveDataMergerService.observeForever(globalStampObserver)
        liveDataMergerOffice.observeForever(globalStampObserver)

        // Simulates LiveData initialization
        sourceIntEmployee.postValue(0)
        sourceIntService.postValue(0)
        sourceIntOffice.postValue(0)
    }

//    @Test
    fun testNumberOfRequestMaxLimit() {

        mockForDataSync()

        // We use multiple Atomic variables to avoid their values to be changed by another test

        val viewModelStillInitializing = AtomicBoolean(true)
        val requestPerformed = AtomicInteger(0)
        val received = AtomicInteger(0)
        val nbToReceive = AtomicInteger(entityViewModelIsToSyncList.filter { it.isToSync }.size)
        val nbToReceiveForInitializing = AtomicInteger(entityViewModelIsToSyncList.size)
        val receivedSyncedTableGS = mutableListOf<GlobalStampWithTable>()
        val numberOfRequestMaxLimit = AtomicInteger(0)

        assertEquals(3, nbToReceive.get())

        globalStampObserver = Observer { globalStampWithTable ->

            if (!viewModelStillInitializing.get()) {

                println("[NEW] [Table : ${globalStampWithTable.tableName}, GlobalStamp : ${globalStampWithTable.globalStamp}]")
                println("Current globalStamps list :")

                for (entityViewModelIsToSync in entityViewModelIsToSyncList)
                    println(" - Table : ${entityViewModelIsToSync.vm.getAssociatedTableName()}, GlobalStamp : ${entityViewModelIsToSync.vm.globalStamp.value}")

                println("[GlobalStamps received : ${received.get() + 1}/$nbToReceive]")

                receivedSyncedTableGS.add(globalStampWithTable)

                if (received.incrementAndGet() == nbToReceive.get()) {

                    val maxGlobalStamp = dataSync.getMaxGlobalStamp(
                        receivedSyncedTableGS,
                        authInfoHelper.globalStamp
                    )
                    println("[maxGlobalStamp = $maxGlobalStamp]")

                    val isAtLeastOneToSync = dataSync.checkIfAtLeastOneTableToSync(
                        maxGlobalStamp,
                        entityViewModelIsToSyncList
                    )

                    if (isAtLeastOneToSync) {
                        println("[There is at least one table that requires data synchronization]")
                        if (dataSync.canPerformNewSync(
                                received,
                                requestPerformed,
                                numberOfRequestMaxLimit.get()
                            )
                        ) {
                            println("[requestPerformed : $requestPerformed]")
                            syncToInfiniteAndBeyond(nbToReceive, maxGlobalStamp, numberOfRequestMaxLimit)
                        } else {
                            println("[Number of request max limit has been reached. Data synchronization is ending with tables not synchronized]")
                        }
                    } else {
                        println("[Synchronization performed, all tables are up-to-date]")
                        fail()
                    }
                }
            } else {
                println("[INITIALIZING] [Table : ${globalStampWithTable.tableName}, GlobalStamp : ${globalStampWithTable.globalStamp}]")
                println("[GlobalStamps received for initializing : ${received.get() + 1}/${nbToReceiveForInitializing.get()}]")
                if (dataSync.canStartSync(
                        received,
                        nbToReceiveForInitializing,
                        viewModelStillInitializing
                    )
                ) {
                    // first sync
                    syncToInfiniteAndBeyond(nbToReceive, authInfoHelper.globalStamp, numberOfRequestMaxLimit)
                }
            }
        }

        liveDataMergerEmployee.observeForever(globalStampObserver)
        liveDataMergerService.observeForever(globalStampObserver)
        liveDataMergerOffice.observeForever(globalStampObserver)

        // Simulates LiveData initialization
        sourceIntEmployee.postValue(0)
        sourceIntService.postValue(0)
        sourceIntOffice.postValue(0)
    }

    @Test
    fun testDataSyncCoreObserverTwo() {

        dataSyncAlt = DataSync(activity, authInfoHelper)

        mockForDataSync()

        val mediatorClosure: (List<EntityViewModelIsToSync>) -> Unit = { }

        val syncClosure: (EntityViewModelIsToSync) -> Unit = { entityViewModelIsToSync ->
            reducedSync(entityViewModelIsToSync)
        }

        val mergedLiveDataClosure: (Observer<GlobalStampWithTable>) -> Unit = { globalStampObserver ->
            liveDataMergerEmployee.observeForever(dataSyncAlt.globalStampObserver)
            liveDataMergerService.observeForever(dataSyncAlt.globalStampObserver)
            liveDataMergerOffice.observeForever(dataSyncAlt.globalStampObserver)
        }

        val successClosure: (Int, List<EntityViewModelIsToSync>) -> Unit = { maxGlobalStamp, entityViewModelIsToSyncList ->
            println("[Synchronization performed, all tables are up-to-date]")
            removeObservers()
            assertSuccess()
        }

        val failureClosure: (List<EntityViewModelIsToSync>) -> Unit = { entityViewModelIsToSyncList ->
            println("[Number of request max limit has been reached. Data synchronization is ending with tables not synchronized]")
            removeObservers()
            fail()
        }

        dataSyncAlt.mediatorClosure = mediatorClosure
        dataSyncAlt.syncClosure = syncClosure
        dataSyncAlt.mergedLiveDataClosure = mergedLiveDataClosure
        dataSyncAlt.successClosure = successClosure
        dataSyncAlt.failureClosure = failureClosure

        dataSyncAlt.setObserver(entityViewModelIsToSyncList, null)

        // Simulates LiveData initialization
        sourceIntEmployee.postValue(globalStampValue_0)
        sourceIntService.postValue(globalStampValue_0)
        sourceIntOffice.postValue(globalStampValue_0)
    }

    @Test
    fun testNumberOfRequestMaxLimitTwo() {

        dataSyncAlt = DataSync(activity, authInfoHelper)

        mockForDataSync()

        val mediatorClosure: (List<EntityViewModelIsToSync>) -> Unit = { }

        val syncClosure: (EntityViewModelIsToSync) -> Unit = { entityViewModelIsToSync ->
            reducedSyncToInfiniteAndBeyond(entityViewModelIsToSync)
        }

        val mergedLiveDataClosure: (Observer<GlobalStampWithTable>) -> Unit = { globalStampObserver ->
            liveDataMergerEmployee.observeForever(dataSyncAlt.globalStampObserver)
            liveDataMergerService.observeForever(dataSyncAlt.globalStampObserver)
            liveDataMergerOffice.observeForever(dataSyncAlt.globalStampObserver)
        }

        val successClosure: (Int, List<EntityViewModelIsToSync>) -> Unit = { maxGlobalStamp, entityViewModelIsToSyncList ->
            println("[Synchronization performed, all tables are up-to-date]")
            removeObservers()
            fail()
        }

        val failureClosure: (List<EntityViewModelIsToSync>) -> Unit = { entityViewModelIsToSyncList ->
            println("[Number of request max limit has been reached. Data synchronization is ending with tables not synchronized]")
            removeObservers()
        }

        dataSyncAlt.mediatorClosure = mediatorClosure
        dataSyncAlt.syncClosure = syncClosure
        dataSyncAlt.mergedLiveDataClosure = mergedLiveDataClosure
        dataSyncAlt.successClosure = successClosure
        dataSyncAlt.failureClosure = failureClosure

        dataSyncAlt.setObserver(entityViewModelIsToSyncList, null)

        // Simulates LiveData initialization
        sourceIntEmployee.postValue(globalStampValue_0)
        sourceIntService.postValue(globalStampValue_0)
        sourceIntOffice.postValue(globalStampValue_0)
    }

    lateinit var globalStampList: MutableList<Int>

    private fun removeObservers() {
        liveDataMergerEmployee.removeObserver(dataSyncAlt.globalStampObserver)
        liveDataMergerService.removeObserver(dataSyncAlt.globalStampObserver)
        liveDataMergerOffice.removeObserver(dataSyncAlt.globalStampObserver)
    }

    private fun reducedSync(entityViewModelIsToSync: EntityViewModelIsToSync) {
        println("[reducedSync] [Table : ${entityViewModelIsToSync.vm.getAssociatedTableName()}, isToSync : ${entityViewModelIsToSync.isToSync}]")

        if (entityViewModelIsToSync.isToSync) {
            entityViewModelIsToSync.isToSync = false

            if (dataSyncAlt.received.get() == 0)
                assertLiveData(iteration)

            globalStampList = when (iteration) {
                1 -> globalStampValueSet_1.toMutableList()
                2 -> globalStampValueSet_2.toMutableList()
                3 -> globalStampValueSet_3.toMutableList()
                else -> mutableListOf()
            }

            if (dataSyncAlt.received.get() + 1 == dataSyncAlt.nbToReceive) {
                iteration++
            }

            emitGlobalStamp(entityViewModelIsToSync, globalStampList)
        }
    }

    private fun reducedSyncToInfiniteAndBeyond(entityViewModelIsToSync: EntityViewModelIsToSync) {
        println("[reducedSync] [Table : ${entityViewModelIsToSync.vm.getAssociatedTableName()}, isToSync : ${entityViewModelIsToSync.isToSync}]")

        if (entityViewModelIsToSync.isToSync) {
            entityViewModelIsToSync.isToSync = false

            globalStampList = mutableListOf(dataSyncAlt.maxGlobalStamp + 1, dataSyncAlt.maxGlobalStamp + 2, dataSyncAlt.maxGlobalStamp + 3)

            if (dataSyncAlt.received.get() + 1 == dataSyncAlt.nbToReceive) {
                iteration++
            }

            emitGlobalStamp(entityViewModelIsToSync, globalStampList)
        }
    }

    private fun sync(iteration: Int, nbToReceive: AtomicInteger, numberOfRequestMaxLimit: AtomicInteger) {

        nbToReceive.set(entityViewModelIsToSyncList.filter { it.isToSync }.size)
        numberOfRequestMaxLimit.set(nbToReceive.get() * FACTOR_OF_MAX_SUCCESSIVE_SYNC)

        assertLiveData(iteration)

        var globalStampList: MutableList<Int>

        for (entityViewModelIsToSync in entityViewModelIsToSyncList) {

            println("[Sync] [Table : ${entityViewModelIsToSync.vm.getAssociatedTableName()}, isToSync : ${entityViewModelIsToSync.isToSync}]")

            if (entityViewModelIsToSync.isToSync) {
                entityViewModelIsToSync.isToSync = false

                globalStampList = when (iteration) {
                    1 -> globalStampValueSet_1.toMutableList()
                    2 -> globalStampValueSet_2.toMutableList()
                    3 -> globalStampValueSet_3.toMutableList()
                    else -> mutableListOf()
                }
                emitGlobalStamp(entityViewModelIsToSync, globalStampList)
            }
        }
    }

    private fun syncToInfiniteAndBeyond(nbToReceive: AtomicInteger, maxGlobalStamp: Int, numberOfRequestMaxLimit: AtomicInteger) {
        nbToReceive.set(entityViewModelIsToSyncList.filter { it.isToSync }.size)
        numberOfRequestMaxLimit.set(nbToReceive.get() * FACTOR_OF_MAX_SUCCESSIVE_SYNC)

        var globalStampList: MutableList<Int>

        for (entityViewModelIsToSync in entityViewModelIsToSyncList) {

            println("[Sync] [Table : ${entityViewModelIsToSync.vm.getAssociatedTableName()}, isToSync : ${entityViewModelIsToSync.isToSync}]")

            if (entityViewModelIsToSync.isToSync) {
                entityViewModelIsToSync.isToSync = false

                globalStampList =
                    mutableListOf(maxGlobalStamp + 1, maxGlobalStamp + 2, maxGlobalStamp + 3)
                emitGlobalStamp(entityViewModelIsToSync, globalStampList)
            }
        }
    }

    private fun assertLiveData(iteration: Int) {
        when (iteration) {
            1 -> {
                assertEquals(globalStampValue_0, entityListViewModelEmployee.globalStamp.value)
                assertEquals(globalStampValue_0, entityListViewModelService.globalStamp.value)
                assertEquals(globalStampValue_0, entityListViewModelOffice.globalStamp.value)
            }
            2 -> {
                assertEquals(globalStampValueSet_1[0], entityListViewModelEmployee.globalStamp.value)
                assertEquals(globalStampValueSet_1[1], entityListViewModelService.globalStamp.value)
                assertEquals(globalStampValueSet_1[2], entityListViewModelOffice.globalStamp.value)
            }
            3 -> {
                assertEquals(globalStampValueSet_2[0], entityListViewModelEmployee.globalStamp.value)
                assertEquals(globalStampValueSet_2[1], entityListViewModelService.globalStamp.value)
                assertEquals(globalStampValueSet_2[2], entityListViewModelOffice.globalStamp.value)
            }
        }
    }

    private fun assertSuccess() {
        assertEquals(globalStampValueSet_3[0], entityListViewModelEmployee.globalStamp.value)
        assertEquals(globalStampValueSet_3[1], entityListViewModelService.globalStamp.value)
        assertEquals(globalStampValueSet_3[2], entityListViewModelOffice.globalStamp.value)
    }

    private fun emitGlobalStamp(
        entityViewModelIsToSync: EntityViewModelIsToSync,
        globalStampList: List<Int>
    ) {
        when (entityViewModelIsToSync.vm.getAssociatedTableName()) {
            EMPLOYEE_TABLE -> {
                println(" -> Table Employee, emitting value ${globalStampList[0]}")
                sourceIntEmployee.postValue(globalStampList[0])
            }
            SERVICE_TABLE -> {
                println(" -> Table Service, emitting value ${globalStampList[1]}")
                sourceIntService.postValue(globalStampList[1])
            }
            OFFICE_TABLE -> {
                println(" -> Table Office, emitting value ${globalStampList[2]}")
                sourceIntOffice.postValue(globalStampList[2])
            }
        }
    }

    private fun mockForDataSync() {
        // Mock ViewModel globalStamp's LiveData
        Mockito.`when`(entityListViewModelEmployee.globalStamp).thenReturn(sourceIntEmployee)
        Mockito.`when`(entityListViewModelService.globalStamp).thenReturn(sourceIntService)
        Mockito.`when`(entityListViewModelOffice.globalStamp).thenReturn(sourceIntOffice)

        // Mock ViewModel tableName
        Mockito.`when`(entityListViewModelEmployee.getAssociatedTableName())
            .thenReturn(EMPLOYEE_TABLE)
        Mockito.`when`(entityListViewModelService.getAssociatedTableName())
            .thenReturn(SERVICE_TABLE)
        Mockito.`when`(entityListViewModelOffice.getAssociatedTableName()).thenReturn(OFFICE_TABLE)

        Mockito.`when`(authInfoHelper.globalStamp).thenReturn(sharedPreferencesGlobalStamp)
    }
}
