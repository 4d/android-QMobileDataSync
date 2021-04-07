/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.utils

import android.app.Application
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobileapi.network.ApiService
import com.qmobile.qmobiledatasync.relation.Relation
import com.qmobile.qmobiledatasync.viewmodel.EntityListViewModel
import com.qmobile.qmobiledatasync.viewmodel.EntityViewModel

/**
 * Interface implemented by MainActivity to provide different elements depending of the generated type
 */
interface FromTableForViewModel {

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
    fun parseEntityFromTable(tableName: String, jsonString: String, fetchedFromRelation: Boolean): EntityModel?

    /**
     * Returns the list of relations of the given table
     */
    fun <T : EntityModel> getRelations(
        tableName: String,
        application: Application
    ): MutableList<Relation>

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
}
