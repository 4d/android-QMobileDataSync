/*
 * Created by Quentin Marciset on 24/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync

import android.net.ConnectivityManager
import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import com.qmobile.qmobileapi.network.AccessibilityApiService
import com.qmobile.qmobiledatasync.viewmodel.ConnectivityViewModel
import io.reactivex.Single
import org.junit.Assert
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
class ConnectivityViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var connectivityViewModel: ConnectivityViewModel

    @Mock
    lateinit var mockedAccessibilityApiService: AccessibilityApiService

    @Mock
    lateinit var connectivityManager: ConnectivityManager

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        connectivityViewModel = ConnectivityViewModel(
            ApplicationProvider.getApplicationContext(),
            connectivityManager,
            mockedAccessibilityApiService
        )
    }

    @Test
    fun testServerAccessible() {

        val response = buildSampleResponseFromJsonString("", true)

        Mockito.`when`(
            mockedAccessibilityApiService.checkAccessibility()
        ).thenAnswer { Single.just(response) }

        connectivityViewModel.isServerConnectionOk { isAccessible ->
            Assert.assertTrue(isAccessible)
        }
    }

    @Test
    fun testServerNotAccessible() {

        val response = buildSampleResponseFromJsonString("", false)

        Mockito.`when`(
            mockedAccessibilityApiService.checkAccessibility()
        ).thenAnswer { Single.just(response) }

        connectivityViewModel.isServerConnectionOk { isAccessible ->
            Assert.assertFalse(isAccessible)
        }
    }
}
