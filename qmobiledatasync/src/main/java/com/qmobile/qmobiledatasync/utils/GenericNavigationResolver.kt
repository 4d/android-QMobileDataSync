/*
 * Created by qmarciset on 13/10/2021.
 * 4D SAS
 * Copyright (c) 2021 qmarciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.utils

import androidx.databinding.ViewDataBinding
import androidx.fragment.app.FragmentActivity
import com.qmobile.qmobiledatastore.data.RoomEntity

/**
 * Interface providing different elements depending of the generated type
 */
interface GenericNavigationResolver {

    /**
     * Navigates from list form to ViewPager (which displays one detail form)
     */
    fun navigateFromListToViewPager(
        viewDataBinding: ViewDataBinding,
        sourceTable: String,
        position: Int,
        query: String,
        relationName: String,
        parentItemId: String,
        path: String,
        navbarTitle: String
    )

    /**
     * Navigates from list or detail form to a relation list form (One to Many relation)
     */
    fun setupOneToManyRelationButtonOnClickAction(
        viewDataBinding: ViewDataBinding,
        relationName: String,
        roomEntity: RoomEntity
    )

    /**
     * Navigates from list or detail form to a relation detail form (Many to One relation)
     */
    fun setupManyToOneRelationButtonOnClickAction(
        viewDataBinding: ViewDataBinding,
        relationName: String,
        roomEntity: RoomEntity
    )

    /**
     * Navigates from list or detail form to action form
     */
    fun navigateToActionForm(
        viewDataBinding: ViewDataBinding,
        tableName: String,
        itemId: String,
        relationName: String,
        parentItemId: String,
        pendingTaskId: String,
        actionUUID: String,
        navbarTitle: String
    )

    /**
     * Navigates from action form to barcode scanner fragment
     */
    fun navigateToActionScanner(viewDataBinding: ViewDataBinding)

    /**
     * Navigates to TasksFragment
     */
    fun navigateToPendingTasks(fragmentActivity: FragmentActivity, tableName: String, currentItemId: String)

    /**
     * Navigates to ActionWebViewFragment
     */
    fun navigateToActionWebView(
        fragmentActivity: FragmentActivity,
        path: String,
        actionName: String,
        actionLabel: String?,
        actionShortLabel: String?,
        base64EncodedContext: String
    )

    /**
     * Navigates to PushInputControlFragment
     */
    fun navigateToPushInputControl(viewDataBinding: ViewDataBinding, inputControlName: String, mandatory: Boolean)

    /**
     * Navigates to SettingsFragment
     */
    fun navigateToSettings(fragmentActivity: FragmentActivity)
}
