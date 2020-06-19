/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmarciset.androidmobiledatasync.utils

import android.app.Application
import com.qmarciset.androidmobileapi.model.entity.EntityModel
import com.qmarciset.androidmobileapi.network.ApiService
import com.qmarciset.androidmobiledatasync.relation.Relation
import com.qmarciset.androidmobiledatasync.viewmodel.EntityListViewModel
import com.qmarciset.androidmobiledatasync.viewmodel.EntityViewModel

/**
 * Interface implemented by MainActivity to provide different elements depending of the generated type
 */
interface FromTableForViewModel {

    /**
     * Provides the list of table names
     */
    val tableNames: List<String>

    /**
     * Provides the appropriate Entity
     */
    fun parseEntityFromTable(tableName: String, jsonString: String): EntityModel

    /**
     * Returns list of table properties as a String, separated by commas, without EntityModel
     * inherited properties
     */
    fun <T> getPropertyListFromTable(tableName: String, application: Application): String

    /**
     * Returns the list of relations of the given table
     */
    fun <T> getRelations(tableName: String, application: Application): MutableList<Relation>

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

    fun entityListViewModelClassFromTable(tableName: String): Class<EntityListViewModel<EntityModel>>
}
