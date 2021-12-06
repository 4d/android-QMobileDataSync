/*
 * Created by Quentin Marciset on 31/3/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.sync

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.qmobile.qmobiledatasync.utils.collectWhenStarted
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

// For now, the tableName field is only useful for debugging, printing tableName
data class GlobalStamp(val tableName: String, val stampValue: Int, val dataSyncProcess: Boolean)

fun MutableList<MediatorLiveData<GlobalStamp>>.removeObservers(activity: AppCompatActivity) {
    this.forEach { mediatorLiveData ->
        mediatorLiveData.removeObservers(activity)
    }
}

fun MutableList<MediatorLiveData<GlobalStamp>>.setObservers(
    activity: AppCompatActivity,
    globalStampObserver: Observer<GlobalStamp>
) {
    this.forEach { mediatorLiveData ->
        mediatorLiveData.observe(activity, globalStampObserver)
    }
}

fun List<StateFlow<GlobalStamp>>.setObserverss(
    lifecycleOwner: LifecycleOwner,
    globalStampObserver: suspend (value: GlobalStamp) -> Unit
) {
    this.forEach { stateFlow ->
//        activity.lifecycleScope.launch {
//            activity.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
//                stateFlow.collect(globalStampObserver)
//            }
//        }
        lifecycleOwner.collectWhenStarted(flow = stateFlow, action = globalStampObserver)
    }
}