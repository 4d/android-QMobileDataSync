/*
 * Created by qmarciset on 6/9/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.utils

interface BaseInputControl {
    fun onClick(outputCallback: (outputText: String) -> Unit)
}

annotation class InputControl
