/*
 * Created by qmarciset on 13/7/2021.
 * 4D SAS
 * Copyright (c) 2021 qmarciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.utils

import android.app.Application
import com.qmobile.qmobileapi.model.queries.Query
import com.qmobile.qmobileapi.utils.getSafeArray
import com.qmobile.qmobileapi.utils.getSafeBoolean
import com.qmobile.qmobileapi.utils.getSafeInt
import com.qmobile.qmobileapi.utils.getSafeString
import com.qmobile.qmobileapi.utils.getStringList
import com.qmobile.qmobileapi.utils.listAssetFiles
import com.qmobile.qmobileapi.utils.readContentFromFile
import com.qmobile.qmobiledatasync.app.BaseApp
import org.json.JSONObject
import timber.log.Timber
import java.io.File

data class RuntimeDataHolder(
    var initialGlobalStamp: Int,
    var guestLogin: Boolean,
    var remoteUrl: String,
    var searchField: JSONObject,
    var sdkVersion: String,
    var logLevel: Int,
    var dumpedTables: String,
    var relationAvailable: Boolean = true,
    var queries: Map<String, String>,
    var tableProperties: Map<String, String>,
    var customFormatters: Map<String, Map<String, FieldMapping>>, // Map<TableName, Map<FieldName, FieldMapping>>
    var embeddedFiles: List<String>
) {

    companion object {

        private const val DEFAULT_LOG_LEVEL = 4
        private const val EMBEDDED_PICTURES_PARENT = "Assets.xcassets"
        private const val EMBEDDED_PICTURES = "Pictures"
        private const val JSON_EXT = ".json"
        const val PROPERTIES_PREFIX = "properties"

        fun init(
            application: Application,
            isDebug: Boolean
        ): RuntimeDataHolder {
            val runtimeDataHolder = build(application)
            BaseApp.sharedPreferencesHolder.apply {
                guestLogin = runtimeDataHolder.guestLogin
                if (remoteUrl.isEmpty()) {
                    remoteUrl = runtimeDataHolder.remoteUrl
                }
            }

            if (isDebug) {
//            LogController.initialize(this)
                LogLevelController.initialize(runtimeDataHolder.logLevel)
                Timber.i("[LOG LEVEL] ${runtimeDataHolder.logLevel}")
            }

            Timber.i("[SDK VERSION] ${runtimeDataHolder.sdkVersion}")

            return runtimeDataHolder
        }

        private fun build(application: Application): RuntimeDataHolder {
            val appInfoJsonObj = JSONObject(readContentFromFile(application.baseContext, "app_info.json"))
            val customFormattersJsonObj =
                JSONObject(readContentFromFile(application.baseContext, "custom_formatters.json"))
            val searchableFieldsJsonObj =
                JSONObject(readContentFromFile(application.baseContext, "searchable_fields.json"))
            val queryJsonObj = JSONObject(readContentFromFile(application.baseContext, "queries.json"))

            val sdkVersion = readContentFromFile(application.baseContext, "sdkVersion")

            return RuntimeDataHolder(
                initialGlobalStamp = appInfoJsonObj.getSafeInt("initialGlobalStamp") ?: 0,
                guestLogin = appInfoJsonObj.getSafeBoolean("guestLogin") ?: true,
                remoteUrl = appInfoJsonObj.getSafeString("remoteUrl") ?: "",
                searchField = searchableFieldsJsonObj,
                sdkVersion = sdkVersion,
                logLevel = appInfoJsonObj.getSafeInt("logLevel") ?: DEFAULT_LOG_LEVEL,
                dumpedTables = appInfoJsonObj.getSafeArray("dumpedTables").getStringList()
                    .joinToString(),
                relationAvailable = appInfoJsonObj.getSafeBoolean("relations") ?: true,
                queries = Query.buildQueries(queryJsonObj),
                tableProperties = buildTableProperties(application),
                customFormatters = FieldMapping.buildCustomFormatterBinding(customFormattersJsonObj),
                embeddedFiles = listAssetFiles(
                    application.baseContext,
                    EMBEDDED_PICTURES_PARENT +
                        File.separator + EMBEDDED_PICTURES
                ).filter { !it.endsWith(JSON_EXT) }
            )
        }

        private fun buildTableProperties(application: Application): Map<String, String> {
            val map = mutableMapOf<String, String>()
            BaseApp.genericTableHelper.apply {
                tableNames().forEach { tableName ->
                    val properties: String = this.getPropertyListFromTable(tableName, application)
                    map["${PROPERTIES_PREFIX}_$tableName"] = properties
                }
            }
            return map
        }
    }

    fun getTableProperty(tableName: String): String = tableProperties["${PROPERTIES_PREFIX}_$tableName"] ?: ""

    fun getQuery(tableName: String): String = queries["${Query.QUERY_PREFIX}_$tableName"] ?: ""
}
