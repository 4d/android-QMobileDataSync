/*
 * Created by qmarciset on 5/9/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.utils

import android.view.View
import androidx.fragment.app.FragmentActivity

/**
 * Interface providing different elements depending of the generated type
 */
interface GenericResourceHelper {

    /**
     * Gets the appropriate input control class
     */
    fun getKotlinInputControl(itemView: View, format: String?): BaseKotlinInputControl?

    /**
     * Gets the appropriate login form
     */
    fun getLoginForm(activity: FragmentActivity): LoginHandler
}
