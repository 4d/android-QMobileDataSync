/*
 * Created by Quentin Marciset on 27/5/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmarciset.androidmobiledatasync.relation

import com.qmarciset.androidmobileapi.model.entity.Entities
import com.qmarciset.androidmobileapi.model.entity.EntityModel

data class OneToManyRelation(val entities: Entities<EntityModel>, val className: String)
