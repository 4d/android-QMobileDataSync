/*
 * Created by Quentin Marciset on 5/5/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmarciset.androidmobiledatasync.utils

import android.app.Application
import com.qmarciset.androidmobileapi.model.entity.EntityModel
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

object RelationHelper {

    /**
     * Retrieve the related entity from its relation name. This method uses reflection
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getRelatedEntity(entity: EntityModel, relationName: String): T? {
        val property =
            entity::class.memberProperties.first { it.name == relationName } as KProperty1<EntityModel, *>
        return property.get(entity) as T?
    }

    /**
     * Checks if the given type is among tableNames list and therefore is a relation
     */
    fun <T> isRelation(
        property: KProperty1<T, *>,
        application: Application,
        tableNames: List<String>
    ): Boolean {
        val type = property.toString().split(":")[1].removeSuffix("?")
        if (type.contains(application.packageName)) {
            val customType =
                type.replace(" ${application.packageName}.data.model.entity.custom.", "")
            if (customType in tableNames) {
                return true
            }
        }
        return false
    }
}
