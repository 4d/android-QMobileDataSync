/*
 * Created by qmarciset on 11/2/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.relation

import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobileapi.utils.getObjectListAsString
import com.qmobile.qmobileapi.utils.getSafeArray
import com.qmobile.qmobileapi.utils.getSafeString
import com.qmobile.qmobiledatasync.app.BaseApp
import org.json.JSONObject

data class JSONRelation(val json: JSONObject, val dest: String, val type: RelationTypeEnum) {

    fun getDestinationTable(): String = if (type == RelationTypeEnum.ONE_TO_MANY)
        json.getSafeString("__DATACLASS")?.filter { !it.isWhitespace() } ?: "" // TODO : WHY !WHITESPACE
    else
        dest

    fun getEntities(): List<EntityModel> {
        val entities = mutableListOf<EntityModel>()
        json.getSafeArray("__ENTITIES")?.getObjectListAsString()?.forEach { entityString ->
            BaseApp.genericTableHelper.parseEntityFromTable(getDestinationTable(), entityString, true)?.let { entity ->
                entities.add(entity)
            }
        }
        return entities
    }

    fun getEntity(): EntityModel? = BaseApp.genericTableHelper.parseEntityFromTable(dest, json.toString(), true)
}
