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
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class DataSyncUnitTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var entityViewModelIsToSyncList: MutableList<EntityViewModelIsToSync>

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

    private var NUMBER_OF_REQUEST_MAX_LIMIT = 0
    private val FACTOR_OF_MAX_SUCCESSIVE_SYNC = 3
    val sharedPreferencesGlobalStamp = 120
    var nbToReceive = 0

    lateinit var sourceIntEmployee: MutableLiveData<Int>
    lateinit var sourceIntService: MutableLiveData<Int>
    lateinit var sourceIntOffice: MutableLiveData<Int>

    var globalStampList = mutableListOf<Int>()

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        getEntityListViewModels()
    }

    private fun getEntityListViewModels() {

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
    }

    @Test
    fun mediatorLiveData_Test() {

        val sourceIntEmployee = MutableLiveData<Int>()
        val sourceIntService = MutableLiveData<Int>()
        val sourceIntOffice = MutableLiveData<Int>()
        val liveDataMerger = MediatorLiveData<GlobalStampWithTable>()

        liveDataMerger.addSource(sourceIntEmployee) {
            if (it != null) {
                liveDataMerger.value =
                    GlobalStampWithTable(
                        "Employee",
                        it
                    )
            }
        }
        liveDataMerger.addSource(sourceIntService) {
            if (it != null) {
                liveDataMerger.value =
                    GlobalStampWithTable(
                        "Service",
                        it
                    )
            }
        }
        liveDataMerger.addSource(sourceIntOffice) {
            if (it != null) {
                liveDataMerger.value =
                    GlobalStampWithTable(
                        "Office",
                        it
                    )
            }
        }
        liveDataMerger.observeForever{}

        sourceIntEmployee.value = 123
        assertEquals(123, liveDataMerger.value?.globalStamp)
        assertEquals("Employee", liveDataMerger.value?.tableName)

        sourceIntService.value = 456
        assertEquals(456, liveDataMerger.value?.globalStamp)
        assertEquals("Service", liveDataMerger.value?.tableName)

        sourceIntOffice.value = 789
        assertEquals(789, liveDataMerger.value?.globalStamp)
        assertEquals("Office", liveDataMerger.value?.tableName)
    }

    @Test
    fun testDataSyncCoreObserver() {

        sourceIntEmployee = MutableLiveData()
        sourceIntService = MutableLiveData()
        sourceIntOffice = MutableLiveData()
        val liveDataMerger = MediatorLiveData<GlobalStampWithTable>()

        Mockito.`when`(entityListViewModelEmployee.globalStamp).thenReturn(sourceIntEmployee)
        Mockito.`when`(entityListViewModelService.globalStamp).thenReturn(sourceIntService)
        Mockito.`when`(entityListViewModelOffice.globalStamp).thenReturn(sourceIntOffice)

        Mockito.`when`(entityListViewModelEmployee.getAssociatedTableName()).thenReturn("Employee")
        Mockito.`when`(entityListViewModelService.getAssociatedTableName()).thenReturn("Service")
        Mockito.`when`(entityListViewModelOffice.getAssociatedTableName()).thenReturn("Office")

        Mockito.`when`(authInfoHelper.globalStamp).thenReturn(sharedPreferencesGlobalStamp)

        liveDataMerger.addSource(sourceIntEmployee) {
            if (it != null) {
                liveDataMerger.value =
                    GlobalStampWithTable(
                        "Employee",
                        it
                    )
            }
        }
        liveDataMerger.addSource(sourceIntService) {
            if (it != null) {
                liveDataMerger.value =
                    GlobalStampWithTable(
                        "Service",
                        it
                    )
            }
        }
        liveDataMerger.addSource(sourceIntOffice) {
            if (it != null) {
                liveDataMerger.value =
                    GlobalStampWithTable(
                        "Office",
                        it
                    )
            }
        }

        var viewModelStillInitializing = true
        var requestPerformed = 0
        val received = AtomicInteger(0)
        nbToReceive = entityViewModelIsToSyncList.filter { it.isToSync }.size
        val receivedSyncedTableGS = mutableListOf<GlobalStampWithTable>()
        val savedVMGSList = mutableListOf<GlobalStampWithTable>()
        NUMBER_OF_REQUEST_MAX_LIMIT = nbToReceive * FACTOR_OF_MAX_SUCCESSIVE_SYNC

        assertEquals(3, nbToReceive)

        val globalStampObserver = Observer<GlobalStampWithTable> { globalStampWithTable ->

            println("[globalStampObserver] [Table : ${globalStampWithTable.tableName}, value : ${globalStampWithTable.globalStamp}]")
            for (eviy in entityViewModelIsToSyncList)
                println("gs = ${eviy.vm.globalStamp.value}")

            if (!viewModelStillInitializing) {

                /*for (entityViewModelIsToSync in entityViewModelIsToSyncList) {
                    for (savedVMGS in savedVMGSList) {
                        if (entityViewModelIsToSync.vm.globalStamp.value ?: 0 > savedVMGS.globalStamp)
                            receivedSyncedTableGS.add(globalStampWithTable)
                    }
                }*/

                receivedSyncedTableGS.add(globalStampWithTable)

                println("nbToReceive = $nbToReceive, received = ${received.get() + 1}")
                if (received.incrementAndGet() == nbToReceive) {

                    val maxGlobalStamp = dataSync.getMaxGlobalStamp(receivedSyncedTableGS, authInfoHelper.globalStamp)
                    println("maxGlobalStamp = $maxGlobalStamp")

                    val isAtLeastOneToSync = dataSync.checkIfAtLeastOneTableToSync(maxGlobalStamp, entityViewModelIsToSyncList)

                    if (!isAtLeastOneToSync) {
                        println("Synchronization performed, all tables are up-to-date")
                        assertEquals(260, entityListViewModelEmployee.globalStamp)
                        assertEquals(260, entityListViewModelService.globalStamp)
                        assertEquals(260, entityListViewModelOffice.globalStamp)
                    } else {
                        println("isAtLeastOneToSync true")
                        received.set(0)
                        requestPerformed++
                        if (requestPerformed <= NUMBER_OF_REQUEST_MAX_LIMIT) {
                            println("requestPerformed = $requestPerformed")
                            if (requestPerformed == 1) {
                                assertEquals(123, entityListViewModelEmployee.globalStamp)
                                assertEquals(123, entityListViewModelService.globalStamp)
                                assertEquals(256, entityListViewModelOffice.globalStamp)
                                sync(2)
                            } else {
                                assertEquals(256, entityListViewModelEmployee.globalStamp)
                                assertEquals(256, entityListViewModelService.globalStamp)
                                assertEquals(259, entityListViewModelOffice.globalStamp)
                                sync(3)
                            }
                        }
                    }
                }
            } else {
                  println("nbToReceive = $nbToReceive, received = ${received.get() + 1}")
                  if (received.incrementAndGet() == nbToReceive) {
                      viewModelStillInitializing = false
                      received.set(0)

//                      for (entityViewModelIsToSync in entityViewModelIsToSyncList)
//                          savedVMGSList.add(GlobalStampWithTable(entityViewModelIsToSync.vm.getAssociatedTableName(), entityViewModelIsToSync.vm.globalStamp.value ?: 0))

                      // first sync
                      sync(1)
//                      sourceIntOffice.postValue(134)
//                      sourceIntOffice.postValue(135)
//                      sourceIntOffice.postValue(136)
                  }
            }
        }

        liveDataMerger.removeObserver(globalStampObserver)
        liveDataMerger.observeForever(globalStampObserver)

        // simulates LiveData initialization
        sourceIntEmployee.postValue(0)
        sourceIntService.postValue(0)
        sourceIntOffice.postValue(0)
    }

    private fun sync(iteration: Int) {
        nbToReceive = entityViewModelIsToSyncList.filter { it.isToSync }.size
        NUMBER_OF_REQUEST_MAX_LIMIT = nbToReceive * FACTOR_OF_MAX_SUCCESSIVE_SYNC

        for (entityViewModelIsToSync in entityViewModelIsToSyncList) {

            println("Sync : tableName = ${entityViewModelIsToSync.vm.getAssociatedTableName()}, isToSync : ${entityViewModelIsToSync.isToSync}")

            if (entityViewModelIsToSync.isToSync) {
                entityViewModelIsToSync.isToSync = false

                when (iteration) {
                    1 -> globalStampList = mutableListOf(123, 124, 256)
                    2 -> globalStampList = mutableListOf(256, 256, 259)
                    3 -> globalStampList = mutableListOf(260, 260, 260)
                }

                emitGlobalStamp(entityViewModelIsToSync, globalStampList)
            }
        }
    }

    private fun emitGlobalStamp(entityViewModelIsToSync: EntityViewModelIsToSync, globalStampList: List<Int>) {
        when (entityViewModelIsToSync.vm.getAssociatedTableName()) {
            "Employee" -> {
                println("table Employee, emitting value ${globalStampList[0]}")
                sourceIntEmployee.postValue(globalStampList[0])
            }
            "Service" -> {
                println("table Service, NOT emitting value ${globalStampList[1]}")
                sourceIntService.postValue(globalStampList[1])
            }
            "Office" -> {
                println("table Office, NOT emitting value ${globalStampList[2]}")
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