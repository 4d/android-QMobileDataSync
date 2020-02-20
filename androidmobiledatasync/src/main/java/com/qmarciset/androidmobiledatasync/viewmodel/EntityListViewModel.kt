/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmarciset.androidmobiledatasync.viewmodel

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.gson.Gson
import com.qmarciset.androidmobileapi.auth.AuthInfoHelper
import com.qmarciset.androidmobileapi.model.entity.Entities
import com.qmarciset.androidmobileapi.model.entity.EntityModel
import com.qmarciset.androidmobileapi.network.ApiService
import com.qmarciset.androidmobileapi.utils.RequestErrorHelper
import com.qmarciset.androidmobileapi.utils.parseJsonToType
import com.qmarciset.androidmobiledatastore.db.AppDatabaseInterface
import com.qmarciset.androidmobiledatasync.DataSyncState
import com.qmarciset.androidmobiledatasync.utils.FromTableForViewModel
import okhttp3.ResponseBody
import timber.log.Timber

@Suppress("UNCHECKED_CAST")
open class EntityListViewModel<T>(
    application: Application,
    tableName: String,
    appDatabase: AppDatabaseInterface,
    apiService: ApiService,
    private val fromTableForViewModel: FromTableForViewModel
) :
    BaseViewModel<T>(application, tableName, appDatabase, apiService) {

    init {
        Timber.i("EntityListViewModel initializing...")
    }

    private val authInfoHelper = AuthInfoHelper(application.applicationContext)
    private var hasGlobalStamp = fromTableForViewModel.hasGlobalStampPropertyFromTable(tableName)
    private val gson = Gson()

    /**
     * LiveData
     */

    open var entityList: LiveData<List<T>> = roomRepository.getAll()

    open val dataLoading = MutableLiveData<Boolean>().apply { value = false }

    open val globalStamp = MutableLiveData<Int>().apply { value = authInfoHelper.globalStamp }

    val dataSynchronized =
        MutableLiveData<DataSyncState>().apply { value = DataSyncState.UNSYNCHRONIZED }

    fun delete(item: EntityModel) {
        roomRepository.delete(item as T)
    }

    fun deleteAll() {
        roomRepository.deleteAll()
    }

    fun insert(item: EntityModel) {
        roomRepository.insert(item as T)
    }

    fun insertAll(items: List<EntityModel>) {
        roomRepository.insertAll(items as List<T>)
    }

    /**
     * Gets all entities more recent than current globalStamp
     */
    fun getData(
        onResult: (shouldSyncData: Boolean) -> Unit
    ) {
        val predicate = buildGlobalStampPredicate(globalStamp.value ?: authInfoHelper.globalStamp)
        Timber.d("predicate = $predicate")

        restRepository.getMoreRecentEntities(predicate) { isSuccess, response, error ->
            if (isSuccess) {
                response?.body()?.let {
                    Timber.d("getData: going to handleData()")
                    decodeGlobalStamp(it) { entities ->

                        val receivedGlobalStamp = entities?.__GlobalStamp ?: 0

                        globalStamp.postValue(receivedGlobalStamp)
//                        globalStamp.postValue(260)

                        if (receivedGlobalStamp > authInfoHelper.globalStamp) {
                            onResult(true)
                        }

                        decodeEntityList(entities)
                    }
                }
                onResult(false)
            } else {
                RequestErrorHelper.handleError(error)
                onResult(false)
            }
        }
    }

    /**
     * Gets all entities
     */
    fun getAll() {
        dataLoading.value = true
        restRepository.getAll { isSuccess, response, error ->
            dataLoading.value = false
            if (isSuccess) {
                response?.body()?.let {
                    decodeGlobalStamp(it) { entities -> decodeEntityList(entities) }
                }
            } else {
                toastMessage.postValue("try_refresh_data")
                RequestErrorHelper.handleError(error)
            }
        }
    }

    /**
     * Retrieves data from response and insert it in database
     */
    private fun decodeGlobalStamp(
        responseBody: ResponseBody,
        onResult: (entities: Entities?) -> Unit
    ) {
        val json = responseBody.string()
        val entities = gson.parseJsonToType<Entities>(json)
        onResult(entities)
    }

    private fun decodeEntityList(entities: Entities?) {
        val entityList: List<T>? = gson.parseJsonToType(entities?.__ENTITIES)
        entityList?.let {
            for (item in entityList) {
                val itemJson = gson.toJson(item)
                val entity: EntityModel? =
                    fromTableForViewModel.parseEntityFromTable(getAssociatedTableName(), itemJson.toString())
                entity.let {
                    this.insert(it as EntityModel)
                }
            }
        }
    }

    /**
     * Returns predicate for requests with __GlobalStamp
     */
    private fun buildGlobalStampPredicate(globalStamp: Int): String {
        return "\"__GlobalStamp > $globalStamp\""
    }

    class EntityListViewModelFactory(
        private val application: Application,
        private val tableName: String,
        private val appDatabase: AppDatabaseInterface,
        private val apiService: ApiService,
        private val fromTableForViewModel: FromTableForViewModel
    ) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return fromTableForViewModel.entityListViewModelFromTable(
                application,
                tableName,
                appDatabase,
                apiService,
                fromTableForViewModel
            ) as T
        }
    }
}
