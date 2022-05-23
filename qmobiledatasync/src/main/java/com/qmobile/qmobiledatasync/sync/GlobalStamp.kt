/*
 * Created by Quentin Marciset on 31/3/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.sync

import kotlin.random.Random

data class GlobalStamp(
    val tableName: String,
    val stampValue: Int,
    val dataSyncProcess: Boolean
) {
    @Suppress("EqualsAlwaysReturnsTrueOrFalse")
    override fun equals(other: Any?): Boolean {
        return false
    }

    override fun hashCode(): Int {
        return Random.nextInt()
    }
}
