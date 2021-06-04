/*
 * Created by Quentin Marciset on 2/3/2021.
 * 4D SAS
 * Copyright (c) 2021 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.utils

import androidx.databinding.ViewDataBinding
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobiledatasync.viewmodel.EntityListViewModel
import com.qmobile.qmobiledatasync.viewmodel.EntityViewModel

/**
 * Interface implemented by MainActivity to provide different elements depending of the generated type
 */
interface FragmentUtil {

    /**
     * Sets the appropriate EntityListViewModel
     */
    fun setEntityListViewModel(
        viewDataBinding: ViewDataBinding,
        entityListViewModel: EntityListViewModel<EntityModel>
    )

    /**
     * Sets the appropriate EntityViewModel
     */
    fun setEntityViewModel(
        viewDataBinding: ViewDataBinding,
        entityViewModel: EntityViewModel<EntityModel>
    )

    /**
     * Provides the list form type
     */
    fun layoutType(tableName: String): String

    /**
     * Sets relations to the appropriate list form
     */
    fun setRelationBinding(
        viewDataBinding: ViewDataBinding,
        relationName: String,
        relatedEntity: Any
    )

    /**
     * Reset relations as PagedListAdapter generates issue with relations
     */
    fun unsetRelationBinding(viewDataBinding: ViewDataBinding)

    /**
     * Provides drawable resources for custom formatters
     */
    fun getDrawableForFormatter(formatName: String, imageName: String): Pair<Int, Int?>?
}
