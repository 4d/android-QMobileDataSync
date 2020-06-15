/*
 * Created by Quentin Marciset on 27/5/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmarciset.androidmobiledatasync.relation

import android.app.Application
import com.qmarciset.androidmobileapi.model.entity.Entities
import com.qmarciset.androidmobileapi.model.entity.EntityModel
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

object RelationHelper {

    /**
     * Retrieve the related type from its relation name. This method uses reflection
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getRelatedEntity(entity: EntityModel, relationName: String): T? {
        val property =
            entity::class.memberProperties.first { it.name == relationName } as KProperty1<EntityModel, *>
        return property.get(entity) as T?
    }

    /**
     * Checks if the given type is among tableNames list and therefore is a many-to-one relation.
     * If it is a many-to-one relation, it returns the related Class name
     */
    fun <T> isManyToOneRelation(
        property: KProperty1<T, *>,
        application: Application,
        tableNames: List<String>
    ): String? {
        val type = property.toString().split(":")[1].removeSuffix("?")
        if (type.contains(application.packageName)) {
            val customType =
                type.replace(" ${application.packageName}.data.model.entity.custom.", "")
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
    fun <T> isOneToManyRelation(
        property: KProperty1<T, *>
    ): String? {
        val type = property.toString().split(":")[1].removeSuffix("?")
        if (type.contains(Entities::class.java.canonicalName.toString())) {
            return Entities::class.simpleName
        }
        return null
    }
}
