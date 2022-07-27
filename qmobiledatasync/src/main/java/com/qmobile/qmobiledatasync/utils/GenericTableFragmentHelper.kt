/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.utils

import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment

/**
 * Interface providing different elements depending of the generated type
 */
interface GenericTableFragmentHelper {

    /**
     * Gets the appropriate detail fragment
     */
    fun getDetailFragment(tableName: String): Fragment

    /**
     * Provides the list form type
     */
    fun layoutType(tableName: String): LayoutType

    /**
     * Provides if horizontal swipe on items is allowed
     */
    fun isSwipeAllowed(tableName: String): Boolean

    /**
     * Provides drawable resources for custom formatters
     */
    fun getDrawableForFormatter(formatName: String, imageName: String): Pair<Int, Int>?

    /**
     * Provides the custom list fragment as list forms are given as a base fragment_list
     */
    fun getCustomEntityListFragment(
        tableName: String,
        binding: ViewDataBinding
    ): CustomEntityListFragment?
}
