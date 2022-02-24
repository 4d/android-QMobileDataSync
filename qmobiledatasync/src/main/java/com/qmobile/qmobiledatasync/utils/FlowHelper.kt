/*
 * Created by qmarciset on 2/12/2021.
 * 4D SAS
 * Copyright (c) 2021 qmarciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.utils

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

fun <T> LifecycleOwner.collectWhenStarted(
    flow: Flow<T>,
    firstTimeDelay: Long = 0L,
    action: suspend (value: T) -> Unit
) {
//    lifecycleScope.launch {
//        delay(firstTimeDelay)
//        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
//            flow.collect(action)
//        }
//    }
}

inline fun <T> Flow<T>.launchAndCollectIn(
    owner: LifecycleOwner,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    crossinline action: suspend CoroutineScope.(T) -> Unit
) = owner.lifecycleScope.launch {
    owner.repeatOnLifecycle(minActiveState) {
        collect {
            action(it)
        }
    }
}