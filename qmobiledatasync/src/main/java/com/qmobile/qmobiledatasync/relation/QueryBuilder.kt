/*
 * Created by qmarciset on 28/2/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.relation

import androidx.sqlite.db.SimpleSQLiteQuery
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobiledatasync.app.BaseApp
import java.lang.StringBuilder

object QueryBuilder {

    fun createQuery(relation: Relation, entity: EntityModel): SimpleSQLiteQuery {

        val path = relation.path.ifEmpty { relation.name }
        val pathList = path.split(".")
        val depth = pathList.size
        val hasDepth = pathList.size > 1

        val builder = StringBuilder("SELECT * FROM ${relation.dest} AS T${depth} WHERE ")

        if (hasDepth) {
            builder.append(depthRelation(relation, pathList, 0, entity))
            repeat(builder.count { it == '(' }) {
                builder.append(" )")
            }
        } else {
            builder.append(endCondition(relation, entity))
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
                query.append(endCondition(relation, entity))
            }
            path.size - 1 -> { // last
                query.append(partQuery(relation, depth))
            }
            else -> {
                query.append(depthRelation(relation, path, depth + 1, entity))
                query.append(" AND EXISTS ( ")
                query.append(partQuery(relation, depth))
            }
        }

        return query.toString()
    }

    private fun partQuery(relation: Relation, depth: Int): String =
        if (relation.type == Relation.Type.MANY_TO_ONE)
            "SELECT * FROM ${relation.source} AS T${depth} WHERE T${depth + 1}.__KEY = T${depth}.__${relation.name}Key"
        else
            "SELECT * FROM ${relation.source} AS T${depth} WHERE T${depth + 1}.__${relation.inverse}Key = T${depth}.__KEY"

    private fun endCondition(relation: Relation, entity: EntityModel): String {
        return if (relation.type == Relation.Type.MANY_TO_ONE) {
            val relationId = BaseApp.genericRelationHelper.getRelationId(relation.source, relation.name, entity) ?: "-1"
            "T1.__KEY = $relationId"
        } else {
            "T1.__${relation.inverse}Key = ${entity.__KEY}"
        }
    }
}
