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
import org.json.JSONArray
import org.json.JSONObject
import java.util.LinkedList

data class FieldMapping(
    val binding: String?,
    val choiceList: Any?, // choiceList can be a JSONObject or a JSONArray
    var choiceListComputed: Map<String, Any>?,
    val type: Any?, // type can be a String or a JSONArray
    val name: String?,
    val imageWidth: Int?, // currently not used, reading the one from layout
    val imageHeight: Int?, // currently not used, reading the one from layout
    val tintable: Boolean?,
    val format: String?
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

        fun buildInputControlsBinding(inputControls: JSONArray?): List<FieldMapping> {
            val inputControlList: MutableList<FieldMapping> = mutableListOf()

            if (inputControls != null) {
                for (i in 0 until inputControls.length()) {
                    inputControls.getSafeObject(i)?.let { fieldMappingJsonObject ->
                        inputControlList.add(getFieldMapping(fieldMappingJsonObject))
                    }
                }
            }
            return inputControlList
        }

        private fun getFieldMapping(fieldMappingJsonObject: JSONObject): FieldMapping =
            FieldMapping(
                binding = fieldMappingJsonObject.getSafeString("binding"),
                choiceList = getChoiceList(fieldMappingJsonObject),
                choiceListComputed = null,
                type = fieldMappingJsonObject.getSafeArray("type")
                    ?.getStringList() // type can be a JSONArray or a String
                    ?: fieldMappingJsonObject.getSafeString("type"),
                name = fieldMappingJsonObject.getSafeString("name"),
                // currently not used, reading the one from layout
                imageWidth = fieldMappingJsonObject.getSafeInt("imageWidth"),
                // currently not used, reading the one from layout
                imageHeight = fieldMappingJsonObject.getSafeInt("imageHeight"),
                tintable = fieldMappingJsonObject.getSafeBoolean("tintable"),
                format = fieldMappingJsonObject.getSafeString("format")
            )

        private fun getChoiceList(fieldMappingJsonObject: JSONObject): Any {
            return if (fieldMappingJsonObject.getSafeObject("choiceList")?.getSafeObject("dataSource") != null) {
                mapOf(
                    "dataSource" to fieldMappingJsonObject.getSafeObject("choiceList")?.getSafeObject("dataSource")
                        ?.toStringMap()
                )
            } else {
                fieldMappingJsonObject.getSafeObject("choiceList")?.toStringMap()
                    ?: fieldMappingJsonObject.getSafeArray("choiceList")
                        .getStringList() // choiceList can be a JSONObject or a JSONArray
            }
        }
    }

    fun getStringInChoiceList(text: String): String? {
        return when (val choiceListComputed = this.choiceListComputed) {
            is Map<*, *> -> getStringInMap(choiceListComputed, text)
            else -> null
        }?: when (choiceList) {
            is Map<*, *> -> getStringInMap(choiceList, text)
            is List<*> -> getStringInList(choiceList, text)
            else -> null
        }
    }

    private fun hasBooleanType(): Boolean = when (type) {
        is List<*> -> type.contains("boolean")
        is String -> type == "boolean"
        else -> false
    }

    fun hasTextType(): Boolean = when (type) {
        is List<*> -> type.contains("text")
        is String -> type == "text"
        else -> false
    }

    private fun booleanStringToBooleanInt(text: String): String? = when (text.lowercase()) {
        "false" -> "0"
        "true" -> "1"
        else -> null
    }

    private fun getStringInMap(choiceList: Map<*, *>, text: String): String? {
        var value: String? = choiceList[text] as? String?
        if (value == null && hasBooleanType()) {
            value = choiceList[booleanStringToBooleanInt(text)] as? String?
        }
        return value
    }

    private fun getStringInList(choiceList: List<*>, text: String): String? {
        var value: String? = null
        text.toIntOrNull()?.let { index ->
            if (index < choiceList.size) {
                value = choiceList[index] as? String?
            }
        }
        if (value == null && hasBooleanType()) {
            booleanStringToBooleanInt(text)?.toIntOrNull()?.let { index ->
                if (index < choiceList.size) {
                    value = choiceList[index] as? String?
                }
            }
        }
        return value
    }

    fun getChoiceList(): LinkedList<Any> {
        return getChoiceList (choiceList)
    }

    fun getChoiceList(choiceList: Any?): LinkedList<Any> {
        return when (choiceList) {
            is Map<*, *> -> LinkedList(choiceList.map { Pair(it.key, it.value) })
            is List<*> -> LinkedList(choiceList)
            else -> LinkedList()
        }
    }
}
