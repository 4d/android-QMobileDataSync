/*
 * Created by Quentin Marciset on 27/5/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.relation

import androidx.databinding.ViewDataBinding
import androidx.lifecycle.LiveData
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobileapi.utils.getSafeObject
import com.qmobile.qmobileapi.utils.getSafeString
import com.qmobile.qmobileapi.utils.retrieveJSONObject
import com.qmobile.qmobiledatastore.data.RoomData
import com.qmobile.qmobiledatasync.app.BaseApp

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

    fun getRelationId(jsonString: String, relationName: String, fetchedFromRelation: Boolean): String? =
        if (fetchedFromRelation)
            retrieveJSONObject(jsonString)?.getSafeObject(relationName)?.getSafeObject("__deferred")
                ?.getSafeString("__KEY")
        else
            retrieveJSONObject(jsonString)?.getSafeObject(relationName)?.getSafeString("__KEY")
}
