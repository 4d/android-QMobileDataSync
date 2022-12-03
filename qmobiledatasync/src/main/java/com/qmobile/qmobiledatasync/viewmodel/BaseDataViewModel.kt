/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.viewmodel

import com.qmobile.qmobileapi.model.action.ActionResponse
import com.qmobile.qmobileapi.model.entity.DeletedRecord
import com.qmobile.qmobileapi.network.ApiService
import com.qmobile.qmobileapi.repository.RestRepository
import com.qmobile.qmobileapi.utils.DELETED_RECORDS
import com.qmobile.qmobileapi.utils.getObjectListAsString
import com.qmobile.qmobileapi.utils.getSafeArray
import com.qmobile.qmobileapi.utils.getSafeString
import com.qmobile.qmobileapi.utils.retrieveJSONObject
import com.qmobile.qmobileapi.utils.retrieveResponseObject
import com.qmobile.qmobiledatastore.dao.BaseDao
import com.qmobile.qmobiledatastore.data.RoomData
import com.qmobile.qmobiledatastore.data.RoomEntity
import com.qmobile.qmobiledatastore.repository.RoomRepository
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.toast.ToastMessage
import okhttp3.RequestBody
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber

/**
 * If you need to use context inside your viewmodel you should use AndroidViewModel, because it
 * contains the application context (to retrieve the context call getApplication() ), otherwise use
 * regular ViewModel.
 */
abstract class BaseDataViewModel<T : Any>(
    private val tableName: String,
    apiService: ApiService
) : BaseViewModel() {

    open fun getAssociatedTableName(): String = tableName
    private val originalAssociatedTableName = BaseApp.runtimeDataHolder.tableInfo[tableName]?.originalName ?: ""

    /**
     * DAO
     */

    var dao: BaseDao<RoomEntity, RoomData> = BaseApp.daoProvider.getDao(tableName)

    /**
     * Repositories
     */

    val roomRepository: RoomRepository<RoomData> = RoomRepository(dao)
    var restRepository: RestRepository =
        RestRepository(originalAssociatedTableName, apiService)

    fun refreshRestRepository(apiService: ApiService) {
        restRepository = RestRepository(originalAssociatedTableName, apiService)
    }

    override fun onCleared() {
        super.onCleared()
        restRepository.disposable.dispose()
    }

    fun sendAction(
        actionName: String,
        actionContent: MutableMap<String, Any>,
        onResult: (actionResponse: ActionResponse?) -> Unit
    ) {
        restRepository.sendAction(
            actionName,
            actionContent
        ) { isSuccess, response, error ->
            if (isSuccess) {
                response?.body()?.let { responseBody ->
                    retrieveResponseObject<ActionResponse>(
                        BaseApp.mapper,
                        responseBody.string()
                    )?.let { actionResponse ->
                        if (actionResponse.success) {
                            toastMessage.showMessage(
                                actionResponse.statusText,
                                getAssociatedTableName(),
                                ToastMessage.Type.SUCCESS
                            )
                        } else {
                            toastMessage.showMessage(
                                actionResponse.statusText,
                                getAssociatedTableName(),
                                ToastMessage.Type.NEUTRAL
                            )
                        }
                        onResult(actionResponse)
                    }
                }
            } else {
                treatFailure(response, error, getAssociatedTableName(), ToastMessage.Type.ERROR)
                onResult(null)
            }
        }
    }

    fun uploadImage(
        imagesToUpload: Map<String, RequestBody?>,
        onImageUploaded: (parameterName: String, receivedId: String) -> Unit,
        onError: () -> Unit,
        onAllUploadFinished: () -> Unit
    ) {
        restRepository.uploadImage(
            imagesToUpload,
            { isSuccess, parameterName, response, error ->
                if (isSuccess) {
                    response?.body()?.let { responseBody ->

                        retrieveJSONObject(responseBody.string())?.let { responseJson ->
                            responseJson.getSafeString("ID")?.let { id ->
                                onImageUploaded(parameterName, id)
                            }
                        }
                    }
                } else {
                    treatFailure(response, error, getAssociatedTableName(), ToastMessage.Type.ERROR)
                    onError()
                }
            }
        ) {
            onAllUploadFinished()
        }
    }

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
            tableName = DELETED_RECORDS,
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
                treatFailure(response, error, getAssociatedTableName())
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
}