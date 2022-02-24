/*
 * Created by qmarciset on 29/10/2021.
 * 4D SAS
 * Copyright (c) 2021 qmarciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.utils

import androidx.lifecycle.LiveData
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobiledatasync.relation.Relation

/**
 * Interface providing different elements depending of the generated type
 */
interface GenericRelationHelper {

    /**
     * Returns the list of relations
     */
    fun getRelations(): List<Relation>

    /**
     * Get relation Id for a given entity
     */
    fun getRelationId(tableName: String, relationName: String, entity: EntityModel): String?
}
