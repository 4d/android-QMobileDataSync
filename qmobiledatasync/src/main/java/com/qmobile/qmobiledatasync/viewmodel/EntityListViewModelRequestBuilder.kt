/*
 * Created by Quentin Marciset on 26/6/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.viewmodel

import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobileapi.model.queries.Query
import com.qmobile.qmobileapi.utils.GLOBALSTAMP_PROPERTY
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.relation.Relation
import org.json.JSONObject
import kotlin.math.max

/**
 * Returns predicate for requests with __GlobalStamp
 */
fun <T : EntityModel> EntityListViewModel<T>.buildPredicate(): String? {

    var gs = globalStamp.value.stampValue

    val isDumpedTable = BaseApp.runtimeDataHolder.dumpedTables.split(", ").contains(getAssociatedTableName())

    if (isDumpedTable)
        gs = max(gs, BaseApp.runtimeDataHolder.initialGlobalStamp)

    var predicate = if (gs > 0) "\"$GLOBALSTAMP_PROPERTY >= $gs\"" else ""

    val query: String = BaseApp.runtimeDataHolder.getQuery(getAssociatedTableName())
    if (query.isNotEmpty()) {
        predicate = if (predicate.isEmpty()) {
            "\"$query\""
        } else {
            predicate.dropLast(1) + " AND ($query)\""
        }
    }
    return if (predicate.isEmpty()) null else predicate
}

fun <T : EntityModel> EntityListViewModel<T>.buildPostRequestBody(): JSONObject {
    return JSONObject().apply {
        // Adding properties
        val properties = BaseApp.runtimeDataHolder.getTableProperty(getAssociatedTableName()).split(", ")
        properties.forEach { property ->
            if (!property.endsWith(Relation.SUFFIX) &&
                !(property.startsWith("__") && property.endsWith("Key"))
            ) {
                put(property, true)
            } // else is a relation
        }

        // Adding relations
        if (BaseApp.runtimeDataHolder.relationAvailable) {
            relations.forEach { relation ->
                put(relation.relationName, buildRelationQueryAndProperties(relation))
            }
        }
    }
}

private fun buildRelationQueryAndProperties(relation: Relation): JSONObject {
    return JSONObject().apply {
        val relationProperties = BaseApp.runtimeDataHolder.getTableProperty(relation.className).split(", ")
        relationProperties.filter { it.isNotEmpty() }.forEach { relationProperty ->
            if (!(relationProperty.startsWith("__") && relationProperty.endsWith("Key"))) {
                put(relationProperty.removeSuffix(Relation.SUFFIX), true)
            }
        }
        val query: String = BaseApp.runtimeDataHolder.getQuery(relation.className)
        if (query.isNotEmpty()) {
            if (BaseApp.sharedPreferencesHolder.userInfo.isEmpty()) {
                // XXX could dev assert here if query contains parameters but no userInfo
                put(Query.QUERY_PROPERTY, query)
            } else {
                put(
                    Query.QUERY_PROPERTY,
                    JSONObject().apply {
                        put(Query.QUERY_STRING_PROPERTY, query)
                        put(
                            Query.SETTINGS,
                            JSONObject().apply {
                                put(Query.PARAMETERS, JSONObject(BaseApp.sharedPreferencesHolder.userInfo))
                            }
                        )
                    }
                )
            }
        }
    }
}
