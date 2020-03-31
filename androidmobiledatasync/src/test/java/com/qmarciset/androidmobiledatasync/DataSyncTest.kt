/*
 * Created by Quentin Marciset on 23/3/2020.
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
import com.qmarciset.androidmobiledatasync.sync.DataSync
import com.qmarciset.androidmobiledatasync.sync.EntityViewModelIsToSync
import com.qmarciset.androidmobiledatasync.sync.GlobalStampWithTable
import com.qmarciset.androidmobiledatasync.viewmodel.EntityListViewModel
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
        const val EMPLOYEE_TABLE = "Employee"
        const val SERVICE_TABLE = "Service"
        const val OFFICE_TABLE = "Office"
    }

    private lateinit var dataSync: DataSync

    // GlobalStamp observer
    private var iteration = 1
    private val sharedPreferencesGlobalStamp = 120
    private lateinit var globalStampList: MutableList<Int>
    private lateinit var entityViewModelIsToSyncList: MutableList<EntityViewModelIsToSync>

    // MutableLiveData for mocked ViewModels
    private lateinit var sourceIntEmployee: MutableLiveData<Int>
    private lateinit var sourceIntService: MutableLiveData<Int>
    private lateinit var sourceIntOffice: MutableLiveData<Int>

    // MediatorLiveData
    private lateinit var liveDataMergerEmployee: MediatorLiveData<GlobalStampWithTable>
    private lateinit var liveDataMergerService: MediatorLiveData<GlobalStampWithTable>
    private lateinit var liveDataMergerOffice: MediatorLiveData<GlobalStampWithTable>

    // Custom closures
    private lateinit var setupObservableClosure: (List<EntityViewModelIsToSync>, Observer<GlobalStampWithTable>) -> Unit
    private lateinit var syncClosure: (EntityViewModelIsToSync) -> Unit
    private lateinit var successfulSyncClosure: (Int, List<EntityViewModelIsToSync>) -> Unit
    private lateinit var unsuccessfulSyncClosure: (List<EntityViewModelIsToSync>) -> Unit

    // Mocks
    @Mock
    lateinit var activity: AppCompatActivity
    @Mock
    lateinit var authInfoHelper: AuthInfoHelper
    @Mock
    lateinit var entityListViewModelEmployee: EntityListViewModel<Employee>
    @Mock
    lateinit var entityListViewModelService: EntityListViewModel<Service>
    @Mock
    lateinit var entityListViewModelOffice: EntityListViewModel<Office>

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)

        initObservation()
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

    @Test
    fun testDataSync() {

        dataSync = DataSync(
            activity,
            authInfoHelper
        )

        mockForDataSync()

        setupObservableClosure = { _, _ ->
            observeMergedLiveData()
        }

        syncClosure = { entityViewModelIsToSync ->
            sync(entityViewModelIsToSync)
        }

        successfulSyncClosure = { _, _ ->
            println("[Synchronization performed, all tables are up-to-date]")
            removeObservers()
            assertSuccess()
        }

        unsuccessfulSyncClosure = { _ ->
            println("[Number of request max limit has been reached. Data synchronization is ending with tables not synchronized]")
            removeObservers()
            fail()
        }

        setClosures()

        dataSync.setObserver(entityViewModelIsToSyncList, null)

        simulateLiveDataInitialization()
    }

    @Test
    fun testNumberOfRequestMaxLimit() {

        dataSync = DataSync(
            activity,
            authInfoHelper
        )

        mockForDataSync()

        setupObservableClosure = { _, _ ->
            observeMergedLiveData()
        }

        syncClosure = { entityViewModelIsToSync ->
            syncToInfiniteAndBeyond(entityViewModelIsToSync)
        }

        successfulSyncClosure = { _, _ ->
            println("[Synchronization performed, all tables are up-to-date]")
            removeObservers()
            fail()
        }

        unsuccessfulSyncClosure = { _ ->
            println("[Number of request max limit has been reached. Data synchronization is ending with tables not synchronized]")
            assertEquals(8, iteration)
            removeObservers()
        }

        setClosures()

        dataSync.setObserver(entityViewModelIsToSyncList, null)

        simulateLiveDataInitialization()
    }

    private fun initObservation() {
        // Initializing and filling EntityViewModelIsToSync list
        entityViewModelIsToSyncList = mutableListOf()

        entityViewModelIsToSyncList.add(
            EntityViewModelIsToSync(
                entityListViewModelEmployee,
                true
            )
        )
        entityViewModelIsToSyncList.add(
            EntityViewModelIsToSync(
                entityListViewModelService,
                true
            )
        )
        entityViewModelIsToSyncList.add(
            EntityViewModelIsToSync(
                entityListViewModelOffice,
                true
            )
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
        Mockito.`when`(entityListViewModelOffice.getAssociatedTableName())
            .thenReturn(OFFICE_TABLE)

        Mockito.`when`(authInfoHelper.globalStamp).thenReturn(sharedPreferencesGlobalStamp)
    }

    private fun observeMergedLiveData() {
        liveDataMergerEmployee.observeForever(dataSync.globalStampObserver)
        liveDataMergerService.observeForever(dataSync.globalStampObserver)
        liveDataMergerOffice.observeForever(dataSync.globalStampObserver)
    }

    private fun setClosures() {
        dataSync.setupObservableClosure = setupObservableClosure
        dataSync.syncClosure = syncClosure
        dataSync.successfulSyncClosure = successfulSyncClosure
        dataSync.unsuccessfulSyncClosure = unsuccessfulSyncClosure
    }

    private fun simulateLiveDataInitialization() {
        sourceIntEmployee.postValue(globalStampValue_0)
        sourceIntService.postValue(globalStampValue_0)
        sourceIntOffice.postValue(globalStampValue_0)
    }

    private fun removeObservers() {
        liveDataMergerEmployee.removeObserver(dataSync.globalStampObserver)
        liveDataMergerService.removeObserver(dataSync.globalStampObserver)
        liveDataMergerOffice.removeObserver(dataSync.globalStampObserver)
    }

    private fun sync(entityViewModelIsToSync: EntityViewModelIsToSync) {
        println("[Sync] [Table : ${entityViewModelIsToSync.vm.getAssociatedTableName()}, isToSync : ${entityViewModelIsToSync.isToSync}]")

        if (entityViewModelIsToSync.isToSync) {
            entityViewModelIsToSync.isToSync = false

            if (dataSync.received.get() == 0)
                assertLiveDataValues(iteration)

            globalStampList = when (iteration) {
                1 -> globalStampValueSet_1.toMutableList()
                2 -> globalStampValueSet_2.toMutableList()
                3 -> globalStampValueSet_3.toMutableList()
                else -> mutableListOf()
            }

            if (dataSync.received.get() + 1 == dataSync.nbToReceive) {
                iteration++
            }

            emitGlobalStamp(entityViewModelIsToSync, globalStampList)
        }
    }

    private fun syncToInfiniteAndBeyond(entityViewModelIsToSync: EntityViewModelIsToSync) {
        println("[Sync] [Table : ${entityViewModelIsToSync.vm.getAssociatedTableName()}, isToSync : ${entityViewModelIsToSync.isToSync}]")

        if (entityViewModelIsToSync.isToSync) {
            entityViewModelIsToSync.isToSync = false

            globalStampList = mutableListOf(
                dataSync.maxGlobalStamp + 1,
                dataSync.maxGlobalStamp + 2,
                dataSync.maxGlobalStamp + 3
            )

            if (dataSync.received.get() + 1 == dataSync.nbToReceive) {
                iteration++
            }

            emitGlobalStamp(entityViewModelIsToSync, globalStampList)
        }
    }

    private fun assertLiveDataValues(iteration: Int) {
        when (iteration) {
            1 -> {
                assertEquals(globalStampValue_0, entityListViewModelEmployee.globalStamp.value)
                assertEquals(globalStampValue_0, entityListViewModelService.globalStamp.value)
                assertEquals(globalStampValue_0, entityListViewModelOffice.globalStamp.value)
            }
            2 -> {
                assertEquals(
                    globalStampValueSet_1[0],
                    entityListViewModelEmployee.globalStamp.value
                )
                assertEquals(globalStampValueSet_1[1], entityListViewModelService.globalStamp.value)
                assertEquals(globalStampValueSet_1[2], entityListViewModelOffice.globalStamp.value)
            }
            3 -> {
                assertEquals(
                    globalStampValueSet_2[0],
                    entityListViewModelEmployee.globalStamp.value
                )
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
                println(" -> Table $EMPLOYEE_TABLE, emitting value ${globalStampList[0]}")
                sourceIntEmployee.postValue(globalStampList[0])
            }
            SERVICE_TABLE -> {
                println(" -> Table $SERVICE_TABLE, emitting value ${globalStampList[1]}")
                sourceIntService.postValue(globalStampList[1])
            }
            OFFICE_TABLE -> {
                println(" -> Table $OFFICE_TABLE, emitting value ${globalStampList[2]}")
                sourceIntOffice.postValue(globalStampList[2])
            }
        }
    }
}
