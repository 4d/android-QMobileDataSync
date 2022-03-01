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
import java.lang.StringBuilder

object RelationHelper {

    fun getRelations(source: String): List<Relation> =
        BaseApp.runtimeDataHolder.relations.filter { it.source == source }

    fun getRelation(source: String, name: String): Relation =
        BaseApp.runtimeDataHolder.relations.first { it.source == source && it.name == name }

    private fun getManyToOneRelations(source: String): List<Relation> =
        BaseApp.runtimeDataHolder.relations.filter { it.source == source && it.type == Relation.Type.MANY_TO_ONE }

    private fun getOneToManyRelations(source: String): List<Relation> =
        BaseApp.runtimeDataHolder.relations.filter { it.source == source && it.type == Relation.Type.ONE_TO_MANY }

    fun List<Relation>.withoutAlias() = this.filter { it.path.isEmpty() }

    /**
     * Provides the relation map extracted from an entity
     */
    fun getRelationsLiveDataMap(source: String, entity: EntityModel): Map<Relation, LiveData<List<RoomData>>> {
        val map = mutableMapOf<Relation, LiveData<List<RoomData>>>()

        getRelations(source).forEach { relation ->
            val query = QueryBuilder.createQuery(relation, entity)
            map[relation] = BaseApp.daoProvider.getDao(relation.dest).getAll(query)
        }
        return map
    }

    /*private fun createQuery(relation: Relation, entity: EntityModel): SimpleSQLiteQuery? {
        val path = relation.path.ifEmpty { relation.name }

        val builder = StringBuilder("SELECT * FROM ${relation.dest} AS T1 WHERE ")

        if (path.contains(".")) {

            val firstRelation = getRelation(relation.source, path.split(".")[0])
            val secondRelation = getRelation(firstRelation.source, path.split(".")[1])
            val thirdRelation = getRelation(secondRelation.source, path.split(".")[2])

            val relationId =
                BaseApp.genericRelationHelper.getRelationId(firstRelation.source, firstRelation.name, entity)
                    ?: return null


            if (firstRelation.type == Relation.Type.MANY_TO_ONE) {
                if (secondRelation.type == Relation.Type.MANY_TO_ONE) {
                    if (thirdRelation.type == Relation.Type.MANY_TO_ONE) {
                        // Relation("Service", "Employee", "", "serviceManaged", Relation.Type.MANY_TO_ONE, "manager.service.manager"),
                        val sql = SimpleSQLiteQuery(
                            "SELECT * FROM ${relation.dest} AS T3 WHERE EXISTS ( " +
                                    "SELECT * FROM ${thirdRelation.source} AS T2 WHERE T3.__KEY = T2.__${thirdRelation.name}Key " +
                                    "AND EXISTS ( " +
                                    "SELECT * FROM ${secondRelation.source} AS T1 WHERE T2.__KEY = T1.__${secondRelation.name}Key " +
                                    "AND T1.__KEY = $relationId " +
                                    " ) ) LIMIT 1"
                        )

                        // Relation("Employee", "Employee", "managerfromservice", "serviceManaged", Relation.Type.MANY_TO_ONE, "service.manager")
                        return SimpleSQLiteQuery(
                            "SELECT * FROM ${relation.dest} AS T2 WHERE EXISTS ( " +
                                    "SELECT * FROM ${secondRelation.source} AS T1 WHERE T2.__KEY = T1.__${secondRelation.name}Key " +
                                    "AND T1.__KEY = $relationId" +
                                    " ) LIMIT 1"
                        )
                    } else { // thirdRelation.type = Relation.Type.ONE_T0_MANY
                        // Relation("Employee", "Service", "", "manager", Relation.Type.ONE_TO_MANY, "service.manager.serviceManaged"),
                        SimpleSQLiteQuery(
                            "SELECT * FROM ${relation.dest} AS T3 WHERE EXISTS ( " +
                                    "SELECT * FROM ${thirdRelation.source} AS T2 WHERE T3.__${thirdRelation.inverse}Key = T2.__KEY " +
                                    "AND EXISTS ( " +
                                    "SELECT * FROM ${secondRelation.source} AS T1 WHERE T2.__KEY = T1.__${secondRelation.name}Key " +
                                    "AND T1.__KEY = $relationId " +
                                    " ) )"
                        )
                    }
                } else { // secondRelation.type = Relation.Type.ONE_T0_MANY
                    if (thirdRelation.type == Relation.Type.MANY_TO_ONE) {
                        // Relation("Employee", "Service", "", "employees", Relation.Type.ONE_TO_MANY, "service.employees.service"),
                        SimpleSQLiteQuery(
                            "SELECT * FROM ${relation.dest} AS T3 WHERE EXISTS ( " +
                                    "SELECT * FROM ${thirdRelation.source} AS T2 WHERE T2.__${thirdRelation.name}Key = T3.__KEY " +
                                    "AND EXISTS ( " +
                                    "SELECT * FROM ${secondRelation.source} AS T1 WHERE T2.__${secondRelation.inverse}Key = T1.__KEY " +
                                    "AND T1.__KEY = $relationId " +
                                    " ) )"
                        )
                    } else { // thirdRelation.type = Relation.Type.ONE_T0_MANY
                        // Relation("Employee", "Service", "", "manager", Relation.Type.ONE_TO_MANY, "service.employees.serviceManaged"),
                        SimpleSQLiteQuery(
                            "SELECT * FROM ${relation.dest} AS T3 WHERE EXISTS ( " +
                                    "SELECT * FROM ${thirdRelation.source} AS T2 WHERE T3.__${thirdRelation.inverse}Key = T2.__KEY " +
                                    "AND EXISTS ( " +
                                    "SELECT * FROM ${secondRelation.source} AS T1 WHERE T2.__${secondRelation.inverse}Key = T1.__KEY " +
                                    "AND T1.__KEY = $relationId " +
                                    " ) )"
                        )
                    }


                    // Relation("Employee", "Employee", "employeesfromservice", "service", Relation.Type.ONE_TO_MANY, "service.employees"),
                    return SimpleSQLiteQuery(
                        "SELECT * FROM ${relation.dest} AS T2 WHERE EXISTS ( " +
                                "SELECT * FROM ${secondRelation.source} AS T1 WHERE T2.__${secondRelation.inverse}Key = T1.__KEY " +
                                "AND T1.__KEY = $relationId" +
                                " )"
                    )
                }
            } else { // firstRelation.type = Relation.Type.ONE_T0_MANY

                if (secondRelation.type == Relation.Type.MANY_TO_ONE) {

                    if (thirdRelation.type == Relation.Type.MANY_TO_ONE) {
                        //    Relation("Service", "Service", "", "employees", Relation.Type.ONE_TO_MANY, "employees.manager.service"),
                        SimpleSQLiteQuery(
                            "SELECT * FROM ${relation.dest} AS T3 WHERE EXISTS ( " +
                                    "SELECT * FROM ${thirdRelation.source} AS T2 WHERE T3.__KEY = T2.__${thirdRelation.name}Key " +
                                    "AND EXISTS ( " +
                                    "SELECT * FROM ${secondRelation.source} AS T1 WHERE T2.__KEY = T1.__${secondRelation.name}Key " +
                                    "AND T1.__${firstRelation.inverse}Key = ${entity.__KEY} " +
                                    " ) )"
                        )
                    } else { // thirdRelation.type = Relation.Type.ONE_T0_MANY
                        //    Relation("Service", "Employee", "", "manager", Relation.Type.ONE_TO_MANY, "employees.manager.subordinates"),
                        SimpleSQLiteQuery(
                            "SELECT * FROM ${relation.dest} AS T3 WHERE EXISTS ( " +
                                    "SELECT * FROM ${thirdRelation.source} AS T2 WHERE T3.__${thirdRelation.inverse}Key = T2.__KEY  " +
                                    "AND EXISTS ( " +
                                    "SELECT * FROM ${secondRelation.source} AS T1 WHERE T2.__KEY = T1.__${secondRelation.name}Key " +
                                    "AND T1.__${firstRelation.inverse}Key = ${entity.__KEY}" +
                                    " ) )"
                        )
                    }

                    // Relation("Service", "Employee", "managerfromemployees", "subordinates", Relation.Type.ONE_TO_MANY, "employees.manager"),
                    return SimpleSQLiteQuery(
                        "SELECT * FROM ${relation.dest} AS T2 WHERE " +
                                "EXISTS ( SELECT * FROM ${secondRelation.source} AS T1 WHERE " +
                                "T2.__KEY = T1.__${relation.inverse}Key " +
                                "AND T1.__${firstRelation.inverse}Key = ${entity.__KEY} " +
                                ")"
                    )
                } else { // secondRelation.type = Relation.Type.ONE_T0_MANY

                    if (thirdRelation.type == Relation.Type.MANY_TO_ONE) {
                        //    Relation("Service", "Service", "", "employees", Relation.Type.ONE_TO_MANY, "employees.subordinates.service"),
                        SimpleSQLiteQuery(
                            "SELECT * FROM ${relation.dest} AS T3 WHERE EXISTS ( " +
                                    "SELECT * FROM ${thirdRelation.source} AS T2 WHERE T3.__KEY = T2.__${thirdRelation.name}Key " +
                                    "AND EXISTS ( " +
                                    "SELECT * FROM ${secondRelation.source} AS T1 WHERE T2.__${secondRelation.inverse}Key = T1.__KEY " +
                                    "AND T1.__${firstRelation.inverse}Key = ${entity.__KEY} " +
                                    " ) )"
                        )
                    } else { // thirdRelation.type = Relation.Type.ONE_T0_MANY
                        // Relation("Service", "Employee", "", "manager", Relation.Type.ONE_TO_MANY, "employees.subordinates.employees")
                        SimpleSQLiteQuery(
                            "SELECT * FROM ${relation.dest} AS T3 WHERE EXISTS ( " +
                                    "SELECT * FROM ${thirdRelation.source} AS T2 WHERE T3.__${thirdRelation.inverse}Key = T2.__KEY " +
                                    "AND EXISTS ( " +
                                    "SELECT * FROM ${secondRelation.source} AS T1 WHERE T2.__${secondRelation.inverse}Key = T1.__KEY" +
                                    "AND T1.__${firstRelation.inverse}Key = ${entity.__KEY} " +
                                    " ) )"
                        )
                    }

                    // Relation("Service", "Employee", "subordinatesromemployees", "manager", Relation.Type.ONE_TO_MANY, "employees.subordinates"),
                    return SimpleSQLiteQuery(
                        "SELECT * FROM ${relation.dest} AS T2 WHERE " +
                                "EXISTS ( SELECT * FROM ${secondRelation.source} AS T1 WHERE " +
                                "T2.__${secondRelation.inverse}Key = T1.__KEY " +
                                "AND T1.__${firstRelation.inverse}Key = ${entity.__KEY} )"
                    )
                }
            }
        } else {

            if (relation.type == Relation.Type.MANY_TO_ONE) {
                // Relation("Employee", "Service", "service", "employees", Relation.Type.MANY_TO_ONE),
                val relationId = BaseApp.genericRelationHelper.getRelationId(relation.source, path, entity)
                    ?: return null // __serviceKey
                return SimpleSQLiteQuery("SELECT * FROM ${relation.dest} AS T1 WHERE T1.__KEY = $relationId LIMIT 1")

            } else {
                // Relation("Service", "Employee", "employees", "service", Relation.Type.ONE_TO_MANY),
                return SimpleSQLiteQuery(
                    "SELECT * FROM ${relation.dest} AS T1 WHERE T1.__${relation.inverse}Key = ${entity.__KEY}"
                )
            }
        }
    }*/

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
