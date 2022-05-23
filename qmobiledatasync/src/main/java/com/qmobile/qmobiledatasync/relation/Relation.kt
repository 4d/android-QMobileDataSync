/*
 * Created by Quentin Marciset on 27/5/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.relation

import androidx.lifecycle.LiveData
import com.qmobile.qmobiledatastore.data.RoomEntity

data class Relation(
    val source: String,
    val dest: String,
    val name: String,
    val inverse: String,
    val type: Type,
    val path: String = ""
) {
    companion object {
        const val SUFFIX = ".*"
    }

    enum class Type {
        ONE_TO_MANY, MANY_TO_ONE
    }

    data class QueryResult(
        val query: String,
        val liveData: LiveData<List<RoomEntity>>
    )
}
