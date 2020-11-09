/*
 * Created by Quentin Marciset on 27/5/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.relation

data class Relation(
    val relationName: String,
    val className: String,
    val relationType: RelationTypeEnum
) {

    companion object {
        const val SUFFIX = ".*"
    }
}
