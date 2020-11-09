/*
 * Created by Quentin Marciset on 24/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync

import android.net.ConnectivityManager
import android.net.Network
import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import com.qmobile.qmobileapi.connectivity.NetworkStateEnum
import com.qmobile.qmobileapi.connectivity.NetworkStateMonitor
import com.qmobile.qmobiledatasync.viewmodel.ConnectivityViewModel
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class ConnectivityViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val sourceServerAccessible: MutableLiveData<Boolean> = MutableLiveData()

    private lateinit var networkStateMonitor: NetworkStateMonitor

    @Mock
    lateinit var connectivityViewModel: ConnectivityViewModel

    @Mock
    lateinit var connectivityManager: ConnectivityManager

    @Mock
    lateinit var network: Network

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun testNetworkStateMonitorLiveData() {

        networkStateMonitor = NetworkStateMonitor(connectivityManager)

        Mockito.`when`(connectivityViewModel.networkStateMonitor).thenReturn(networkStateMonitor)

        networkStateMonitor.networkStateObject.onAvailable(network)
        Assert.assertEquals(NetworkStateEnum.CONNECTED, connectivityViewModel.networkStateMonitor.value)

        networkStateMonitor.networkStateObject.onUnavailable()
        Assert.assertEquals(
            NetworkStateEnum.DISCONNECTED,
            connectivityViewModel.networkStateMonitor.value
        )

        networkStateMonitor.networkStateObject.onLost(network)
        Assert.assertEquals(
            NetworkStateEnum.CONNECTION_LOST,
            connectivityViewModel.networkStateMonitor.value
        )
    }

    @Test
    fun testServerAccessible() {

        Mockito.`when`(connectivityViewModel.serverAccessible).thenReturn(sourceServerAccessible)

        Mockito.`when`(connectivityViewModel.checkAccessibility(anyString())).thenAnswer {
            sourceServerAccessible.postValue(true)
        }

        connectivityViewModel.checkAccessibility(anyString())
        Assert.assertEquals(true, connectivityViewModel.serverAccessible.value)
    }

    @Test
    fun testServerNotAccessible() {

        Mockito.`when`(connectivityViewModel.serverAccessible).thenReturn(sourceServerAccessible)

        Mockito.`when`(connectivityViewModel.checkAccessibility(anyString())).thenAnswer {
            sourceServerAccessible.postValue(false)
        }

        connectivityViewModel.checkAccessibility(anyString())
        Assert.assertEquals(false, connectivityViewModel.serverAccessible.value)
    }
}
