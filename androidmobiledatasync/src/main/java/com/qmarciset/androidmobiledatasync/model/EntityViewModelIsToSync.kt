/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmarciset.androidmobiledatasync.model

import com.qmarciset.androidmobileapi.model.entity.EntityModel
import com.qmarciset.androidmobiledatasync.viewmodel.EntityListViewModel

data class EntityViewModelIsToSync(val vm: EntityListViewModel<EntityModel>, var isToSync: Boolean)
