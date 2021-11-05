/*
 * Created by qmarciset on 13/10/2021.
 * 4D SAS
 * Copyright (c) 2021 qmarciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.utils

import androidx.databinding.ViewDataBinding
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobiledatastore.data.RoomData

/**
 * Interface providing different elements depending of the generated type
 */
interface GenericNavigationResolver {

    /**
     * Navigates from list form to ViewPager (which displays one detail form)
     */
    fun navigateFromListToViewPager(
        viewDataBinding: ViewDataBinding,
        key: String,
        query: String,
        destinationTable: String,
        currentItemId: String,
        inverseName: String
    )

    /**
     * Navigates from list form to another list form (One to Many relation)
     */
    fun setupOneToManyRelationButtonOnClickActionForCell(
        viewDataBinding: ViewDataBinding,
        relationName: String,
        parentItemId: String,
        entity: EntityModel, // if relationName contains ".", parentItemId is inverseName's key
        anyRelatedEntity: RoomData? = null
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
        parentItemId: String,
        entity: EntityModel, // if relationName contains ".", parentItemId is inverseName's key
        anyRelatedEntity: RoomData? = null
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
