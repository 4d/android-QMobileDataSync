/*
 * Created by Quentin Marciset on 27/5/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmarciset.androidmobiledatasync.relation

data class Relation(val relationName: String, val className: String, val relationType: RelationType)
// Example : Relation(organizer, Employee, RelationType.MANY_TO_ONE)
// Example : Relation(employees, Entities, RelationType.ONE_TO_MANY)
