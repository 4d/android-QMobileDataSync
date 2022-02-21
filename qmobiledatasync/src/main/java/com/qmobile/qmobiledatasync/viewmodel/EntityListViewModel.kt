/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.viewmodel

import androidx.lifecycle.asFlow
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.sqlite.db.SupportSQLiteQuery
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobileapi.network.ApiService
import com.qmobile.qmobileapi.utils.UTF8
import com.qmobile.qmobileapi.utils.getObjectListAsString
import com.qmobile.qmobileapi.utils.getSafeArray
import com.qmobile.qmobileapi.utils.getSafeInt
import com.qmobile.qmobileapi.utils.getSafeObject
import com.qmobile.qmobileapi.utils.retrieveJSONObject
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.relation.JSONRelation
import com.qmobile.qmobiledatasync.relation.RelationHelper
import com.qmobile.qmobiledatasync.relation.RelationTypeEnum
import com.qmobile.qmobiledatasync.sync.DataSyncStateEnum
import com.qmobile.qmobiledatasync.sync.GlobalStamp
import com.qmobile.qmobiledatasync.utils.ScheduleRefreshEnum
import com.qmobile.qmobiledatasync.utils.getViewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.shareIn
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.net.URLEncoder
import java.util.concurrent.atomic.AtomicBoolean

abstract class EntityListViewModel<T : EntityModel>(
    tableName: String,
    apiService: ApiService
) : BaseViewModel<T>(tableName, apiService) {

    init {
        Timber.v("EntityListViewModel initializing... $tableName")
    }

    companion object {
        private const val DEFAULT_ROOM_PAGE_SIZE = 60
        private const val DEFAULT_REST_PAGE_SIZE = 50
    }

    val coroutineScope = getViewModelScope()

    /**
     * LiveData
     */

    private val searchChanel = MutableStateFlow<SupportSQLiteQuery?>(null)
    // We will use a StateFlow as this will only broadcast
    // the most recent sent element to all the subscribers

    fun setSearchQuery(sqLiteQuery: SupportSQLiteQuery) {
        searchChanel.value = sqLiteQuery
    }

    val entityListPagedListSharedFlow: SharedFlow<PagedList<T>> =
        searchChanel
            .filterNotNull()
            .flatMapLatest {
                // We use flatMapLatest as we don't want flows of flows and
                // we only want to query the latest searched string.
                LivePagedListBuilder(roomRepository.getAllPagedList(it), DEFAULT_ROOM_PAGE_SIZE)
                    .build().asFlow()
            }.catch { throwable ->
                Timber.e("Error while getting entityListPagedListSharedFlow in EntityListViewModel of [$tableName]")
                Timber.e(throwable.localizedMessage)
            }.shareIn(coroutineScope, SharingStarted.WhileSubscribed())

    val entityListPagingDataFlow: Flow<PagingData<T>> =
        searchChanel
            .filterNotNull()
            .flatMapLatest { query ->
                // We use flatMapLatest as we don't want flows of flows and
                // we only want to query the latest searched string.
                roomRepository.getAllPagingData(
                    sqLiteQuery = query,
                    pagingConfig = PagingConfig(
                        pageSize = DEFAULT_ROOM_PAGE_SIZE,
                        enablePlaceholders = false
                    )
                )
            }.catch { throwable ->
                Timber.e("Error while getting entityListPagingDataFlow in EntityListViewModel of [$tableName]")
                Timber.e(throwable.localizedMessage)
            }

    private val _dataLoading = MutableStateFlow(false)
    val dataLoading: StateFlow<Boolean> = _dataLoading

    private val _dataSynchronized = MutableStateFlow(DataSyncStateEnum.UNSYNCHRONIZED)
    open val dataSynchronized: StateFlow<DataSyncStateEnum> = _dataSynchronized

    private val _globalStamp =
        MutableStateFlow(newGlobalStamp(BaseApp.sharedPreferencesHolder.globalStamp))
    open val globalStamp: StateFlow<GlobalStamp> = _globalStamp

    private val _scheduleRefresh = MutableStateFlow(ScheduleRefreshEnum.NO)
    val scheduleRefresh: StateFlow<ScheduleRefreshEnum> = _scheduleRefresh

    private val _jsonRelation = MutableSharedFlow<JSONRelation>(replay = 1)
    val jsonRelation: SharedFlow<JSONRelation> = _jsonRelation

    open val isToSync = AtomicBoolean(false)

    /**
     * Gets all entities more recent than current globalStamp
     */
    fun getEntities(
        onResult: (shouldSyncData: Boolean) -> Unit
    ) {

        var iter = 0
        var totalReceived = 0

        _dataLoading.value = true

        fun paging(
            onResult: (shouldSyncData: Boolean) -> Unit
        ) {
            performRequest(
                iter = iter,
                totalReceived = totalReceived
            ) { isSuccess, hasFinished, receivedFromIter, shouldSyncData ->

                if (isSuccess) {
                    if (hasFinished) {
                        onResult(shouldSyncData)
                        _dataLoading.value = false
                        return@performRequest
                    } else {
                        iter++
                        totalReceived += receivedFromIter
                        paging(onResult)
                    }
                } else {
                    onResult(shouldSyncData)
                    _dataLoading.value = false
                    return@performRequest
                }
            }
        }

        paging(onResult)
    }

    private fun performRequest(
        iter: Int,
        totalReceived: Int,
        onResult: (isSuccess: Boolean, hasFinished: Boolean, receivedFromIter: Int, shouldSyncData: Boolean) -> Unit
    ) {

        val predicate: String? = buildPredicate()
        Timber.d("Performing data request, with predicate $predicate")

        val jsonRequestBody = buildPostRequestBody()
        Timber.d("Json body ${getAssociatedTableName()} : $jsonRequestBody")

        val paramsEncoded = "'" + URLEncoder.encode(
            BaseApp.sharedPreferencesHolder.userInfo,
            UTF8
        ) + "'" // String encoded

        restRepository.getEntitiesExtendedAttributes(
            jsonRequestBody = jsonRequestBody,
            filter = predicate,
            paramsEncoded = paramsEncoded,
            iter = iter,
            limit = DEFAULT_REST_PAGE_SIZE
        ) { isSuccess, response, error ->

            if (isSuccess) {
                response?.body()?.let { responseBody ->

                    retrieveJSONObject(responseBody.string())?.let { responseJson ->

                        responseJson.getSafeArray("__ENTITIES")?.let { jsonArray ->

                            val receivedFromIter = jsonArray.length()

                            responseJson.getSafeInt("__COUNT")?.let { count ->
                                if (count <= totalReceived + receivedFromIter) { // it's time to stop

                                    val receivedGlobalStamp =
                                        responseJson.getSafeInt("__GlobalStamp") ?: 0

                                    _globalStamp.value = newGlobalStamp(receivedGlobalStamp)

                                    if (receivedGlobalStamp > BaseApp.sharedPreferencesHolder.globalStamp) {
                                        onResult(true, true, receivedFromIter, true)
                                    } else {
                                        onResult(true, true, receivedFromIter, false)
                                    }
                                } else {
                                    onResult(true, false, receivedFromIter, false)
                                }
                            }

                            decodeEntities(jsonArray, false)
                        }
                    }
                }
            } else {
                // send previous globalStamp value for data sync
                _globalStamp.value = newGlobalStamp(0)
                response?.let { toastMessage.showMessage(it, getAssociatedTableName()) }
                error?.let { toastMessage.showMessage(it, getAssociatedTableName()) }
                onResult(false, true, 0, false)
            }
        }
    }

    /**
     * Decodes the list of entities retrieved
     */
    fun decodeEntities(entitiesJsonArray: JSONArray?, fetchedFromRelation: Boolean) {

        val parsedList: MutableList<EntityModel> = mutableListOf()
        entitiesJsonArray?.getObjectListAsString()?.forEach { entityJsonString ->
            Timber.d("decodeEntityModel called. Extracted from relation ? $fetchedFromRelation")
            val entity: EntityModel? =
                BaseApp.genericTableHelper.parseEntityFromTable(
                    getAssociatedTableName(),
                    entityJsonString,
                    fetchedFromRelation
                )
            entity?.let {
                parsedList.add(it)
                if (!fetchedFromRelation)
                    checkRelations(entityJsonString)
            }
        }
        this.insertAll(parsedList)
    }

    /**
     * Checks in the retrieved [entityJsonString] if there is a provided relation. If there is any, we want it
     * to be added in the appropriate Room dao
     */
    fun checkRelations(entityJsonString: String) {
        RelationHelper.getRelations(getAssociatedTableName()).forEach { relation ->
            JSONObject(entityJsonString).getSafeObject(relation.name)?.let { relatedJson ->
                val jsonRelation = if (relatedJson.getSafeInt("__COUNT") == null)
                    JSONRelation(relatedJson, relation.dest, RelationTypeEnum.MANY_TO_ONE)
                else
                    JSONRelation(relatedJson, relation.dest, RelationTypeEnum.ONE_TO_MANY)
                _jsonRelation.tryEmit(jsonRelation)
            }
        }
    }

    fun insertRelation(jsonRelation: JSONRelation) {
        if (jsonRelation.type == RelationTypeEnum.ONE_TO_MANY)
            jsonRelation.getEntities().forEach { this.insert(it) }
        else
            jsonRelation.getEntity()?.let { this.insert(it) }
    }

    fun setDataSyncState(state: DataSyncStateEnum) {
        _dataSynchronized.value = state
    }

    fun setDataLoadingState(startLoading: Boolean) {
        _dataLoading.value = startLoading
    }

    fun setScheduleRefreshState(scheduleRefresh: ScheduleRefreshEnum) {
        _scheduleRefresh.value = scheduleRefresh
    }

    private fun newGlobalStamp(globalStamp: Int): GlobalStamp =
        GlobalStamp(
            tableName = this.getAssociatedTableName(),
            stampValue = globalStamp,
            dataSyncProcess = this.dataSynchronized.value == DataSyncStateEnum.SYNCHRONIZING ||
                this.dataSynchronized.value == DataSyncStateEnum.RESYNC
        )
}
