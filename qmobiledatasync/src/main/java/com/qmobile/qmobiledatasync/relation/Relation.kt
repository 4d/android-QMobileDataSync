/*
 * Created by Quentin Marciset on 27/5/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.relation

data class Relation(
    val source: String,
    val dest: String,
    val name: String,
    val inverse: String,
    val type: RelationTypeEnum,
    val path: String = ""
) {
    companion object {
        const val SUFFIX = ".*"
    }
}
