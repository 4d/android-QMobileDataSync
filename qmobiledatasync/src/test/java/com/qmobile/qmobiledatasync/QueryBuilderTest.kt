/*
 * Created by qmarciset on 1/3/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync

import android.app.Application
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.qmobile.qmobileapi.model.entity.Entities
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobileapi.utils.getSafeObject
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.relation.QueryBuilder
import com.qmobile.qmobiledatasync.relation.Relation
import com.qmobile.qmobiledatasync.utils.ReflectionUtils
import com.qmobile.qmobiledatasync.utils.RuntimeDataHolder
import io.mockk.mockkObject
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
import kotlin.reflect.full.declaredMemberProperties

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class QueryBuilderTest {

    private val prefixForTests = "com.qmobile.qmobiledatasync."

    @Mock
    lateinit var application: Application

    @Mock
    lateinit var entity: EntityModel

    private val relations: List<Relation> = listOf(
        Relation("Employee", "Service", "service", "employees", Relation.Type.MANY_TO_ONE),
        Relation("Service", "Employee", "employees", "service", Relation.Type.ONE_TO_MANY),
        Relation("Employee", "Employee", "manager", "subordinates", Relation.Type.MANY_TO_ONE),
        Relation("Employee", "Employee", "subordinates", "manager", Relation.Type.ONE_TO_MANY),
        Relation("Service", "Employee", "manager", "serviceManaged", Relation.Type.MANY_TO_ONE),
        Relation("Employee", "Service", "serviceManaged", "manager", Relation.Type.ONE_TO_MANY),

        Relation("Service", "Employee", "manager.subordinates", "manager", Relation.Type.ONE_TO_MANY),
        Relation("Service", "Service", "manager.service", "employees", Relation.Type.MANY_TO_ONE),

        Relation("Employee", "Employee", "managerfromservice", "serviceManaged", Relation.Type.MANY_TO_ONE, "service.manager"),
        Relation("Employee", "Employee", "employeesfromservice", "service", Relation.Type.ONE_TO_MANY, "service.employees"),
        Relation("Service", "Employee", "managerfromemployees", "subordinates", Relation.Type.ONE_TO_MANY, "employees.manager"),
        Relation("Service", "Employee", "subordinatesromemployees", "manager", Relation.Type.ONE_TO_MANY, "employees.subordinates"),

        Relation("Employee", "Employee", "abc", "serviceManaged", Relation.Type.MANY_TO_ONE, "manager.service.manager"),
        Relation("Employee", "Service", "def", "employees", Relation.Type.ONE_TO_MANY, "service.employees.service"),
        Relation("Employee", "Service", "ghi", "manager", Relation.Type.ONE_TO_MANY, "service.employees.serviceManaged"),
        Relation("Employee", "Service", "jkl", "manager", Relation.Type.ONE_TO_MANY, "service.manager.serviceManaged"),
        Relation("Service", "Service", "mno", "employees", Relation.Type.ONE_TO_MANY, "employees.manager.service"),
        Relation("Service", "Employee", "pqr", "manager", Relation.Type.ONE_TO_MANY, "employees.manager.subordinates"),
        Relation("Service", "Service", "stu", "employees", Relation.Type.ONE_TO_MANY, "employees.subordinates.service"),
        Relation("Service", "Service", "vwx", "manager", Relation.Type.ONE_TO_MANY, "employees.subordinates.serviceManaged"),
        Relation("Service", "Employee", "aaa", "manager", Relation.Type.ONE_TO_MANY, "manager.manager.serviceManaged.employees")
    )

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)

        Mockito.`when`(application.packageName).thenReturn(
            ApplicationProvider.getApplicationContext<Application>().packageName.removeSuffix(".test")
        )

        mockkObject(BaseApp)
        val mockRuntimeDataHolder = Mockito.mock(RuntimeDataHolder::class.java)
        mockRuntimeDataHolder.relations = relations
        BaseApp.runtimeDataHolder = mockRuntimeDataHolder
    }

    @Test
    fun manyToOneManyToOneManyToOne() {

        val relation = Relation("Employee", "Employee", "abc", "serviceManaged", Relation.Type.MANY_TO_ONE, "manager.service.manager")
        val query = QueryBuilder.createQuery(relation, entity)

        val expectation = "SELECT * FROM Employee AS T3 WHERE EXISTS ( " +
                "SELECT * FROM Service AS T2 WHERE T3.__KEY = T2.__managerKey " +
                "AND EXISTS ( " +
                "SELECT * FROM Service AS T1 WHERE T2.__KEY = T1.__serviceKey " +
                "AND T1.__KEY = -1" +
                " ) ) LIMIT 1"
        Assert.assertEquals(expectation, query.toString())
    }
}
