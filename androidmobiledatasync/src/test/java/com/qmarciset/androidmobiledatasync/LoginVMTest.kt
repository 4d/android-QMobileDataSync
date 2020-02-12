/*
 * Created by Quentin Marciset on 11/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmarciset.androidmobiledatasync

import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import androidx.test.core.app.ApplicationProvider
import com.qmarciset.androidmobileapi.auth.AuthenticationState
import com.qmarciset.androidmobileapi.network.LoginApiService
import com.qmarciset.androidmobiledatasync.viewmodel.LoginViewModel
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Ignore("WIP")
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class LoginVMTest {

    @Mock
    lateinit var loginApiService: LoginApiService

    @Mock
    lateinit var dataLoadingObserver: Observer<Boolean>

    @Mock
    lateinit var authenticationStateObserver: Observer<AuthenticationState>

    private lateinit var loginViewModel: LoginViewModel

    @Rule
    @JvmField
    val instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        loginViewModel =
            LoginViewModel(ApplicationProvider.getApplicationContext(), loginApiService)
        loginViewModel.dataLoading.observeForever(dataLoadingObserver)
        loginViewModel.authenticationState.observeForever(authenticationStateObserver)
    }

    @After
    fun tearDown() {
    }
}
