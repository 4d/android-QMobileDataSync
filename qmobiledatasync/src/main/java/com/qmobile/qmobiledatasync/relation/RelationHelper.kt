/*
 * Created by Quentin Marciset on 27/5/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.relation

import androidx.databinding.ViewDataBinding
import androidx.lifecycle.LiveData
import androidx.sqlite.db.SimpleSQLiteQuery
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobiledatastore.data.RoomData
import com.qmobile.qmobiledatasync.app.BaseApp

object RelationHelper {

    fun getRelations(source: String): List<Relation> =
        BaseApp.runtimeDataHolder.relations.filter { it.source == source }

    fun getRelation(source: String, name: String): Relation? =
        BaseApp.runtimeDataHolder.relations.firstOrNull { it.source == source && it.name == name }

    private fun getManyToOneRelations(source: String): List<Relation> =
        BaseApp.runtimeDataHolder.relations.filter { it.source == source && it.type == Relation.Type.MANY_TO_ONE }

    private fun getOneToManyRelations(source: String): List<Relation> =
        BaseApp.runtimeDataHolder.relations.filter { it.source == source && it.type == Relation.Type.ONE_TO_MANY }

    /**
     * Provides the relation map extracted from an entity
     */
    fun getRelationsLiveDataMap(source: String, entity: EntityModel): Map<Relation, LiveData<List<RoomData>>> {
        val map = mutableMapOf<Relation, LiveData<List<RoomData>>>()

        getRelations(source).forEach { relation ->
            createQuery(relation, entity)?.let { query ->
                map[relation] = BaseApp.daoProvider.getDao(relation.dest).getAll(query)
            }
        }
        return map
    }

    private fun createQuery(relation: Relation, entity: EntityModel): SimpleSQLiteQuery? {
        return if (relation.type == Relation.Type.ONE_TO_MANY)
            createOneToManyQuery(relation, entity)
        else
            createManyToOneQuery(relation, entity)
    }

    // TODO :
//    Relation("Service", "Employee", "", "serviceManaged", Relation.Type.MANY_TO_ONE, "manager.service.manager"),
//    Relation("Employee", "Service", "", "employees", Relation.Type.ONE_TO_MANY, "service.employees.service"),
//    Relation("Employee", "Service", "", "manager", Relation.Type.ONE_TO_MANY, "service.employees.serviceManaged"),
//    Relation("Service", "Service", "", "employees", Relation.Type.ONE_TO_MANY, "employees.manager.service"),
//    Relation("Service", "Employee", "", "manager", Relation.Type.ONE_TO_MANY, "employees.manager.subordinates"),
//    Relation("Service", "Service", "", "employees", Relation.Type.ONE_TO_MANY, "employees.subordinates.service"),
//    Relation("Service", "Employee", "", "manager", Relation.Type.ONE_TO_MANY, "employees.subordinates.employees")

    @Suppress("ReturnCount")
    private fun createOneToManyQuery(relation: Relation, entity: EntityModel): SimpleSQLiteQuery? {

        val path = relation.path.ifEmpty { relation.name }

        if (path.contains(".")) {
            val first = path.split(".")[0]
            val second = path.split(".")[1]

            val firstRelation = getRelation(relation.source, first) ?: return null
            val secondRelation = getRelation(firstRelation.source, second) ?: return null

            // Note : secondRelation.inverse equals relation.inverse

            if (firstRelation.type == Relation.Type.MANY_TO_ONE) {

                val relationId = BaseApp.genericRelationHelper.getRelationId(relation.source, first, entity)
                    ?: return null

                // Relation("Employee", "Employee", "employeesfromservice", "service", Relation.Type.ONE_TO_MANY, "service.employees"),
                return SimpleSQLiteQuery(
                    "SELECT * FROM ${relation.dest} AS T1 WHERE " +
                        "EXISTS ( SELECT * FROM ${firstRelation.source} AS T2 WHERE " +
                        "T1.__${relation.inverse}Key = $relationId AND T2.__KEY = $relationId )"
                )
            } else {

                if (secondRelation.type == Relation.Type.MANY_TO_ONE) {
                    // Relation("Service", "Employee", "managerfromemployees", "subordinates", Relation.Type.ONE_TO_MANY, "employees.manager"),
                    return SimpleSQLiteQuery(
                        "SELECT * FROM ${relation.dest} AS T1 WHERE " +
                            "EXISTS ( SELECT * FROM ${firstRelation.source} AS T2 WHERE " +
                            "T1.__KEY = T2.__${relation.inverse}Key " +
                                "AND T2.__${firstRelation.inverse}Key = ${entity.__KEY} )"
                    )
                } else {
                    // Relation("Service", "Employee", "subordinatesromemployees", "manager", Relation.Type.ONE_TO_MANY, "employees.subordinates"),
                    return SimpleSQLiteQuery(
                        "SELECT * FROM ${relation.dest} AS T1 WHERE " +
                            "EXISTS ( SELECT * FROM ${firstRelation.source} AS T2 WHERE " +
                            "T1.__${secondRelation.inverse}Key = T2.__KEY " +
                                "AND T2.__${firstRelation.inverse}Key = ${entity.__KEY} )"
                    )
                }
            }
        } else {

            return SimpleSQLiteQuery(
                "SELECT * FROM ${relation.dest} AS T1 WHERE " +
                    "EXISTS ( SELECT * FROM ${relation.source} AS T2 WHERE " +
                    "T1.__${relation.inverse}Key = ${entity.__KEY} )"
            )
        }
    }

    @Suppress("ReturnCount")
    private fun createManyToOneQuery(relation: Relation, entity: EntityModel): SimpleSQLiteQuery? {

        val path = relation.path.ifEmpty { relation.name }

        if (path.contains(".")) {

            val first = path.split(".")[0]
            val second = path.split(".")[1]

            val firstRelation = getRelation(relation.source, first) ?: return null

            val relationId = BaseApp.genericRelationHelper.getRelationId(relation.source, first, entity) ?: return null

            // Relation("Employee", "Employee", "managerfromservice", "serviceManaged", Relation.Type.MANY_TO_ONE, "service.manager")
            return SimpleSQLiteQuery(
                "SELECT * FROM ${relation.dest} AS T1 WHERE " +
                    "EXISTS ( SELECT * FROM ${firstRelation.source} AS T2 WHERE " +
                    "T1.__KEY = T2.__${second}Key AND T2.__KEY = $relationId ) LIMIT 1"
            )
        } else {
            val relationId = BaseApp.genericRelationHelper.getRelationId(relation.source, path, entity) ?: return null
            return SimpleSQLiteQuery("SELECT * FROM ${relation.dest} AS T1 WHERE T1.__KEY = $relationId LIMIT 1")
        }
    }

    fun refreshOneToManyNavForNavbarTitle(
        source: String,
        binding: ViewDataBinding,
        entity: EntityModel,
        anyRelatedEntity: RoomData
    ) {
        entity.__KEY?.let { itemId ->
            getOneToManyRelations(source).forEach { relation ->
                if (relation.name.contains(".")) {
                    BaseApp.genericNavigationResolver.setupOneToManyRelationButtonOnClickAction(
                        viewDataBinding = binding,
                        relationName = relation.name,
                        itemId = itemId,
                        entity = entity,
                        anyRelatedEntity = anyRelatedEntity
                    )
                }
            }
        }
    }

    fun setupRelationNavigation(source: String, binding: ViewDataBinding, entity: EntityModel) {
        entity.__KEY?.let { itemId ->
            getOneToManyRelations(source).forEach { relation ->
                BaseApp.genericNavigationResolver.setupOneToManyRelationButtonOnClickAction(
                    viewDataBinding = binding,
                    relationName = relation.name,
                    itemId = itemId,
                    entity = entity
                )
            }
            getManyToOneRelations(source).forEach { relation ->
                BaseApp.genericNavigationResolver.setupManyToOneRelationButtonOnClickAction(
                    viewDataBinding = binding,
                    relationName = relation.name,
                    entity = entity
                )
            }
        }
    }
}
