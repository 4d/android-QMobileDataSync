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
        parentItemId: String,
        inverseName: String
    )

    /**
     * Navigates from list or detail form to a relation list form (One to Many relation)
     */
    fun setupOneToManyRelationButtonOnClickAction(
        viewDataBinding: ViewDataBinding,
        relationName: String,
        itemId: String,
        entity: EntityModel, // if relationName contains ".", parentItemId is inverseName's key
        anyRelatedEntity: RoomData? = null
    )

    /**
     * Navigates from list or detail form to a relation detail form (Many to One relation)
     */
    fun setupManyToOneRelationButtonOnClickAction(
        viewDataBinding: ViewDataBinding,
        relationName: String,
        entity: EntityModel
    )

    /**
     * Navigates from list or detail form to action form
     */
    fun navigateToActionForm(
        viewDataBinding: ViewDataBinding,
        tableName: String,
        itemId: String,
        destinationTable: String,
        parentItemId: String,
        inverseName: String,
        navbarTitle: String
    )

    /**
     * Navigates from action form to barcode scanner fragment
     */
    fun navigateToActionScanner(viewDataBinding: ViewDataBinding, position: Int)
}
