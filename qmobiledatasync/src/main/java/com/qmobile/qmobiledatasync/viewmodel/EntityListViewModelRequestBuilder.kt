/*
 * Created by Quentin Marciset on 26/6/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.viewmodel

import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobileapi.utils.GLOBALSTAMP_PROPERTY
import com.qmobile.qmobileapi.utils.Query
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.relation.Relation
import com.qmobile.qmobiledatasync.relation.RelationHelper
import com.qmobile.qmobiledatasync.relation.RelationHelper.withoutAlias
import com.qmobile.qmobiledatasync.utils.fieldAdjustment
import org.json.JSONObject
import kotlin.math.max

/**
 * Returns predicate for requests with __GlobalStamp
 */
fun <T : EntityModel> EntityListViewModel<T>.buildPredicate(): String? {
    var gs = globalStamp.value.stampValue

    val isDumpedTable = BaseApp.runtimeDataHolder.dumpedTables.contains(getAssociatedTableName())

    if (isDumpedTable) {
        gs = max(gs, BaseApp.runtimeDataHolder.initialGlobalStamp)
    }

    var predicate = if (gs > 0) "\"$GLOBALSTAMP_PROPERTY >= $gs\"" else ""

    val query: String = BaseApp.runtimeDataHolder.tableInfo[getAssociatedTableName()]?.query ?: ""
    if (query.isNotEmpty()) {
        predicate = if (predicate.isEmpty()) {
            "\"$query\""
        } else {
            predicate.dropLast(1) + " AND ($query)\""
        }
    }
    return predicate.ifEmpty { null }
}

fun <T : EntityModel> EntityListViewModel<T>.buildPostRequestBody(): JSONObject {
    return JSONObject().apply {
        // Adding properties
        val properties = BaseApp.runtimeDataHolder.tableInfo[getAssociatedTableName()]?.fields?.values
        properties?.forEach { property ->
            addProperty(getAssociatedTableName(), property)
        }
    }
}

private fun JSONObject.addProperty(tableName: String, property: String) {
    when {
        !property.endsWith(Relation.SUFFIX) -> {
            put(property, true)
        }
        BaseApp.runtimeDataHolder.relationAvailable -> {
            RelationHelper.getRelations(tableName).withoutAlias()
                .find { it.name == property.removeSuffix(Relation.SUFFIX).fieldAdjustment() }?.let {
                    put(property.removeSuffix(Relation.SUFFIX), buildRelationQueryAndProperties(it.dest))
                }
        }
    }
}

private fun buildRelationQueryAndProperties(dest: String): JSONObject {
    return JSONObject().apply {
        val relationProperties = BaseApp.runtimeDataHolder.tableInfo[dest]?.fields?.values
        relationProperties?.forEach { relationProperty ->
            put(relationProperty.removeSuffix(Relation.SUFFIX), true)
        }
        val query: String = BaseApp.runtimeDataHolder.tableInfo[dest]?.query ?: ""
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
