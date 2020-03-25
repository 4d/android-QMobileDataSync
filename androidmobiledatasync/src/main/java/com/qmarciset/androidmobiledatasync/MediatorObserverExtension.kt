/*
 * Created by Quentin Marciset on 24/3/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmarciset.androidmobiledatasync

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer
import com.qmarciset.androidmobiledatasync.model.GlobalStampWithTable

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
