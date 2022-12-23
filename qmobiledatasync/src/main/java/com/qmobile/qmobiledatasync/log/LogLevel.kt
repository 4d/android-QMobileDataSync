/*
 * Created by qmarciset on 20/12/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.log

@Suppress("MagicNumber")
enum class LogLevel(val level: Int) {
    VERBOSE(2),
    DEBUG(3),
    INFO(4),
    WARN(5),
    ERROR(6),
    ASSERT(7),
    NONE(8);

    companion object {
        private val map = values().associateBy(LogLevel::level)
        fun fromLevel(type: Int) = map[type]
    }
}
