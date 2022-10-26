/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.utils

import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobileapi.network.ApiService
import com.qmobile.qmobiledatasync.section.SectionField
import com.qmobile.qmobiledatasync.viewmodel.EntityListViewModel
import com.qmobile.qmobiledatasync.viewmodel.EntityViewModel

/**
 * Interface providing different elements depending of the generated type
 */
interface GenericTableHelper {

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
     * Provides the appropriate SectionField
     */
    fun getSectionFieldForTable(tableName: String): SectionField?
}
