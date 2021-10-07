/*
 * Created by qmarciset on 16/7/2021.
 * 4D SAS
 * Copyright (c) 2021 qmarciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.utils

import timber.log.Timber

object LogLevelController {
    /**
     *  val VERBOSE = 2
     *  val DEBUG = 3
     *  val INFO = 4
     *  val WARN = 5
     *  val ERROR = 6
     *  val ASSERT = 7
     */

    fun initialize(level: Int) {
        Timber.plant(
            LogLevelControllerTree(level)
        )
    }

    class LogLevelControllerTree(private val level: Int) : Timber.DebugTree() {

        override fun createStackElementTag(element: StackTraceElement): String {
            return String.format(
                "Class:%s: Line: %s, Method: %s",
                super.createStackElementTag(element),
                element.lineNumber,
                element.methodName
            )
        }

        override fun isLoggable(tag: String?, priority: Int): Boolean {
            return level <= priority
        }
    }
}
