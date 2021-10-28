/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.utils

import android.app.Application
import androidx.lifecycle.LiveData
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobileapi.network.ApiService
import com.qmobile.qmobiledatastore.data.RoomRelation
import com.qmobile.qmobiledatasync.viewmodel.EntityListViewModel
import com.qmobile.qmobiledatasync.viewmodel.EntityViewModel
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty1

/**
 * Interface providing different elements depending of the generated type
 */
interface GenericTableHelper {

    /**
     * Provides the list of table names
     */
    fun tableNames(): List<String>

    /**
     * Provides the original table name. May contain spaces for example
     */
    fun originalTableName(tableName: String): String

    /**
     * Provides the appropriate Entity
     */
    fun parseEntityFromTable(
        tableName: String,
        jsonString: String,
        fetchedFromRelation: Boolean
    ): EntityModel?

    /**
     * Provides the appropriate EntityListViewModel
     */
    fun entityListViewModelFromTable(
        tableName: String,
        apiService: ApiService
    ): EntityListViewModel<*>

    /**
     * Provides the appropriate EntityViewModel
     */
    fun entityViewModelFromTable(
        tableName: String,
        id: String,
        apiService: ApiService
    ): EntityViewModel<*>

    /**
     * Provides the appropriate EntityListViewModel KClass
     */
    fun entityListViewModelClassFromTable(tableName: String): Class<EntityListViewModel<EntityModel>>

    /**
     * Provides the appropriate EntityViewModel KClass
     */
    fun entityViewModelClassFromTable(tableName: String): Class<EntityViewModel<EntityModel>>

    /**
     * Uses Kotlin reflection to retrieve type properties
     */
    fun <T : EntityModel> getReflectedProperties(
        tableName: String
    ): Pair<Collection<KProperty1<T, *>>, List<KParameter>?>

    /**
     * Retrieves the table name of a related field
     */
    fun getRelatedTableName(sourceTableName: String, relationName: String): String

    /**
     * Provides the relation map extracted from an entity
     */
    fun getManyToOneRelationsInfo(
        tableName: String,
        entity: EntityModel
    ): Map<String, LiveData<RoomRelation>>

    fun getOneToManyRelationsInfo(
        tableName: String,
        entity: EntityModel
    ): Map<String, LiveData<RoomRelation>>

    /**
     * Returns list of table properties as a String, separated by commas, without EntityModel
     * inherited properties
     */
    fun getPropertyListFromTable(tableName: String, application: Application): String

    /**
     * Provides the list of One to Many relations for given tableName
     */
    fun getOneToManyRelationNames(tableName: String): List<String>

    /**
     * Provides the list of Many to One relations for given tableName
     */
    fun getManyToOneRelationNames(tableName: String): List<String>
}
