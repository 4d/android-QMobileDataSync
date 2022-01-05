/*
 * Created by Quentin Marciset on 31/3/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.sync

import java.util.UUID

data class GlobalStamp(
    val tableName: String,
    val stampValue: Int,
    val dataSyncProcess: Boolean,
    val uuid: UUID?
)
