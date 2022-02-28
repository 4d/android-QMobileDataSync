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
        val isAlias = depth > 1

        val relationId = BaseApp.genericRelationHelper.getRelationId(relation.source, path.split(".")[0], entity) ?: ""

        val builder = StringBuilder("SELECT * FROM ${relation.dest} AS T${depth} WHERE ")

        when {
            isAlias -> {

                if (relation.type == Relation.Type.MANY_TO_ONE) {
                    builder.append(depthRelation(relation, pathList, 0, relationId))
                } else {

                }

            }
            relation.type == Relation.Type.MANY_TO_ONE -> {

                // Relation("Employee", "Service", "service", "employees", Relation.Type.MANY_TO_ONE),
                builder.append("T1.__KEY = $relationId")
            }
            relation.type == Relation.Type.ONE_TO_MANY -> {

                // Relation("Service", "Employee", "employees", "service", Relation.Type.ONE_TO_MANY),
                builder.append("T1.__${relation.inverse}Key = ${entity.__KEY}")
            }
        }

        if (relation.type == Relation.Type.MANY_TO_ONE)
            builder.append(" LIMIT 1")

        return SimpleSQLiteQuery(builder.toString())
    }

    private fun depthRelation(parentRelation: Relation, path: List<String>, depth: Int, relationId: String): String {

        val relation = RelationHelper.getRelation(parentRelation.source, path[depth])

        val query = StringBuilder()

        when (depth) {
            0 -> { // first
                query.append("EXISTS ( ")
                query.append(depthRelation(relation, path, depth + 1, relationId))
                query.append(" )")
            }
            path.size - 1 -> { // last
                if (relation.type == Relation.Type.MANY_TO_ONE) {
                    query.append(ManyToOne.addPart(relation, depth))
                    if (depth == 1) {
                        query.append(" AND T1.__KEY = $relationId")
                    }
                } else {
                    query.append(ManyToOne.addPart(relation, depth))
                    if (depth == 1) {
                        query.append(" AND T1.__KEY = $relationId")
                    }
                }
            }
            else -> {
                query.append(depthRelation(relation, path, depth + 1, relationId))
                query.append(" AND EXISTS ( ")
                query.append(ManyToOne.addPart(relation, depth))
                query.append(" )")
            }
        }
        return query.toString()
    }


    object ManyToOne {

        fun addPart(relation: Relation, depth: Int): String =
            "SELECT * FROM ${relation.source} AS T${depth} WHERE T${depth + 1}.__KEY = T${depth}.__${relation.name}Key"

    }

}
