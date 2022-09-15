/*
 * Created by qmarciset on 6/9/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.utils

interface BaseInputControl {

    val autocomplete: Boolean

    fun getIconName() = ""

    fun process(inputValue: Any? = null, outputCallback: (output: Any) -> Unit)
}

annotation class InputControl
