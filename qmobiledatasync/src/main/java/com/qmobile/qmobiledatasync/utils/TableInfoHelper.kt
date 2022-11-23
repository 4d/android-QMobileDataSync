
package com.qmobile.qmobiledatasync.utils
import com.qmobile.qmobileapi.model.entity.TableInfo
import com.qmobile.qmobileapi.utils.getSafeObject
import com.qmobile.qmobileapi.utils.getSafeString
import org.json.JSONObject

object TableInfoHelper {

    fun buildTableInfo(tableInfoJsonObj: JSONObject): Map<String, TableInfo> {
        val map = mutableMapOf<String, TableInfo>()
        tableInfoJsonObj.keys().forEach { tableName ->
            tableInfoJsonObj.getSafeObject(tableName)?.let {
                val originalName = it.getSafeString("originalName") ?: ""
                val query = it.getSafeString("query") ?: ""
                val fields = it.getSafeString("fields")?.split(", ") ?: listOf()
                val searchField = it.getSafeString("searchFields")?.split(", ") ?: listOf()

                map[tableName] = TableInfo(
                    originalName,
                    query,
                    fields,
                    searchField)
            }
        }
        return map
    }
}
