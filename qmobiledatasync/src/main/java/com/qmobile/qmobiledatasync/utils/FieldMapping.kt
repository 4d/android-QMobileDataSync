/*
 * Created by qmarciset on 13/7/2021.
 * 4D SAS
 * Copyright (c) 2021 qmarciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.utils

import com.qmobile.qmobileapi.utils.getSafeArray
import com.qmobile.qmobileapi.utils.getSafeBoolean
import com.qmobile.qmobileapi.utils.getSafeInt
import com.qmobile.qmobileapi.utils.getSafeObject
import com.qmobile.qmobileapi.utils.getSafeString
import com.qmobile.qmobileapi.utils.getStringList
import com.qmobile.qmobileapi.utils.toStringMap
import org.json.JSONObject

data class FieldMapping(
    val binding: String?,
    val choiceList: Any?, // choiceList can be a JSONObject or a JSONArray
    val name: String?,
    val imageWidth: Int?, // currently not used, reading the one from layout
    val imageHeight: Int?, // currently not used, reading the one from layout
    val tintable: Boolean?
) {

    companion object {

        fun buildCustomFormatterBinding(customFormatters: JSONObject?): Map<String, Map<String, FieldMapping>> {
            val tableMap: MutableMap<String, Map<String, FieldMapping>> = mutableMapOf()

            customFormatters?.keys()?.forEach { tableKey ->
                customFormatters.getSafeObject(tableKey)?.let { fieldsJsonObject ->
                    tableMap[tableKey] = buildFieldMap(fieldsJsonObject)
                }
            }
            return tableMap
        }

        private fun buildFieldMap(fieldsJsonObject: JSONObject): Map<String, FieldMapping> {
            val fieldMap: MutableMap<String, FieldMapping> = mutableMapOf()

            fieldsJsonObject.keys().forEach { fieldKey ->
                fieldsJsonObject.getSafeObject(fieldKey)?.let { fieldMappingJsonObject ->
                    fieldMap[fieldKey] = getFieldMapping(fieldMappingJsonObject)
                }
            }
            return fieldMap
        }

        private fun getFieldMapping(fieldMappingJsonObject: JSONObject): FieldMapping =
            FieldMapping(
                binding = fieldMappingJsonObject.getSafeString("binding"),
                choiceList = fieldMappingJsonObject.getSafeObject("choiceList")
                    ?.toStringMap()
                    ?: fieldMappingJsonObject.getSafeArray("choiceList")
                        .getStringList(), // choiceList can be a JSONObject or a JSONArray
                name = fieldMappingJsonObject.getSafeString("name"),
                // currently not used, reading the one from layout
                imageWidth = fieldMappingJsonObject.getSafeInt("imageWidth"),
                // currently not used, reading the one from layout
                imageHeight = fieldMappingJsonObject.getSafeInt("imageHeight"),
                tintable = fieldMappingJsonObject.getSafeBoolean("tintable")
            )
    }

    fun getChoiceListString(text: String): String? {
        return when (choiceList) {
            is Map<*, *> -> {
                choiceList[text] as? String?
            }
            is List<*> -> {
                text.toIntOrNull()?.let { index ->
                    if (index < choiceList.size)
                        choiceList[index] as? String?
                    else
                        null
                }
            }
            else -> null
        }
    }
}
