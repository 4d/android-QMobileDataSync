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

    private fun getManyToOneRelations(source: String): List<Relation> =
        BaseApp.runtimeDataHolder.relations.filter { it.source == source && it.type == RelationTypeEnum.MANY_TO_ONE }

    private fun getOneToManyRelations(source: String): List<Relation> =
        BaseApp.runtimeDataHolder.relations.filter { it.source == source && it.type == RelationTypeEnum.ONE_TO_MANY }

    fun getDest(source: String, name: String): String? =
        BaseApp.runtimeDataHolder.relations.firstOrNull { it.source == source && it.name == name }?.dest

    fun getInverse(source: String, name: String): String? =
        BaseApp.runtimeDataHolder.relations.firstOrNull { it.source == source && it.name == name }?.inverse

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
        return if (relation.type == RelationTypeEnum.ONE_TO_MANY)
            createOneToManyQuery(relation, entity)
        else
            createManyToOneQuery(relation, entity)
    }

    @Suppress("ReturnCount")
    private fun createOneToManyQuery(relation: Relation, entity: EntityModel): SimpleSQLiteQuery? {

        if (relation.name.contains(".")) {
            val manyToOne = relation.name.split(".")[0]
            val oneToMany = relation.name.split(".")[1]

            val manyToOneDest = getDest(relation.source, manyToOne)

            BaseApp.genericRelationHelper.getRelationId(relation.source, manyToOne, entity)?.let { relationId ->

                return SimpleSQLiteQuery(
                    "SELECT * FROM ${relation.dest} AS T1 WHERE " +
                        "EXISTS ( SELECT * FROM $manyToOneDest AS T2 WHERE " +
                        "T1.__${relation.inverse}Key = $relationId AND T2.__KEY = $relationId  )"
                )
            }
        } else {

            return SimpleSQLiteQuery(
                "SELECT * FROM ${relation.dest} AS T1 WHERE " +
                    "EXISTS ( SELECT * FROM ${relation.source} AS T2 WHERE " +
                    "T1.__${relation.inverse}Key = ${entity.__KEY} )"
            )
        }
        return null
    }

//    Relation("Employee", "Employee", "managerfromservice", "serviceManaged.employees",
//    RelationTypeEnum.MANY_TO_ONE, "service.manager"),

    @Suppress("ReturnCount")
    private fun createManyToOneQuery(relation: Relation, entity: EntityModel): SimpleSQLiteQuery? {
        if (relation.name.contains(".")) {
            val manyToOne = relation.name.split(".")[0]
            val oneToMany = relation.name.split(".")[1]

            val manyToOneDest = getDest(relation.source, manyToOne)

            BaseApp.genericRelationHelper.getRelationId(relation.source, manyToOne, entity)?.let { relationId ->

                return SimpleSQLiteQuery(
                    "SELECT * FROM ${relation.dest} AS T1 WHERE " +
                        "EXISTS ( SELECT * FROM $manyToOneDest AS T2 WHERE " +
                        "T1.__KEY = T2.__${oneToMany}Key AND T2.__KEY = $relationId ) LIMIT 1"
                )
            }
        } else {

            BaseApp.genericRelationHelper.getRelationId(relation.source, relation.name, entity)?.let { relationId ->

                return SimpleSQLiteQuery("SELECT * FROM ${relation.dest} AS T1 WHERE T1.__KEY = $relationId LIMIT 1")
            }
        }
        return null
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
