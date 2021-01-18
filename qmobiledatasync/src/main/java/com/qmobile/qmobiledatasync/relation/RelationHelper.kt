/*
 * Created by Quentin Marciset on 27/5/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.relation

import android.app.Application
import com.qmobile.qmobileapi.model.entity.Entities
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobileapi.utils.getSafeObject
import org.json.JSONObject
import kotlin.reflect.KProperty1

object RelationHelper {

    /**
     * Retrieve the related type from its relation name. This method uses reflection
     */
    fun getRelatedEntity(entityJsonString: String, relationName: String): JSONObject? {
        val relationJsonObject = JSONObject(entityJsonString)
        return relationJsonObject.getSafeObject(relationName)
    }

    /**
     * Checks if the given type is among tableNames list and therefore is a many-to-one relation.
     * If it is a many-to-one relation, it returns the related Class name
     */
    fun <T : EntityModel> isManyToOneRelation(
        property: KProperty1<T, *>,
        application: Application,
        tableNames: List<String>
    ): String? {
        val type = property.toString().split(":")[1].removeSuffix("?")
        if (type.contains(application.packageName)) {
            val customType =
                type.replace(" ${application.packageName}.data.model.entity.", "")
            if (customType in tableNames) {
                return customType
            }
        }
        return null
    }

    /**
     * Checks if the given type is Entities and therefore is a one-to-many relation.
     * If it is a one-to-many relation, it returns the related Class name
     */
    fun <T : EntityModel> isOneToManyRelation(
        property: KProperty1<T, *>,
        application: Application,
        tableNames: List<String>
    ): String? {
        val type = property.toString().split(":")[1].removeSuffix("?")
        val entitiesPrefix = " ${Entities::class.java.canonicalName}"
        if (type.contains(entitiesPrefix)) {

            val canonicalType = type.removePrefix(entitiesPrefix).filter { it !in "<>?" }
            if (canonicalType.contains(application.packageName)) {
                val customType =
                    canonicalType.replace(
                        "${application.packageName}.data.model.entity.",
                        ""
                    )
                if (customType in tableNames) {
                    return customType
                }
            }
        }
        return null
    }
}