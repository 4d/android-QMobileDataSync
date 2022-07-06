/*
 * Created by Quentin Marciset on 24/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync

import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobileapi.network.ApiService
import com.qmobile.qmobiledatasync.viewmodel.EntityListViewModel
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.asResponseBody
import okio.Buffer
import org.json.JSONObject
import retrofit2.Response
import java.nio.charset.Charset

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

const val employeeEntitiesString =
    "{\"__KEY\":\"1\",\"__TIMESTAMP\":\"2020-05-05T06:41:31.800Z\",\"__STAMP\":19,\"service\":{\"__KEY\":\"16\",\"__TIMESTAMP\":\"2020-05-05T06:42:19.053Z\",\"__STAMP\":7,\"ID\":16,\"name\":\"Sponsorship\",\"comment\":\"sponsors service\",\"__GlobalStamp\":278,\"employeeNumber\":7,\"budget\":\"$14.4k\",\"managerID\":2,\"employees\":{\"__deferred\":{\"uri\":\"/mobileapp/Service(16)/employees?\$expand=employees\"}},\"manager\":{\"__deferred\":{\"uri\":\"/mobileapp/Employee(2)\",\"__KEY\":\"2\"}}},\"serviceManaged\":{\"__ENTITYSET\":\"/mobileapp/Employee(1)/serviceManaged?\$expand=serviceManaged\",\"__DATACLASS\":\"Service\",\"__GlobalStamp\":279,\"__COUNT\":0,\"__FIRST\":0,\"__ENTITIES\":[],\"__SENT\":0},\"FirstName\":\"Lisa\",\"LastName\":\"Hart\",\"Job\":\"Musician\",\"Company\":\"Alpha City\",\"Email\":\"lisa.hart@fakemail.com\",\"Notes\":\"Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor i\",\"Phone\":\"333-444-4444\",\"Address\":\"1425 High Camp Avenue\",\"Location\":\"Boston\",\"Photo\":{\"__deferred\":{\"uri\":\"/mobileapp/Employee(1)/Photo?\$imageformat=best&\$version=19&\$expand=Photo\",\"image\":true}},\"ID\":1},{\"__KEY\":\"2\",\"__TIMESTAMP\":\"2020-04-27T10:27:26.062Z\",\"__STAMP\":16,\"service\":{\"__KEY\":\"16\",\"__TIMESTAMP\":\"2020-05-05T06:42:19.053Z\",\"__STAMP\":7,\"ID\":16,\"name\":\"Sponsorship\",\"comment\":\"sponsors service\",\"__GlobalStamp\":278,\"employeeNumber\":7,\"budget\":\"$14.4k\",\"managerID\":2,\"employees\":{\"__deferred\":{\"uri\":\"/mobileapp/Service(16)/employees?\$expand=employees\"}},\"manager\":{\"__deferred\":{\"uri\":\"/mobileapp/Employee(2)\",\"__KEY\":\"2\"}}},\"serviceManaged\":{\"__ENTITYSET\":\"/mobileapp/Employee(2)/serviceManaged?\$expand=serviceManaged\",\"__DATACLASS\":\"Service\",\"__GlobalStamp\":279,\"__COUNT\":1,\"__FIRST\":0,\"__ENTITIES\":[{\"__KEY\":\"16\",\"__TIMESTAMP\":\"2020-05-05T06:42:19.053Z\",\"__STAMP\":7,\"ID\":16,\"name\":\"Sponsorship\",\"comment\":\"sponsors service\",\"__GlobalStamp\":278,\"employeeNumber\":7,\"budget\":\"$14.4k\",\"managerID\":2,\"employees\":{\"__deferred\":{\"uri\":\"/mobileapp/Service(16)/employees?\$expand=employees\"}},\"manager\":{\"__deferred\":{\"uri\":\"/mobileapp/Employee(2)\",\"__KEY\":\"2\"}}}],\"__SENT\":1},\"FirstName\":\"Joel\",\"LastName\":\"Price\",\"Job\":\"Artist\",\"Company\":\"Klean Corp\",\"Email\":\"joel.price@test.com\",\"Notes\":\"Egestas diam in arcu cursus. Nunc scelerisque viverra mauris in aliquam sem. Gravida rutrum quisque non tellus orci ac auctor augue. Nibh sit amet commodo nulla facilisi. Est placerat in egestas erat imperdiet sed euismod nisi. Mattis nunc sed blandit lib\",\"Phone\":\"555-666-3333\",\"Address\":\"4583 Summer Way\",\"Location\":\"Oakland\",\"Photo\":{\"__deferred\":{\"uri\":\"/mobileapp/Employee(2)/Photo?\$imageformat=best&\$version=16&\$expand=Photo\",\"image\":true}},\"ID\":2},{\"__KEY\":\"3\",\"__TIMESTAMP\":\"2020-03-02T09:00:15.086Z\",\"__STAMP\":16,\"service\":null,\"serviceManaged\":{\"__ENTITYSET\":\"/mobileapp/Employee(3)/serviceManaged?\$expand=serviceManaged\",\"__DATACLASS\":\"Service\",\"__GlobalStamp\":279,\"__COUNT\":0,\"__FIRST\":0,\"__ENTITIES\":[],\"__SENT\":0},\"FirstName\":\"Monica\",\"LastName\":\"Hunt\",\"Job\":\"Actor\",\"Company\":\"Foograf\",\"Email\":\"monica.hunt@fakemail.com\",\"Notes\":\"Sem viverra aliquet eget sit amet tellus cras adipiscing enim. Enim tortor at auctor urna nunc id cursus. Leo vel fringilla est ullamcorper eget. Non arcu risus quis varius quam quisque id diam vel. Cras semper auctor neque vitae tempus. Sagittis purus si\",\"Phone\":\"888-999-3333\",\"Address\":\"4441 Lincoln Park Avenue\",\"Location\":\"Sacramento\",\"Photo\":{\"__deferred\":{\"uri\":\"/mobileapp/Employee(3)/Photo?\$imageformat=best&\$version=16&\$expand=Photo\",\"image\":true}},\"ID\":3}],\"__SENT\":23}"

fun buildSampleResponseFromJsonString(
    responseJsonString: String,
    isSuccess: Boolean
): Response<ResponseBody> {
    val charset: Charset = Charsets.UTF_8
    val buffer = Buffer().writeString(responseJsonString, charset)
    val responseBody = buffer.asResponseBody(null, buffer.size)
    return if (isSuccess) {
        Response.success(responseBody)
    } else {
        Response.error(401, responseBody)
    }
}

open class EntityListViewModelEmployee(tableName: String, apiService: ApiService) :
    EntityListViewModel<Employee>(tableName, apiService)

open class EntityListViewModelService(tableName: String, apiService: ApiService) :
    EntityListViewModel<Service>(tableName, apiService)

open class EntityListViewModelOffice(tableName: String, apiService: ApiService) :
    EntityListViewModel<Office>(tableName, apiService)
