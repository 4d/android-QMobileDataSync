/*
 * Created by Quentin Marciset on 23/3/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.testing.TestLifecycleOwner
import com.qmobile.qmobileapi.utils.SharedPreferencesHolder
import com.qmobile.qmobiledatasync.sync.DataSync
import com.qmobile.qmobiledatasync.sync.DataSyncStateEnum
import com.qmobile.qmobiledatasync.sync.GlobalStamp
import com.qmobile.qmobiledatasync.sync.resetIsToSync
import com.qmobile.qmobiledatasync.viewmodel.EntityListViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import org.junit.After
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
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

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
    private val sharedPreferencesGlobalStamp = 120
    private lateinit var globalStampList: MutableList<Int>
    private lateinit var entityListViewModelList: MutableList<EntityListViewModel<*>>

    // MutableLiveData for mocked ViewModels
    private val _sourceIntEmployee =
        MutableStateFlow(GlobalStamp("Employee", 0, true))
    private val sourceIntEmployee: StateFlow<GlobalStamp> = _sourceIntEmployee
    private val _sourceIntService =
        MutableStateFlow(GlobalStamp("Service", 0, true))
    private val sourceIntService: StateFlow<GlobalStamp> = _sourceIntService
    private val _sourceIntOffice =
        MutableStateFlow(GlobalStamp("Office", 0, true))
    private val sourceIntOffice: StateFlow<GlobalStamp> = _sourceIntOffice

    // Custom closures
    private lateinit var setupObservableClosure: (suspend (value: GlobalStamp) -> Unit) -> Unit
    private lateinit var syncClosure: (EntityListViewModel<*>, Boolean) -> Unit
    private lateinit var successfulSyncClosure: (Int) -> Unit
    private lateinit var unsuccessfulSyncClosure: () -> Unit

    // Mocks
    @Mock
    lateinit var activity: AppCompatActivity

    @Mock
    lateinit var sharedPreferencesHolder: SharedPreferencesHolder

    @Mock
    lateinit var entityListViewModelEmployee: EntityListViewModelEmployee

    @Mock
    lateinit var entityListViewModelService: EntityListViewModelService

    @Mock
    lateinit var entityListViewModelOffice: EntityListViewModelOffice

//    private val testDispatcher = TestCoroutineDispatcher()
//    private val testScope = TestCoroutineScope(testDispatcher)

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
//        Dispatchers.setMain(testDispatcher)
        initObservation()
    }

    @After
    fun ending() {
        Dispatchers.resetMain()
        // Reset Coroutine Dispatcher and Scope.
//        testDispatcher.cleanupTestCoroutines()
//        testScope.cleanupTestCoroutines()
    }

    @Test
    fun testDataSync() {

        val syncIterationMap = mutableMapOf<String, Int>()
        syncIterationMap[EMPLOYEE_TABLE] = 0
        syncIterationMap[SERVICE_TABLE] = 0
        syncIterationMap[OFFICE_TABLE] = 0

        dataSync = DataSync(
            activity,
            entityListViewModelList,
            sharedPreferencesHolder
        )

        mockForDataSync()

        setupObservableClosure = { _ ->
            observeMergedLiveData()
        }

        syncClosure = { entityViewModelIsToSync, _ ->
            syncIterationMap[entityViewModelIsToSync.getAssociatedTableName()]?.let { currentIteration ->
                syncIterationMap[entityViewModelIsToSync.getAssociatedTableName()] =
                    currentIteration + 1
            }

            sync(
                entityViewModelIsToSync,
                syncIterationMap[entityViewModelIsToSync.getAssociatedTableName()] ?: 0
            )
        }

        successfulSyncClosure = { _ ->
            println("[Synchronization performed, all tables are up-to-date]")
            assertSuccess()
        }

        unsuccessfulSyncClosure = {
            println("[Number of request max limit has been reached. Data synchronization is ending with tables not synchronized]")
            fail()
        }

        setClosures()

        dataSync.perform()
    }

    @Test
    fun testNumberOfRequestMaxLimit() {

        val syncIterationMap = mutableMapOf<String, Int>()
        syncIterationMap[EMPLOYEE_TABLE] = 0
        syncIterationMap[SERVICE_TABLE] = 0
        syncIterationMap[OFFICE_TABLE] = 0

        dataSync = DataSync(
            activity,
            entityListViewModelList,
            sharedPreferencesHolder
        )

        mockForDataSync()

        setupObservableClosure = { _ ->
            observeMergedLiveData()
        }

        syncClosure = { entityViewModelIsToSync, _ ->
            syncIterationMap[entityViewModelIsToSync.getAssociatedTableName()]?.let { currentIteration ->
                syncIterationMap[entityViewModelIsToSync.getAssociatedTableName()] =
                    currentIteration + 1
            }

            syncToInfiniteAndBeyond(entityViewModelIsToSync)
        }

        successfulSyncClosure = { _ ->
            println("[Synchronization performed, all tables are up-to-date]")
            fail()
        }

        unsuccessfulSyncClosure = {
            println("[Number of request max limit has been reached. Data synchronization is ending with tables not synchronized]")
            assertEquals(16, syncIterationMap.values.sum())
        }

        setClosures()

        dataSync.perform()
    }

    /*@Test
    fun testDecodeDeletedRecords() = testDispatcher.runBlockingTest {

        mockForDataSync()

        Mockito.`when`(entityListViewModelEmployee.coroutineScope).thenReturn(testScope)

        entityListViewModelEmployee.decodeDeletedRecords(sampleDeletedRecord.getSafeArray("__ENTITIES")) { deletedRecordList ->
            for (deletedRecordString in deletedRecordList) {

                val deletedRecordJson = retrieveJSONObject(deletedRecordString)

//                entityListViewModelEmployee.viewModelScope.launch {
                    entityListViewModelList.deleteRecord(deletedRecordJson)
//                }
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
        val employeeIsToSync = AtomicBoolean()
        val serviceIsToSync = AtomicBoolean()
        val officeIsToSync = AtomicBoolean()
        Mockito.`when`(entityListViewModelEmployee.isToSync).thenReturn(employeeIsToSync)
        Mockito.`when`(entityListViewModelService.isToSync).thenReturn(serviceIsToSync)
        Mockito.`when`(entityListViewModelOffice.isToSync).thenReturn(officeIsToSync)
        entityListViewModelList.resetIsToSync()
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

        entityListViewModelList.forEach {
            Mockito.`when`(it.dataSynchronized)
                .thenReturn(MutableStateFlow(DataSyncStateEnum.SYNCHRONIZING))
        }

        Mockito.`when`(sharedPreferencesHolder.globalStamp).thenReturn(sharedPreferencesGlobalStamp)
    }

    private fun observeMergedLiveData() = runBlocking {

        entityListViewModelList.map { it.globalStamp }.forEach { stateFlow ->
            val owner = TestLifecycleOwner(Lifecycle.State.STARTED, TestCoroutineDispatcher())
            owner.lifecycleScope.launch {
                delay(0L)
                owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    stateFlow.collect(dataSync.globalStampObserver)
                }
            }
        }
    }

    private fun setClosures() {
        dataSync.setupObservableClosure = setupObservableClosure
        dataSync.syncClosure = syncClosure
        dataSync.successfulSyncClosure = successfulSyncClosure
        dataSync.unsuccessfulSyncClosure = unsuccessfulSyncClosure
    }

    private fun sync(entityListViewModel: EntityListViewModel<*>, syncIteration: Int) {

        println("[Sync] [Table : ${entityListViewModel.getAssociatedTableName()}, isToSync : ${entityListViewModel.isToSync.get()}]")

        if (entityListViewModel.isToSync.getAndSet(false)) {

            assertLiveDataValues(syncIteration, entityListViewModel.getAssociatedTableName())

            globalStampList = when (syncIteration) {
                1 -> globalStampValueSet_1.toMutableList()
                2 -> globalStampValueSet_2.toMutableList()
                3 -> globalStampValueSet_3.toMutableList()
                else -> mutableListOf()
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

            emitGlobalStamp(entityListViewModel, globalStampList)
        }
    }

    private fun assertLiveDataValues(iteration: Int, tableName: String) {
        when (iteration) {
            1 -> {
                when (tableName) {
                    EMPLOYEE_TABLE -> assertEquals(
                        globalStampValue_0,
                        entityListViewModelEmployee.globalStamp.value.stampValue
                    )
                    SERVICE_TABLE -> assertEquals(
                        globalStampValue_0,
                        entityListViewModelService.globalStamp.value.stampValue
                    )
                    OFFICE_TABLE -> assertEquals(
                        globalStampValue_0,
                        entityListViewModelOffice.globalStamp.value.stampValue
                    )
                }
            }
            2 -> {
                when (tableName) {
                    EMPLOYEE_TABLE -> assertEquals(
                        globalStampValueSet_1[0],
                        entityListViewModelEmployee.globalStamp.value.stampValue
                    )
                    SERVICE_TABLE -> assertEquals(
                        globalStampValueSet_1[1],
                        entityListViewModelService.globalStamp.value.stampValue
                    )
                    OFFICE_TABLE -> assertEquals(
                        globalStampValueSet_1[2],
                        entityListViewModelOffice.globalStamp.value.stampValue
                    )
                }
            }
            3 -> {
                when (tableName) {
                    EMPLOYEE_TABLE -> assertEquals(
                        globalStampValueSet_2[0],
                        entityListViewModelEmployee.globalStamp.value.stampValue
                    )
                    SERVICE_TABLE -> assertEquals(
                        globalStampValueSet_2[1],
                        entityListViewModelService.globalStamp.value.stampValue
                    )
                    OFFICE_TABLE -> assertEquals(
                        globalStampValueSet_2[2],
                        entityListViewModelOffice.globalStamp.value.stampValue
                    )
                }
            }
        }
    }

    private fun assertSuccess() {
        assertEquals(
            globalStampValueSet_3[0],
            entityListViewModelEmployee.globalStamp.value.stampValue
        )
        assertEquals(
            globalStampValueSet_3[1],
            entityListViewModelService.globalStamp.value.stampValue
        )
        assertEquals(
            globalStampValueSet_3[2],
            entityListViewModelOffice.globalStamp.value.stampValue
        )
    }

    private fun emitGlobalStamp(
        entityListViewModel: EntityListViewModel<*>,
        globalStampList: List<Int>
    ) {
        when (entityListViewModel.getAssociatedTableName()) {
            EMPLOYEE_TABLE -> {
                println(" -> Table $EMPLOYEE_TABLE, emitting value ${globalStampList[0]}")
                _sourceIntEmployee.value =
                    GlobalStamp(EMPLOYEE_TABLE, globalStampList[0], true)
            }
            SERVICE_TABLE -> {
                println(" -> Table $SERVICE_TABLE, emitting value ${globalStampList[1]}")
                _sourceIntService.value =
                    GlobalStamp(SERVICE_TABLE, globalStampList[1], true)
            }
            OFFICE_TABLE -> {
                println(" -> Table $OFFICE_TABLE, emitting value ${globalStampList[2]}")
                _sourceIntOffice.value =
                    GlobalStamp(OFFICE_TABLE, globalStampList[2], true)
            }
            else -> {}
        }
    }
}
