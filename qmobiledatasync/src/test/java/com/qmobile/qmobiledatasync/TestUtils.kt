/*
 * Created by Quentin Marciset on 24/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync

import com.qmobile.qmobileapi.model.entity.EntityModel
import org.json.JSONObject

// Sample tables for synchronization

data class Employee(
    override val __KEY: String,
    override val __STAMP: Int? = null,
    override val __GlobalStamp: Int? = null,
    override val __TIMESTAMP: String? = null
) : EntityModel

data class Service(
    override val __KEY: String,
    override val __STAMP: Int? = null,
    override val __GlobalStamp: Int? = null,
    override val __TIMESTAMP: String? = null
) : EntityModel

data class Office(
    override val __KEY: String,
    override val __STAMP: Int? = null,
    override val __GlobalStamp: Int? = null,
    override val __TIMESTAMP: String? = null
) : EntityModel

// Sample data sets
const val globalStampValue_0 = 0
val globalStampValueSet_1 = listOf(123, 124, 256)
val globalStampValueSet_2 = listOf(256, 260, 256)
val globalStampValueSet_3 = listOf(260, 260, 260)

// Sample deletedRecords

val sampleDeletedRecord =
    JSONObject("{    \"__entityModel\": \"__DeletedRecords\",    \"__GlobalStamp\": 10,    \"__COUNT\": 3,    \"__SENT\": 3,    \"__FIRST\": 0,    \"__ENTITIES\": [    {        \"__KEY\": \"9\",        \"__TIMESTAMP\": \"2017-06-14T07:38:09.130Z\",        \"__STAMP\": 1,        \"__Stamp\": 9,        \"__PrimaryKey\": \"24\",        \"__TableNumber\": 1,        \"__TableName\": \"Employee\"    },{        \"__KEY\": \"10\",        \"__TIMESTAMP\": \"2017-07-14T07:38:09.130Z\",        \"__STAMP\": 1,        \"__Stamp\": 10,        \"__PrimaryKey\": \"25\",        \"__TableNumber\": 1,        \"__TableName\": \"Employee\"    },{        \"__KEY\": \"11\",        \"__TIMESTAMP\": \"2017-08-14T07:38:09.130Z\",        \"__STAMP\": 1,        \"__Stamp\": 11,        \"__PrimaryKey\": \"26\",        \"__TableNumber\": 2,        \"__TableName\": \"Service\"    }    ]}")
