/*
 * Created by qmarciset on 5/9/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.utils

import android.view.View

/**
 * Interface providing different elements depending of the generated type
 */
interface GenericActionHelper {

    /**
     * Gets the appropriate input control class
     */
    fun getInputControl(itemView: View, format: String?): BaseInputControl?
}
