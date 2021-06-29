/*
 * Created by Quentin Marciset on 16/11/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync

import android.app.Application
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.qmobile.qmobileapi.model.entity.Entities
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobiledatasync.relation.RelationHelper
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.reflect.full.declaredMemberProperties

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class RelationHelperTest {

    private val prefixForTests = "com.qmobile.qmobiledatasync."

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
    fun isManyToOneRelation() {

        val tableNames =
            listOf("Table1", "Table2", " ${prefixForTests}Employee", " ${prefixForTests}Service")

        data class CustomEmployee(
            val serviceProperty: Service? = null,
            override val __KEY: String,
            override val __STAMP: Int? = null,
            override val __GlobalStamp: Int? = null,
            override val __TIMESTAMP: String? = null
        ) : EntityModel

        val properties: Collection<*>
        properties = CustomEmployee::class.declaredMemberProperties

        properties.toList().forEach { property ->

            val manyToOneRelation = RelationHelper.isManyToOneRelation(
                property,
                application,
                tableNames
            )

            if (property.toString().contains("serviceProperty")) {
                Assert.assertEquals(" ${prefixForTests}Service", manyToOneRelation)
            } else {
                Assert.assertNull(manyToOneRelation)
            }
        }
    }

    @Test
    fun isOneToManyRelation() {

        val tableNames =
            listOf("Table1", "Table2", "${prefixForTests}Employee", "${prefixForTests}Service")

        data class CustomEmployee(
            val serviceProperty: Service? = null,
            val employeesProperty: Entities<Employee>? = null,
            override val __KEY: String,
            override val __STAMP: Int? = null,
            override val __GlobalStamp: Int? = null,
            override val __TIMESTAMP: String? = null
        ) : EntityModel

        val properties: Collection<*>
        properties = CustomEmployee::class.declaredMemberProperties

        properties.toList().forEach { property ->

            val oneToManyRelation = RelationHelper.isOneToManyRelation(
                property,
                application,
                tableNames
            )

            if (property.toString().contains("employeesProperty")) {
                Assert.assertEquals("${prefixForTests}Employee", oneToManyRelation)
            } else {
                Assert.assertNull(oneToManyRelation)
            }
        }
    }

    @Test
    fun getRelatedEntity() {
        // Many to One relation
        var jsonObject = RelationHelper.getRelatedEntity(
            entityJsonString = employeeEntitiesString,
            relationName = "service"
        )
        Assert.assertNotNull(jsonObject)

        val subJsonObject = RelationHelper.getRelatedEntity(
            entityJsonString = jsonObject.toString(),
            relationName = "employees"
        )
        Assert.assertNotNull(subJsonObject)

        // One to Many relation
        jsonObject = RelationHelper.getRelatedEntity(
            entityJsonString = employeeEntitiesString,
            relationName = "serviceManaged"
        )
        Assert.assertNotNull(jsonObject)

        // Unknown relationName
        jsonObject = RelationHelper.getRelatedEntity(
            entityJsonString = employeeEntitiesString,
            relationName = "xxx"
        )
        Assert.assertNull(jsonObject)
    }
}
