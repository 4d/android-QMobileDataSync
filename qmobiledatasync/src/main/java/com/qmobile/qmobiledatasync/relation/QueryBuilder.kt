/*
 * Created by qmarciset on 28/2/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.relation

import androidx.sqlite.db.SimpleSQLiteQuery
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobiledatasync.app.BaseApp
import timber.log.Timber
import java.lang.StringBuilder

object QueryBuilder {

    fun createQuery(relation: Relation, entity: EntityModel): SimpleSQLiteQuery {

        val path = relation.path.ifEmpty { relation.name }
        val newPath = RelationHelper.unAliasPath(path, relation.source)
        Timber.d("newPath: $newPath")

        val pathList = newPath.split(".")

        val hasDepth = pathList.size > 1

        val builder = StringBuilder("SELECT * FROM ${relation.dest} AS T_FINAL WHERE ")

        if (hasDepth) {
            builder.append(depthRelation(relation, pathList, 0, entity))
            repeat(builder.count { it == '(' }) {
                builder.append(" )")
            }
        } else {
            builder.append(endCondition(relation, entity, true))
        }

        if (relation.type == Relation.Type.MANY_TO_ONE) {
            builder.append(" LIMIT 1")
        }

        return SimpleSQLiteQuery(builder.toString())
    }

    private fun depthRelation(parent: Relation, path: List<String>, depth: Int, entity: EntityModel): String {

        val source = if (depth == 0) parent.source else parent.dest
        val relation = RelationHelper.getRelation(source, path[depth])

        val query = StringBuilder()

        when (depth) {
            0 -> { // first
                query.append("EXISTS ( ")
                query.append(depthRelation(relation, path, depth + 1, entity))
                query.append(" AND ")
                query.append(endCondition(relation, entity, false))
            }
            path.size - 1 -> { // last
                query.append(partQuery(relation, depth, true))
            }
            else -> {
                query.append(depthRelation(relation, path, depth + 1, entity))
                query.append(" AND EXISTS ( ")
                query.append(partQuery(relation, depth, false))
            }
        }

        return query.toString()
    }

    private fun partQuery(relation: Relation, depth: Int, isFinal: Boolean): String {
        val tName = if (isFinal) "T_FINAL" else "T${depth + 1}"
        return if (relation.type == Relation.Type.MANY_TO_ONE)
            "SELECT * FROM ${relation.source} AS T$depth WHERE $tName.__KEY = T$depth.__${relation.name}Key"
        else
            "SELECT * FROM ${relation.source} AS T$depth WHERE $tName.__${relation.inverse}Key = T$depth.__KEY"
    }

    private fun endCondition(relation: Relation, entity: EntityModel, isFinal: Boolean): String {
        val tName = if (isFinal) "T_FINAL" else "T1"
        return if (relation.type == Relation.Type.MANY_TO_ONE) {
            val relationId = BaseApp.genericRelationHelper.getRelationId(relation.source, relation.name, entity) ?: "-1"
            "$tName.__KEY = $relationId"
        } else {
            "$tName.__${relation.inverse}Key = ${entity.__KEY}"
        }
    }
}
