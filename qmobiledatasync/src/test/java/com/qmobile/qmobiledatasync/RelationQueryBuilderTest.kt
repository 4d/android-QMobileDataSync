/*
 * Created by qmarciset on 1/3/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync

import android.os.Build
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.relation.RelationQueryBuilder
import com.qmobile.qmobiledatasync.relation.Relation
import com.qmobile.qmobiledatasync.utils.GenericRelationHelper
import com.qmobile.qmobiledatasync.utils.RuntimeDataHolder
import io.mockk.mockkObject
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
class RelationQueryBuilderTest {

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

        Relation(
            "Employee",
            "Employee",
            "managerfromservice",
            "serviceManaged",
            Relation.Type.MANY_TO_ONE,
            "service.manager"
        ),
        Relation(
            "Employee",
            "Employee",
            "employeesfromservice",
            "service",
            Relation.Type.ONE_TO_MANY,
            "service.employees"
        ),
        Relation(
            "Service",
            "Employee",
            "managerfromemployees",
            "subordinates",
            Relation.Type.ONE_TO_MANY,
            "employees.manager"
        ),
        Relation(
            "Service",
            "Employee",
            "subordinatesromemployees",
            "manager",
            Relation.Type.ONE_TO_MANY,
            "employees.subordinates"
        ),

        Relation("Employee", "Employee", "abc", "serviceManaged", Relation.Type.MANY_TO_ONE, "manager.service.manager"),
        Relation("Employee", "Service", "def", "employees", Relation.Type.ONE_TO_MANY, "service.employees.service"),
        Relation(
            "Employee",
            "Service",
            "ghi",
            "manager",
            Relation.Type.ONE_TO_MANY,
            "service.employees.serviceManaged"
        ),
        Relation("Employee", "Service", "jkl", "manager", Relation.Type.ONE_TO_MANY, "service.manager.serviceManaged"),
        Relation("Service", "Service", "mno", "employees", Relation.Type.ONE_TO_MANY, "employees.manager.service"),
        Relation("Service", "Employee", "pqr", "manager", Relation.Type.ONE_TO_MANY, "employees.manager.subordinates"),
        Relation("Service", "Service", "stu", "employees", Relation.Type.ONE_TO_MANY, "employees.subordinates.service"),
        Relation(
            "Service",
            "Service",
            "vwx",
            "manager",
            Relation.Type.ONE_TO_MANY,
            "employees.subordinates.serviceManaged"
        ),
        Relation(
            "Service",
            "Employee",
            "aaa",
            "manager",
            Relation.Type.ONE_TO_MANY,
            "manager.manager.serviceManaged.employees"
        )
    )

    private val entityKey = "5"

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)

        Mockito.`when`(entity.__KEY).thenReturn(entityKey)

        mockkObject(BaseApp)
        val mockRuntimeDataHolder = Mockito.mock(RuntimeDataHolder::class.java)
        val mockGenericRelationHelper = Mockito.mock(GenericRelationHelper::class.java)
        mockRuntimeDataHolder.relations = relations
        BaseApp.runtimeDataHolder = mockRuntimeDataHolder
        BaseApp.genericRelationHelper = mockGenericRelationHelper
        Mockito.`when`(
            mockGenericRelationHelper.getRelationId(
                Mockito.anyString(),
                Mockito.anyString(),
                any(EntityModel::class.java)
            )
        ).thenReturn("-1")
    }

    @Test
    fun `many_to_one many_to_one many_to_one`() {

        val relation = Relation(
            "Employee",
            "Employee",
            "abc",
            "serviceManaged",
            Relation.Type.MANY_TO_ONE,
            "manager.service.manager"
        )
        val query = RelationQueryBuilder.createQuery(relation, entity)

        val expectation = "SELECT * FROM Employee AS T_FINAL WHERE EXISTS ( " +
            "SELECT * FROM Service AS T2 WHERE T_FINAL.__KEY = T2.__managerKey " +
            "AND EXISTS ( " +
            "SELECT * FROM Employee AS T1 WHERE T2.__KEY = T1.__serviceKey " +
            "AND T1.__KEY = -1" +
            " ) ) LIMIT 1"
        Assert.assertEquals(expectation, query.sql)
    }

    @Test
    fun `many_to_one many_to_one one_to_many`() {

        val relation =
            Relation("Employee", "Service", "", "manager", Relation.Type.ONE_TO_MANY, "service.manager.serviceManaged")
        val query = RelationQueryBuilder.createQuery(relation, entity)

        val expectation = "SELECT * FROM Service AS T_FINAL WHERE EXISTS ( " +
            "SELECT * FROM Employee AS T2 WHERE T_FINAL.__managerKey = T2.__KEY " +
            "AND EXISTS ( " +
            "SELECT * FROM Service AS T1 WHERE T2.__KEY = T1.__managerKey " +
            "AND T1.__KEY = -1" +
            "" +
            " ) )"
        Assert.assertEquals(expectation, query.sql)
    }

    @Test
    fun `many_to_one one_to_many many_to_one`() {

        val relation =
            Relation("Employee", "Service", "", "employees", Relation.Type.ONE_TO_MANY, "service.employees.service")
        val query = RelationQueryBuilder.createQuery(relation, entity)

        val expectation = "SELECT * FROM Service AS T_FINAL WHERE EXISTS ( " +
            "SELECT * FROM Employee AS T2 WHERE T_FINAL.__KEY = T2.__serviceKey " +
            "AND EXISTS ( " +
            "SELECT * FROM Service AS T1 WHERE T2.__serviceKey = T1.__KEY " +
            "AND T1.__KEY = -1" +
            " ) )"
        Assert.assertEquals(expectation, query.sql)
    }

    @Test
    fun `many_to_one one_to_many one_to_many`() {

        val relation = Relation(
            "Employee",
            "Service",
            "",
            "manager",
            Relation.Type.ONE_TO_MANY,
            "service.employees.serviceManaged"
        )
        val query = RelationQueryBuilder.createQuery(relation, entity)

        val expectation = "SELECT * FROM Service AS T_FINAL WHERE EXISTS ( " +
            "SELECT * FROM Employee AS T2 WHERE T_FINAL.__managerKey = T2.__KEY " +
            "AND EXISTS ( " +
            "SELECT * FROM Service AS T1 WHERE T2.__serviceKey = T1.__KEY " +
            "AND T1.__KEY = -1" +
            " ) )"
        Assert.assertEquals(expectation, query.sql)
    }

    @Test
    fun `one_to_many many_to_one many_to_one`() {

        val relation =
            Relation("Service", "Service", "", "employees", Relation.Type.ONE_TO_MANY, "employees.manager.service")
        val query = RelationQueryBuilder.createQuery(relation, entity)

        val expectation = "SELECT * FROM Service AS T_FINAL WHERE EXISTS ( " +
            "SELECT * FROM Employee AS T2 WHERE T_FINAL.__KEY = T2.__serviceKey " +
            "AND EXISTS ( " +
            "SELECT * FROM Employee AS T1 WHERE T2.__KEY = T1.__managerKey " +
            "AND T1.__serviceKey = ${entity.__KEY}" +
            " ) )"
        Assert.assertEquals(expectation, query.sql)
    }

    @Test
    fun `one_to_many many_to_one one_to_many`() {

        val relation =
            Relation("Service", "Employee", "", "manager", Relation.Type.ONE_TO_MANY, "employees.manager.subordinates")
        val query = RelationQueryBuilder.createQuery(relation, entity)

        val expectation = "SELECT * FROM Employee AS T_FINAL WHERE EXISTS ( " +
            "SELECT * FROM Employee AS T2 WHERE T_FINAL.__managerKey = T2.__KEY " +
            "AND EXISTS ( " +
            "SELECT * FROM Employee AS T1 WHERE T2.__KEY = T1.__managerKey " +
            "AND T1.__serviceKey = ${entity.__KEY}" +
            " ) )"
        Assert.assertEquals(expectation, query.sql)
    }

    @Test
    fun `one_to_many one_to_many many_to_one`() {

        val relation =
            Relation("Service", "Service", "", "employees", Relation.Type.ONE_TO_MANY, "employees.subordinates.service")
        val query = RelationQueryBuilder.createQuery(relation, entity)

        val expectation = "SELECT * FROM Service AS T_FINAL WHERE EXISTS ( " +
            "SELECT * FROM Employee AS T2 WHERE T_FINAL.__KEY = T2.__serviceKey " +
            "AND EXISTS ( " +
            "SELECT * FROM Employee AS T1 WHERE T2.__managerKey = T1.__KEY " +
            "AND T1.__serviceKey = ${entity.__KEY}" +
            " ) )"
        Assert.assertEquals(expectation, query.sql)
    }

    @Test
    fun `one_to_many one_to_many one_to_many`() {

        val relation = Relation(
            "Service",
            "Service",
            "",
            "manager",
            Relation.Type.ONE_TO_MANY,
            "employees.subordinates.serviceManaged"
        )
        val query = RelationQueryBuilder.createQuery(relation, entity)

        val expectation = "SELECT * FROM Service AS T_FINAL WHERE EXISTS ( " +
            "SELECT * FROM Employee AS T2 WHERE T_FINAL.__managerKey = T2.__KEY " +
            "AND EXISTS ( " +
            "SELECT * FROM Employee AS T1 WHERE T2.__managerKey = T1.__KEY " +
            "AND T1.__serviceKey = ${entity.__KEY}" +
            " ) )"
        Assert.assertEquals(expectation, query.sql)
    }

    @Test
    fun `one_to_many one_to_many`() {

        val relation = Relation(
            "Service",
            "Employee",
            "subordinatesromemployees",
            "manager",
            Relation.Type.ONE_TO_MANY,
            "employees.subordinates"
        )
        val query = RelationQueryBuilder.createQuery(relation, entity)

        val expectation = "SELECT * FROM Employee AS T_FINAL WHERE " +
            "EXISTS ( SELECT * FROM Employee AS T1 WHERE " +
            "T_FINAL.__managerKey = T1.__KEY " +
            "AND T1.__serviceKey = ${entity.__KEY} )"
        Assert.assertEquals(expectation, query.sql)
    }

    @Test
    fun `one_to_many many_to_one`() {

        val relation = Relation(
            "Service",
            "Employee",
            "managerfromemployees",
            "subordinates",
            Relation.Type.ONE_TO_MANY,
            "employees.manager"
        )
        val query = RelationQueryBuilder.createQuery(relation, entity)

        val expectation = "SELECT * FROM Employee AS T_FINAL WHERE " +
            "EXISTS ( SELECT * FROM Employee AS T1 WHERE " +
            "T_FINAL.__KEY = T1.__managerKey " +
            "AND T1.__serviceKey = ${entity.__KEY} " +
            ")"
        Assert.assertEquals(expectation, query.sql)
    }

    @Test
    fun `many_to_one one_to_many`() {

        val relation = Relation(
            "Employee",
            "Employee",
            "employeesfromservice",
            "service",
            Relation.Type.ONE_TO_MANY,
            "service.employees"
        )
        val query = RelationQueryBuilder.createQuery(relation, entity)

        val expectation = "SELECT * FROM Employee AS T_FINAL WHERE EXISTS ( " +
            "SELECT * FROM Service AS T1 WHERE T_FINAL.__serviceKey = T1.__KEY " +
            "AND T1.__KEY = -1" +
            " )"
        Assert.assertEquals(expectation, query.sql)
    }

    @Test
    fun `many_to_one many_to_one`() {

        val relation = Relation(
            "Employee",
            "Employee",
            "managerfromservice",
            "serviceManaged",
            Relation.Type.MANY_TO_ONE,
            "service.manager"
        )
        val query = RelationQueryBuilder.createQuery(relation, entity)

        val expectation = "SELECT * FROM Employee AS T_FINAL WHERE EXISTS ( " +
            "SELECT * FROM Service AS T1 WHERE T_FINAL.__KEY = T1.__managerKey " +
            "AND T1.__KEY = -1" +
            " ) LIMIT 1"
        Assert.assertEquals(expectation, query.sql)
    }

    @Test
    fun many_to_one() {

        val relation = Relation("Employee", "Service", "service", "employees", Relation.Type.MANY_TO_ONE)
        val query = RelationQueryBuilder.createQuery(relation, entity)

        val expectation = "SELECT * FROM Service AS T_FINAL WHERE T_FINAL.__KEY = -1 LIMIT 1"
        Assert.assertEquals(expectation, query.sql)
    }

    @Test
    fun one_to_many() {

        val relation = Relation("Service", "Employee", "employees", "service", Relation.Type.ONE_TO_MANY)
        val query = RelationQueryBuilder.createQuery(relation, entity)

        val expectation = "SELECT * FROM Employee AS T_FINAL WHERE T_FINAL.__serviceKey = ${entity.__KEY}"
        Assert.assertEquals(expectation, query.sql)
    }
}

fun <T> any(type: Class<T>): T {
    Mockito.any(type)
    return uninitialized()
}

@Suppress("UNCHECKED_CAST")
private fun <T> uninitialized(): T = null as T
