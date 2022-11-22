/*
 * Created by qmarciset on 13/7/2021.
 * 4D SAS
 * Copyright (c) 2021 qmarciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.utils

import android.app.Application
import com.qmobile.qmobileapi.model.entity.TableInfo
import com.qmobile.qmobileapi.utils.FileHelper.listAssetFiles
import com.qmobile.qmobileapi.utils.FileHelper.readContentFromFile
import com.qmobile.qmobileapi.utils.getSafeArray
import com.qmobile.qmobileapi.utils.getSafeBoolean
import com.qmobile.qmobileapi.utils.getSafeInt
import com.qmobile.qmobileapi.utils.getSafeObject
import com.qmobile.qmobileapi.utils.getSafeString
import com.qmobile.qmobileapi.utils.getStringList
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.relation.Relation
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber

open class RuntimeDataHolder(
    var initialGlobalStamp: Int,
    var guestLogin: Boolean,
    var remoteUrl: String,
    var sdkVersion: String,
    var logLevel: Int,
    var dumpedTables: List<String>,
    var relationAvailable: Boolean = true,
    var relations: List<Relation>,
    var tableInfo: Map<String, TableInfo>,
    var customFormatters: Map<String, Map<String, FieldMapping>>, // Map<TableName, Map<FieldName, FieldMapping>>
    var embeddedFiles: List<String>,
    var tableActions: JSONObject,
    var currentRecordActions: JSONObject,
    var inputControls: List<FieldMapping>
) {

    companion object {

        private const val DEFAULT_LOG_LEVEL = 4
        private const val EMBEDDED_PICTURES = "Pictures"
        private const val JSON_EXT = ".json"

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
            val appInfoJsonObj =
                JSONObject(readContentFromFile(application.baseContext, "appInfo.json"))
            val customFormattersJsonObj =
                JSONObject(readContentFromFile(application.baseContext, "formatters.json"))
            val actionsJsonObj =
                JSONObject(readContentFromFile(application.baseContext, "actions.json"))
            val tableInfoJsonObj =
                JSONObject(readContentFromFile(application.baseContext, "tableInfo.json"))
            val inputControlsJsonArray =
                JSONArray(readContentFromFile(application.baseContext, "inputControls.json"))

            val sdkVersion = readContentFromFile(application.baseContext, "sdkVersion")

            return RuntimeDataHolder(
                initialGlobalStamp = appInfoJsonObj.getSafeInt("initialGlobalStamp") ?: 0,
                guestLogin = appInfoJsonObj.getSafeBoolean("guestLogin") ?: true,
                remoteUrl = appInfoJsonObj.getSafeString("remoteUrl") ?: "",
                sdkVersion = sdkVersion,
                logLevel = appInfoJsonObj.getSafeInt("logLevel") ?: DEFAULT_LOG_LEVEL,
                dumpedTables = appInfoJsonObj.getSafeArray("dumpedTables").getStringList(),
                relationAvailable = appInfoJsonObj.getSafeBoolean("relations") ?: true,
                relations = BaseApp.genericRelationHelper.getRelations(),
                tableInfo = buildTableInfo(tableInfoJsonObj),
                customFormatters = FieldMapping.buildCustomFormatterBinding(customFormattersJsonObj),
                embeddedFiles = listAssetFiles(
                    application.baseContext,
                    EMBEDDED_PICTURES
                ).filter { !it.endsWith(JSON_EXT) },
                tableActions = actionsJsonObj.getSafeObject("table")?.addActionId() ?: JSONObject(),
                currentRecordActions = actionsJsonObj.getSafeObject("currentRecord")?.addActionId() ?: JSONObject(),
                inputControls = FieldMapping.buildInputControlsBinding(inputControlsJsonArray)
            )
        }

        private fun buildTableInfo(tableInfoJsonObj: JSONObject): Map<String, TableInfo> {
            val map = mutableMapOf<String, TableInfo>()
            tableInfoJsonObj.keys().forEach { tableName ->
                tableInfoJsonObj.getSafeObject(tableName)?.let {
                    val originalName = it.getSafeString("originalName") ?: ""
                    val query = it.getSafeString("query") ?: ""
                    val fields = it.getSafeString("fields")?.split(", ") ?: listOf()
                    val searchFieldString = it.getSafeString("searchFields") ?: ""
                    val searchField = if (searchFieldString.isEmpty()) {
                        listOf()
                    } else {
                        searchFieldString.split(", ")
                    }
                    map[tableName] = TableInfo(originalName, query, fields, searchField)
                }
            }
            return map
        }

        private fun JSONObject.addActionId(): JSONObject {
            this.keys().forEach { tableName ->
                this.getSafeArray(tableName.toString())?.let { actionsArray ->
                    for (i in 0 until actionsArray.length()) {
                        // pattern: $actionName$tableName
                        val id = actionsArray.getSafeObject(i)?.getSafeString("name") + tableName
                        actionsArray.getSafeObject(i)?.put("uuid", id)
                    }
                }
            }
            return this
        }
    }
}
