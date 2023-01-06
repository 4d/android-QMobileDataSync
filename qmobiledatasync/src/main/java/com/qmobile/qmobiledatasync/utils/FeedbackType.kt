/*
 * Created by qmarciset on 16/12/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.utils

enum class FeedbackType(val key: String) {
    TALK_TO_US("question"),
    SUGGEST_IMPROVEMENT("improvement"),
    SHOW_CURRENT_LOG("log"),
    REPORT_A_PROBLEM("bug"),
    REPORT_PREVIOUS_CRASH("crash")
}
