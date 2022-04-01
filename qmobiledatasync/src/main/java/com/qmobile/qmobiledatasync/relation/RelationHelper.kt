/*
 * Created by Quentin Marciset on 27/5/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.relation

import androidx.databinding.ViewDataBinding
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobileapi.utils.getSafeObject
import com.qmobile.qmobileapi.utils.getSafeString
import com.qmobile.qmobileapi.utils.retrieveJSONObject
import com.qmobile.qmobiledatastore.data.RoomEntity
import com.qmobile.qmobiledatasync.app.BaseApp

object RelationHelper {

    fun getRelations(source: String): List<Relation> =
        BaseApp.runtimeDataHolder.relations.filter { it.source == source }

    fun getRelation(source: String, name: String): Relation =
        BaseApp.runtimeDataHolder.relations.first { it.source == source && it.name == name }

    private fun getRelationNullable(source: String, name: String): Relation? =
        BaseApp.runtimeDataHolder.relations.firstOrNull { it.source == source && it.name == name }

    private fun getManyToOneRelations(source: String): List<Relation> =
        BaseApp.runtimeDataHolder.relations.filter { it.source == source && it.type == Relation.Type.MANY_TO_ONE }

    private fun getOneToManyRelations(source: String): List<Relation> =
        BaseApp.runtimeDataHolder.relations.filter { it.source == source && it.type == Relation.Type.ONE_TO_MANY }

    fun List<Relation>.withoutAlias() = this.filter { it.path.isEmpty() }

    /**
     * Provides the relation map extracted from an entity
     */
    fun getRelationsLiveDataMap(source: String, entity: EntityModel): Map<Relation, Relation.QueryResult> {
        val map = mutableMapOf<Relation, Relation.QueryResult>()

        getRelations(source).forEach { relation ->
            val query = QueryBuilder.createQuery(relation, entity)
//            map[relation] = Relation.QueryResult(query.sql, BaseApp.daoProvider.getDao(relation.dest).getAll(query))
        }
        return map
    }

    fun setupRelationNavigation(source: String, binding: ViewDataBinding, entity: RoomEntity) {
        getOneToManyRelations(source).forEach { relation ->
            BaseApp.genericNavigationResolver.setupOneToManyRelationButtonOnClickAction(
                viewDataBinding = binding,
                relationName = relation.name,
                entity = entity
            )
        }
        getManyToOneRelations(source).forEach { relation ->
            BaseApp.genericNavigationResolver.setupManyToOneRelationButtonOnClickAction(
                viewDataBinding = binding,
                relationName = relation.name,
                entity = entity,
            )
        }
    }

    fun getRelationId(jsonString: String, relationName: String, fetchedFromRelation: Boolean): String? =
        if (fetchedFromRelation)
            retrieveJSONObject(jsonString)?.getSafeObject(relationName)?.getSafeObject("__deferred")
                ?.getSafeString("__KEY")
        else
            retrieveJSONObject(jsonString)?.getSafeObject(relationName)?.getSafeString("__KEY")

    /**
     * Replace path alias by their own path
     * Returns a Pair of <nextTableSource, path>
     */
    private fun checkPath(pathPart: String, source: String, depth: Int): Pair<String?, String> {

        val relation = getRelationNullable(source, pathPart)

        return when {
            relation == null -> Pair(null, "") // case service.Name
            relation.path.isNotEmpty() -> { // case service.alias
                var composedPath = ""
                relation.path.split(".").forEach { name ->
                    val dest = if (depth == 0) relation.source else relation.dest
                    composedPath = if (composedPath.isEmpty())
                        checkPath(name, dest, depth + 1).second
                    else
                        composedPath + "." + checkPath(name, dest, depth + 1).second
                }
                Pair(relation.dest, composedPath)
            }
            else -> Pair(relation.dest, pathPart) // case service
        }
    }

    fun unAliasPath(path: String, source: String): String {
        var nextTableName = source
        var newPath = ""
        path.split(".").forEach {
            val pair = checkPath(it, nextTableName, 0)
            nextTableName = pair.first ?: ""
            newPath = if (newPath.isEmpty())
                pair.second
            else
                newPath + "." + pair.second
        }
        return newPath.removeSuffix(".")
    }

    fun Relation.inverseAliasPath(): String {
        val relationList = mutableListOf<Relation>()
        var nextSource = ""
        unAliasPath(this.path, this.source).split(".").forEachIndexed { index, partName ->
            val currentRelation = if (index == 0)
                getRelation(this.source, partName)
            else
                getRelation(nextSource, partName)
            nextSource = currentRelation.dest
            relationList.add(currentRelation)
        }

        var newPath = ""
        relationList.reversed().forEach { partRelation ->
            newPath = if (newPath.isEmpty())
                partRelation.name
            else
                newPath + "." + partRelation.name
        }
        return newPath
    }
}
