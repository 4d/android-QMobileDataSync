/*
 * Created by Quentin Marciset on 27/5/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmarciset.androidmobiledatasync.relation

import org.json.JSONObject

data class ManyToOneRelation(val entity: JSONObject, val className: String)
