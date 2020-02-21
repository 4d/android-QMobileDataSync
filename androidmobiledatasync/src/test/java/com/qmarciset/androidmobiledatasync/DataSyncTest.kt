/*
 * Created by Quentin Marciset on 20/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmarciset.androidmobiledatasync

import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.qmarciset.androidmobileapi.auth.AuthInfoHelper
import com.qmarciset.androidmobileapi.model.entity.EntityModel
import com.qmarciset.androidmobiledatasync.model.EntityViewModelIsToSync
import com.qmarciset.androidmobiledatasync.model.GlobalStampWithTable
import com.qmarciset.androidmobiledatasync.viewmodel.EntityListViewModel
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
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
class DataSyncUnitTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    companion object {
        var NUMBER_OF_REQUEST_MAX_LIMIT = 0
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
    private var nbToReceive = 0
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

        sourceIntEmployee.value = 123
        sourceIntService.value = 456
        sourceIntOffice.value = 789

        assertEquals(123, liveDataMergerEmployee.value?.globalStamp)
        assertEquals(EMPLOYEE_TABLE, liveDataMergerEmployee.value?.tableName)

        assertEquals(456, liveDataMergerService.value?.globalStamp)
        assertEquals(SERVICE_TABLE, liveDataMergerService.value?.tableName)

        assertEquals(789, liveDataMergerOffice.value?.globalStamp)
        assertEquals(OFFICE_TABLE, liveDataMergerOffice.value?.tableName)
    }

    @Test
    fun testDataSyncCoreObserver() {

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

        var viewModelStillInitializing = true
        var requestPerformed = 0
        val received = AtomicInteger(0)
        nbToReceive = entityViewModelIsToSyncList.filter { it.isToSync }.size
        val receivedSyncedTableGS = mutableListOf<GlobalStampWithTable>()
        NUMBER_OF_REQUEST_MAX_LIMIT = nbToReceive * FACTOR_OF_MAX_SUCCESSIVE_SYNC

        assertEquals(3, nbToReceive)

        globalStampObserver = Observer { globalStampWithTable ->

            println("[globalStampObserver] [Table : ${globalStampWithTable.tableName}, GlobalStamp : ${globalStampWithTable.globalStamp}]")

            for (entityViewModelIsToSync in entityViewModelIsToSyncList)
                println("[Table : ${entityViewModelIsToSync.vm.getAssociatedTableName()}, GlobalStamp : ${entityViewModelIsToSync.vm.globalStamp.value}")

            if (!viewModelStillInitializing) {

                receivedSyncedTableGS.add(globalStampWithTable)

                println("nbToReceive = $nbToReceive, received = ${received.get() + 1}")
                if (received.incrementAndGet() == nbToReceive) {

                    val maxGlobalStamp = dataSync.getMaxGlobalStamp(
                        receivedSyncedTableGS,
                        authInfoHelper.globalStamp
                    )
                    println("maxGlobalStamp = $maxGlobalStamp")

                    val isAtLeastOneToSync = dataSync.checkIfAtLeastOneTableToSync(
                        maxGlobalStamp,
                        entityViewModelIsToSyncList
                    )

                    if (isAtLeastOneToSync) {
                        println("isAtLeastOneToSync true")
                        received.set(0)
                        requestPerformed++
                        if (requestPerformed <= NUMBER_OF_REQUEST_MAX_LIMIT) {
                            println("requestPerformed = $requestPerformed")
                            if (requestPerformed == 1) {
                                assertEquals(123, entityListViewModelEmployee.globalStamp.value)
                                assertEquals(124, entityListViewModelService.globalStamp.value)
                                assertEquals(256, entityListViewModelOffice.globalStamp.value)
                                sync(2)
                            } else {
                                assertEquals(256, entityListViewModelEmployee.globalStamp.value)
                                assertEquals(260, entityListViewModelService.globalStamp.value)
                                assertEquals(256, entityListViewModelOffice.globalStamp.value)
                                sync(3)
                            }
                        }
                    } else {
                        println("Synchronization performed, all tables are up-to-date")
                        assertEquals(260, entityListViewModelEmployee.globalStamp.value)
                        assertEquals(260, entityListViewModelService.globalStamp.value)
                        assertEquals(260, entityListViewModelOffice.globalStamp.value)
                    }
                }
            } else {
                println("nbToReceive = $nbToReceive, received = ${received.get() + 1}")
                if (received.incrementAndGet() == nbToReceive) {
                    viewModelStillInitializing = false
                    received.set(0)
                    // first sync
                    sync(1)
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

    private fun sync(iteration: Int) {
        nbToReceive = entityViewModelIsToSyncList.filter { it.isToSync }.size
        NUMBER_OF_REQUEST_MAX_LIMIT = nbToReceive * FACTOR_OF_MAX_SUCCESSIVE_SYNC

        assertLiveData(iteration)

        var globalStampList: MutableList<Int>

        for (entityViewModelIsToSync in entityViewModelIsToSyncList) {

            println("Sync : tableName = ${entityViewModelIsToSync.vm.getAssociatedTableName()}, isToSync : ${entityViewModelIsToSync.isToSync}")

            if (entityViewModelIsToSync.isToSync) {
                entityViewModelIsToSync.isToSync = false

                globalStampList = when (iteration) {
                    1 -> mutableListOf(123, 124, 256)
                    2 -> mutableListOf(256, 260, 256)
                    3 -> mutableListOf(260, 260, 260)
                    else -> mutableListOf()
                }
                emitGlobalStamp(entityViewModelIsToSync, globalStampList)
            }
        }
    }

    private fun assertLiveData(iteration: Int) {
        when (iteration) {
            1 -> {
                assertEquals(0, entityListViewModelEmployee.globalStamp.value)
                assertEquals(0, entityListViewModelService.globalStamp.value)
                assertEquals(0, entityListViewModelOffice.globalStamp.value)
            }
            2 -> {
                assertEquals(123, entityListViewModelEmployee.globalStamp.value)
                assertEquals(124, entityListViewModelService.globalStamp.value)
                assertEquals(256, entityListViewModelOffice.globalStamp.value)
            }
            3 -> {
                assertEquals(256, entityListViewModelEmployee.globalStamp.value)
                assertEquals(260, entityListViewModelService.globalStamp.value)
                assertEquals(256, entityListViewModelOffice.globalStamp.value)
            }
        }
    }

    private fun assertSuccess() {
        assertEquals(260, entityListViewModelEmployee.globalStamp.value)
        assertEquals(260, entityListViewModelService.globalStamp.value)
        assertEquals(260, entityListViewModelOffice.globalStamp.value)
    }

    private fun emitGlobalStamp(
        entityViewModelIsToSync: EntityViewModelIsToSync,
        globalStampList: List<Int>
    ) {
        when (entityViewModelIsToSync.vm.getAssociatedTableName()) {
            EMPLOYEE_TABLE -> {
                println("table Employee, emitting value ${globalStampList[0]}")
                sourceIntEmployee.postValue(globalStampList[0])
            }
            SERVICE_TABLE -> {
                println("table Service, emitting value ${globalStampList[1]}")
                sourceIntService.postValue(globalStampList[1])
            }
            OFFICE_TABLE -> {
                println("table Office, emitting value ${globalStampList[2]}")
                sourceIntOffice.postValue(globalStampList[2])
            }
        }
    }
}

// Sample tables for synchronization

class Employee(
    override val __KEY: String,
    override val __STAMP: Int? = null,
    override val __GlobalStamp: Int? = null,
    override val __TIMESTAMP: String? = null,
    override val __entityModel: String? = null
) : EntityModel

class Service(
    override val __KEY: String,
    override val __STAMP: Int? = null,
    override val __GlobalStamp: Int? = null,
    override val __TIMESTAMP: String? = null,
    override val __entityModel: String? = null
) : EntityModel

class Office(
    override val __KEY: String,
    override val __STAMP: Int? = null,
    override val __GlobalStamp: Int? = null,
    override val __TIMESTAMP: String? = null,
    override val __entityModel: String? = null
) : EntityModel
