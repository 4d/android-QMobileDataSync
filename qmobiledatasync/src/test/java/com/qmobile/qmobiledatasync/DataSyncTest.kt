/*
 * Created by Quentin Marciset on 23/3/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.qmobile.qmobileapi.utils.SharedPreferencesHolder
import com.qmobile.qmobiledatasync.sync.DataSync
import com.qmobile.qmobiledatasync.sync.GlobalStampWithTable
import com.qmobile.qmobiledatasync.sync.resetIsToSync
import com.qmobile.qmobiledatasync.viewmodel.EntityListViewModel
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
    private lateinit var entityListViewModelList: MutableList<EntityListViewModel<*>>

    // MutableLiveData for mocked ViewModels
    private val _sourceIntEmployee = MutableLiveData<Int>()
    private val sourceIntEmployee: LiveData<Int> = _sourceIntEmployee
    private val _sourceIntService = MutableLiveData<Int>()
    private val sourceIntService: LiveData<Int> = _sourceIntService
    private val _sourceIntOffice = MutableLiveData<Int>()
    private val sourceIntOffice: LiveData<Int> = _sourceIntOffice

    // MediatorLiveData
    private lateinit var liveDataMergerEmployee: MediatorLiveData<GlobalStampWithTable>
    private lateinit var liveDataMergerService: MediatorLiveData<GlobalStampWithTable>
    private lateinit var liveDataMergerOffice: MediatorLiveData<GlobalStampWithTable>

    // Custom closures
    private lateinit var setupObservableClosure: (List<EntityListViewModel<*>>, Observer<GlobalStampWithTable>) -> Unit
    private lateinit var syncClosure: (EntityListViewModel<*>) -> Unit
    private lateinit var successfulSyncClosure: (Int, List<EntityListViewModel<*>>) -> Unit
    private lateinit var unsuccessfulSyncClosure: (List<EntityListViewModel<*>>) -> Unit

    // Mocks
    @Mock
    lateinit var activity: AppCompatActivity

    @Mock
    lateinit var sharedPreferencesHolder: SharedPreferencesHolder

    @Mock
    lateinit var entityListViewModelEmployee: EntityListViewModel<Employee>

    @Mock
    lateinit var entityListViewModelService: EntityListViewModel<Service>

    @Mock
    lateinit var entityListViewModelOffice: EntityListViewModel<Office>

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        initObservation()
    }

    @Test
    fun testMediatorLiveData() {

        val testValueSet = listOf(123, 456, 789)

        _sourceIntEmployee.value = testValueSet[0]
        _sourceIntService.value = testValueSet[1]
        _sourceIntOffice.value = testValueSet[2]

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
            sharedPreferencesHolder
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

        dataSync.setObserver(entityListViewModelList, null)

        simulateLiveDataInitialization()
    }

    @Test
    fun testNumberOfRequestMaxLimit() {

        dataSync = DataSync(
            activity,
            sharedPreferencesHolder
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

        dataSync.setObserver(entityListViewModelList, null)

        simulateLiveDataInitialization()
    }

    /*@Test
    fun testDecodeDeletedRecords() = runBlocking {

        mockForDataSync()

        entityListViewModelEmployee.decodeDeletedRecords(sampleDeletedRecord.getSafeArray("__ENTITIES")) { deletedRecordList ->
            for (deletedRecordString in deletedRecordList) {

                val deletedRecordJson = retrieveJSONObject(deletedRecordString)
                entityViewModelIsToSyncList.deleteRecord(deletedRecordJson)
                println(
                    "deleted record id is ${deletedRecordJson?.getSafeString("__PrimaryKey")} for table ${
                    deletedRecordJson?.getSafeString("__TableName")}"
                )
            }
            assertEquals(
                "24",
                retrieveJSONObject(deletedRecordList[0])?.getSafeString("__PrimaryKey")
            )
            assertEquals(
                "25",
                retrieveJSONObject(deletedRecordList[1])?.getSafeString("__PrimaryKey")
            )
            assertEquals(
                "26",
                retrieveJSONObject(deletedRecordList[2])?.getSafeString("__PrimaryKey")
            )
        }
    }*/

    private fun initObservation() {
        // Initializing and filling EntityViewModelIsToSync list
        entityListViewModelList = mutableListOf()
        entityListViewModelList.add(entityListViewModelEmployee)
        entityListViewModelList.add(entityListViewModelService)
        entityListViewModelList.add(entityListViewModelOffice)
        entityListViewModelList.resetIsToSync()

//        _sourceIntEmployee = MutableLiveData()
//        _sourceIntService = MutableLiveData()
//        _sourceIntOffice = MutableLiveData()

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

        Mockito.`when`(sharedPreferencesHolder.globalStamp).thenReturn(sharedPreferencesGlobalStamp)
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
        _sourceIntEmployee.postValue(globalStampValue_0)
        _sourceIntService.postValue(globalStampValue_0)
        _sourceIntOffice.postValue(globalStampValue_0)
    }

    private fun removeObservers() {
        liveDataMergerEmployee.removeObserver(dataSync.globalStampObserver)
        liveDataMergerService.removeObserver(dataSync.globalStampObserver)
        liveDataMergerOffice.removeObserver(dataSync.globalStampObserver)
    }

    private fun sync(entityListViewModel: EntityListViewModel<*>) {
        println("[Sync] [Table : ${entityListViewModel.getAssociatedTableName()}, isToSync : ${entityListViewModel.isToSync.get()}]")

        if (entityListViewModel.isToSync.getAndSet(false)) {

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

            emitGlobalStamp(entityListViewModel, globalStampList)
        }
    }

    private fun syncToInfiniteAndBeyond(entityListViewModel: EntityListViewModel<*>) {
        println("[Sync] [Table : ${entityListViewModel.getAssociatedTableName()}, isToSync : ${entityListViewModel.isToSync.get()}]")

        if (entityListViewModel.isToSync.getAndSet(false)) {

            globalStampList = mutableListOf(
                dataSync.maxGlobalStamp + 1,
                dataSync.maxGlobalStamp + 2,
                dataSync.maxGlobalStamp + 3
            )

            if (dataSync.received.get() + 1 == dataSync.nbToReceive) {
                iteration++
            }

            emitGlobalStamp(entityListViewModel, globalStampList)
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
        entityListViewModel: EntityListViewModel<*>,
        globalStampList: List<Int>
    ) {
        when (entityListViewModel.getAssociatedTableName()) {
            EMPLOYEE_TABLE -> {
                println(" -> Table $EMPLOYEE_TABLE, emitting value ${globalStampList[0]}")
                _sourceIntEmployee.postValue(globalStampList[0])
            }
            SERVICE_TABLE -> {
                println(" -> Table $SERVICE_TABLE, emitting value ${globalStampList[1]}")
                _sourceIntService.postValue(globalStampList[1])
            }
            OFFICE_TABLE -> {
                println(" -> Table $OFFICE_TABLE, emitting value ${globalStampList[2]}")
                _sourceIntOffice.postValue(globalStampList[2])
            }
        }
    }
}
