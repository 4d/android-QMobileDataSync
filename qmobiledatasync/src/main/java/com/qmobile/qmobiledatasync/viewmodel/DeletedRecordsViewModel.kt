/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.viewmodel

import com.qmobile.qmobileapi.model.entity.DeletedRecord
import com.qmobile.qmobileapi.network.ApiService
import com.qmobile.qmobileapi.repository.RestRepository
import com.qmobile.qmobileapi.utils.DELETED_RECORDS
import com.qmobile.qmobileapi.utils.getObjectListAsString
import com.qmobile.qmobileapi.utils.getSafeArray
import com.qmobile.qmobileapi.utils.retrieveJSONObject
import com.qmobile.qmobiledatasync.app.BaseApp
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber

open class DeletedRecordsViewModel(apiService: ApiService) : BaseViewModel() {

    init {
        Timber.v("DeletedRecordsViewModel initializing...")
    }

    private var restRepository: RestRepository = RestRepository(DELETED_RECORDS, apiService)

    fun getDeletedRecords(
        onResult: (entitiesList: List<String>) -> Unit
    ) {
        getDeletedRecords(restRepository) { responseJson ->
            decodeDeletedRecords(responseJson.getSafeArray("__ENTITIES")) { entitiesList ->
                onResult(entitiesList)
            }
        }
    }

    private fun getDeletedRecords(
        restRepository: RestRepository,
        onResult: (responseJson: JSONObject) -> Unit
    ) {
        val predicate =
            DeletedRecord.buildStampPredicate(BaseApp.sharedPreferencesHolder.deletedRecordsStamp)
        Timber.d("Performing data request, with predicate $predicate")

        restRepository.getEntities(
            filter = predicate
        ) { isSuccess, response, error ->
            if (isSuccess) {
                response?.body()?.let { responseBody ->

                    retrieveJSONObject(responseBody.string())?.let { responseJson ->

                        BaseApp.sharedPreferencesHolder.deletedRecordsStamp =
                            BaseApp.sharedPreferencesHolder.globalStamp

                        onResult(responseJson)
                    }
                }
            } else {
                treatFailure(response, error, DELETED_RECORDS)
            }
        }
    }

    private fun decodeDeletedRecords(
        entitiesJsonArray: JSONArray?,
        onResult: (entitiesList: List<String>) -> Unit
    ) {
        val entitiesList: List<String>? = entitiesJsonArray?.getObjectListAsString()
        entitiesList?.let {
            onResult(entitiesList)
        }
    }

    override fun onCleared() {
        super.onCleared()
        restRepository.disposable.dispose()
    }

    fun refreshRestRepository(apiService: ApiService) {
        restRepository = RestRepository(DELETED_RECORDS, apiService)
    }
}
