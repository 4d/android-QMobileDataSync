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
    fun getRelationsLiveData(source: String, entity: EntityModel): Map<Relation, LiveData<List<RoomData>>> {
        val map = mutableMapOf<Relation, LiveData<List<RoomData>>>()

        entity.__KEY?.let { key ->
            getOneToManyRelations(source).forEach { relation ->
                map[relation] = BaseApp.daoProvider.getDao(relation.dest).getAll(getOneToManyQuery(relation, key))
            }
        }
        getManyToOneRelations(source).forEach { relation ->
            BaseApp.genericRelationHelper.getRelationId(source, relation.name, entity)?.let { relationId ->
                map[relation] = BaseApp.daoProvider.getDao(relation.dest).getAll(getManyToOneQuery(relation, relationId))
            }
        }
        return map
    }

    private fun getOneToManyQuery(relation: Relation, key: String): SimpleSQLiteQuery {
        return SimpleSQLiteQuery(
            "SELECT * FROM ${relation.dest} AS T1 WHERE " +
                    "EXISTS ( SELECT * FROM ${relation.source} AS T2 WHERE " +
                    "T1.__${relation.inverse}Key = $key )"
        )
    }

    private fun getManyToOneQuery(relation: Relation, relationId: String): SimpleSQLiteQuery {
        return SimpleSQLiteQuery(
            "SELECT * FROM ${relation.dest} AS T1 WHERE T1.__KEY = $relationId"
        )
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
