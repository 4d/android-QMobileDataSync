/*
 * Created by Quentin Marciset on 16/11/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync

import android.app.Application
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.qmobile.qmobileapi.utils.getSafeObject
import org.json.JSONObject
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class RelationHelperTest {

    @Mock
    lateinit var application: Application

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)

        Mockito.`when`(application.packageName).thenReturn(
            ApplicationProvider.getApplicationContext<Application>().packageName.removeSuffix(".test")
        )
    }

    @Test
    fun getRelatedEntity() {
        // Many to One relation
        var jsonObject = JSONObject(employeeEntitiesString).getSafeObject("service")
        Assert.assertNotNull(jsonObject)

        val subJsonObject = JSONObject(jsonObject.toString()).getSafeObject("employees")
        Assert.assertNotNull(subJsonObject)

        // One to Many relation
        jsonObject = JSONObject(employeeEntitiesString).getSafeObject("serviceManaged")
        Assert.assertNotNull(jsonObject)

        // Unknown relationName
        jsonObject = JSONObject(employeeEntitiesString).getSafeObject("xxx")
        Assert.assertNull(jsonObject)
    }
}
