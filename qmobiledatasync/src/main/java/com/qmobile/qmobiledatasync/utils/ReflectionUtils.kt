/*
 * Created by qmarciset on 11/2/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.utils

import java.util.Locale
import kotlin.reflect.KProperty1

object ReflectionUtils {

    @Suppress("UNCHECKED_CAST")
    fun readInstanceProperty(instance: Any, propertyName: String): Any? {
        val property = instance::class.members
            .first {
                it.name.lowercase(Locale.getDefault()) == propertyName.lowercase(Locale.getDefault())
            } as KProperty1<Any, *>
        return property.get(instance)
    }
}
