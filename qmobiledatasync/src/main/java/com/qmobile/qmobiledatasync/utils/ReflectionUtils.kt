/*
 * Created by qmarciset on 11/2/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.utils

import android.app.Application
import com.fasterxml.jackson.annotation.JsonProperty
import com.qmobile.qmobileapi.model.entity.Entities
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.relation.Relation
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation

object ReflectionUtils {

    /**
     * Returns list of table properties as a String, separated by commas, without EntityModel
     * inherited properties
     */
    fun <T : EntityModel> getPropertyListString(
        tableName: String,
        application: Application
    ): String {

        val entityModelProperties = EntityModel::class.declaredMemberProperties.map { it.name }
        val tableNames = BaseApp.genericTableHelper.tableNames()

        val reflectedProperties = BaseApp.genericTableHelper.getReflectedProperties<T>(tableName)

        val propertyList = reflectedProperties.first.toList()
        val constructorParameters = reflectedProperties.second

        val names = mutableListOf<String>()
        propertyList.forEach eachProperty@{ property ->

            val propertyName: String = property.name

            val serializedName: String? = constructorParameters?.find { it.name == propertyName }
                ?.findAnnotation<JsonProperty>()?.value

            var name: String = serializedName ?: propertyName

            if (getManyToOneRelation(property, application, tableNames) != null ||
                getOneToManyRelation(property, application, tableNames) != null
            ) {
                name += Relation.SUFFIX
            }
            names.add(name)
        }

        val difference = names.toSet().minus(entityModelProperties.toSet())
        return difference.toString().filter { it !in "[]" }
    }

    /**
     * Checks if the given type is among tableNames list and therefore is a many-to-one relation.
     * If it is a many-to-one relation, it returns the related Class name
     */
    fun <T : EntityModel> getManyToOneRelation(
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
    fun <T : EntityModel> getOneToManyRelation(
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
