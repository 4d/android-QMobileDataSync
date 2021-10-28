/*
 * Created by Quentin Marciset on 27/5/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.relation

import android.app.Application
import androidx.lifecycle.LiveData
import com.fasterxml.jackson.annotation.JsonProperty
import com.qmobile.qmobileapi.model.entity.Entities
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobileapi.utils.getSafeObject
import com.qmobile.qmobiledatastore.dao.RelationBaseDao
import com.qmobile.qmobiledatastore.data.RoomRelation
import com.qmobile.qmobiledatasync.app.BaseApp
import org.json.JSONObject
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation

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

    fun addRelation(
        relationName: String,
        relationId: String,
        sourceTableName: String,
        map: MutableMap<String, LiveData<RoomRelation>>,
        relationType: RelationTypeEnum
    ) {
        val relatedTableName =
            BaseApp.genericTableHelper.getRelatedTableName(sourceTableName, relationName)
        val relationDao: RelationBaseDao<RoomRelation> =
            if (relationType == RelationTypeEnum.MANY_TO_ONE)
                BaseApp.daoProvider.getRelationDao(sourceTableName, relatedTableName)
            else
                BaseApp.daoProvider.getRelationDao(relatedTableName, sourceTableName)
        map[relationName] = relationDao.getRelation(relationId)
    }

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

            if (isManyToOneRelation(property, application, tableNames) != null ||
                isOneToManyRelation(property, application, tableNames) != null
            ) {
                name += Relation.SUFFIX
            }
            names.add(name)
        }

        val difference = names.toSet().minus(entityModelProperties.toSet())
        return difference.toString().filter { it !in "[]" }
    }
}
