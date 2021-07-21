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
    val type: Any?, // type can be a String or a JSONArray
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
                type = fieldMappingJsonObject.getSafeArray("type")
                    ?.getStringList() // type can be a JSONArray or a String
                    ?: fieldMappingJsonObject.getSafeString("type"),
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
            is Map<*, *> -> getMapValue(choiceList, text)
            is List<*> -> getListValue(choiceList, text)
            else -> null
        }
    }

    private fun hasBooleanType(): Boolean = when (type) {
        is List<*> -> type.contains("boolean")
        is String -> type == "boolean"
        else -> false
    }

    private fun asBooleanStringToBooleanInt(text: String): String? = when (text.lowercase()) {
        "false" -> "0"
        "true" -> "1"
        else -> null
    }

    private fun getMapValue(choiceList: Map<*, *>, text: String): String? {
        var value: String? = choiceList[text] as? String?
        if (value == null && hasBooleanType()) {
            value = choiceList[asBooleanStringToBooleanInt(text)] as? String?
        }
        return value
    }

    private fun getListValue(choiceList: List<*>, text: String): String? {
        var value: String? = null
        text.toIntOrNull()?.let { index ->
            if (index < choiceList.size)
                value = choiceList[index] as? String?
        }
        if (value == null && hasBooleanType()) {
            asBooleanStringToBooleanInt(text)?.toIntOrNull()?.let { index ->
                if (index < choiceList.size)
                    value = choiceList[index] as? String?
            }
        }
        return value
    }
}
