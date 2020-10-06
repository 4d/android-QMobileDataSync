/*
 * Created by Quentin Marciset on 31/3/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.sync

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer

// For now, the tableName field is only useful for debugging, printing tableName
data class GlobalStampWithTable(val tableName: String, val globalStamp: Int)

fun MutableList<MediatorLiveData<GlobalStampWithTable>>.removeObservers(activity: AppCompatActivity) {
    for (mediatorLiveData in this) {
        mediatorLiveData.removeObservers(activity)
    }
}

fun MutableList<MediatorLiveData<GlobalStampWithTable>>.setObservers(
    activity: AppCompatActivity,
    globalStampObserver: Observer<GlobalStampWithTable>
) {
    for (mediatorLiveData in this) {
        mediatorLiveData.observe(activity, globalStampObserver)
    }
}
