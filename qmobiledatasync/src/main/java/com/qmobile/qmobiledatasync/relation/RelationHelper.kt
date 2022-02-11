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
import com.qmobile.qmobiledatastore.data.RoomData
import com.qmobile.qmobiledatastore.data.RoomRelation
import com.qmobile.qmobiledatasync.app.BaseApp
import org.json.JSONObject

object RelationHelper {

    /**
     * Retrieve the related type from its relation name. This method uses reflection
     */
    fun getRelatedEntity(entityJsonString: String, name: String): JSONObject? =
        JSONObject(entityJsonString).getSafeObject(name)

    fun getRelations(source: String): List<Relation> =
        BaseApp.runtimeDataHolder.relations.filter { it.source == source }

    fun getManyToOneRelations(source: String): List<Relation> =
        BaseApp.runtimeDataHolder.relations.filter { it.source == source && it.type == RelationTypeEnum.MANY_TO_ONE }

    fun getOneToManyRelations(source: String): List<Relation> =
        BaseApp.runtimeDataHolder.relations.filter { it.source == source && it.type == RelationTypeEnum.ONE_TO_MANY }

    fun getDest(source: String, name: String): String? =
        BaseApp.runtimeDataHolder.relations.firstOrNull { it.source == source && it.name == name }?.dest

    fun getInverse(source: String, name: String): String? =
        BaseApp.runtimeDataHolder.relations.firstOrNull { it.source == source && it.name == name }?.inverse

    /**
     * Provides the relation map extracted from an entity
     */
    fun getRelationsLiveData(source: String, entity: EntityModel): Map<Relation, LiveData<RoomRelation>> {
        val map = mutableMapOf<Relation, LiveData<RoomRelation>>()

        entity.__KEY?.let { key ->
            getOneToManyRelations(source).forEach { relation ->
                val relationDao = BaseApp.daoProvider.getRelationDao(relation.dest, source, relation.inverse)
                map[relation] = relationDao.getRelation(key)
            }
        }
        getManyToOneRelations(source).forEach { relation ->
            BaseApp.genericRelationHelper.getRelationId(source, relation.name, entity)?.let { relationId ->
                val relationDao = BaseApp.daoProvider.getRelationDao(source, relation.dest, relation.name)
                map[relation] = relationDao.getRelation(relationId)
            }
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
}
