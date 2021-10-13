/*
 * Created by qmarciset on 13/10/2021.
 * 4D SAS
 * Copyright (c) 2021 qmarciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.utils

import androidx.databinding.ViewDataBinding
import com.qmobile.qmobileapi.model.entity.EntityModel

interface GenericNavigationResolver {

    /**
     * Navigates from list form to ViewPager (which displays one detail form)
     */
    fun navigateFromListToViewPager(viewDataBinding: ViewDataBinding, position: Int)

    /**
     * Navigates from list form to another list form (One to Many relation)
     */
    fun setupOneToManyRelationButtonOnClickActionForCell(
        viewDataBinding: ViewDataBinding,
        relationName: String,
        parentItemId: String
    )

    /**
     * Navigates from list form to a detail form (Many to One relation)
     */
    fun setupManyToOneRelationButtonOnClickActionForCell(
        viewDataBinding: ViewDataBinding,
        relationName: String,
        entity: EntityModel
    )

    /**
     * Navigates from detail form to a list form (One to Many relation)
     */
    fun setupOneToManyRelationButtonOnClickActionForDetail(
        viewDataBinding: ViewDataBinding,
        relationName: String,
        parentItemId: String
    )

    /**
     * Navigates from detail form to another detail form (Many to One relation)
     */
    fun setupManyToOneRelationButtonOnClickActionForDetail(
        viewDataBinding: ViewDataBinding,
        relationName: String,
        entity: EntityModel
    )
}
