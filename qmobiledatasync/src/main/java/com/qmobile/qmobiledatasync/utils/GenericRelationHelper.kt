/*
 * Created by qmarciset on 29/10/2021.
 * 4D SAS
 * Copyright (c) 2021 qmarciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.utils

import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobiledatasync.relation.Relation

/**
 * Interface providing different elements depending of the generated type
 */
interface GenericRelationHelper {

    fun getRelations(): List<Relation>

    fun getRelationId(tableName: String, relationName: String, entity: EntityModel): String?
}
