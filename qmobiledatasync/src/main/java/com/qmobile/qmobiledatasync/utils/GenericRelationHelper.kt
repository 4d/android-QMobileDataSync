/*
 * Created by qmarciset on 29/10/2021.
 * 4D SAS
 * Copyright (c) 2021 qmarciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.utils

import android.app.Application
import androidx.lifecycle.LiveData
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobiledatastore.data.RoomRelation

/**
 * Interface providing different elements depending of the generated type
 */
interface GenericRelationHelper {

    /**
     * Retrieves the table name of a related field
     */
    fun getRelatedTableName(sourceTableName: String, relationName: String): String

    /**
     * Provides the many to one relation map extracted from an entity
     */
    fun getManyToOneRelationsInfo(
        tableName: String,
        entity: EntityModel
    ): Map<String, LiveData<RoomRelation>>

    /**
     * Provides the one to many relation map extracted from an entity
     */
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
