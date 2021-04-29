/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import androidx.sqlite.db.SupportSQLiteQuery
import com.qmobile.qmobileapi.auth.AuthInfoHelper
import com.qmobile.qmobileapi.model.entity.DeletedRecord
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobileapi.network.ApiService
import com.qmobile.qmobileapi.repository.RestRepository
import com.qmobile.qmobileapi.utils.DELETED_RECORDS
import com.qmobile.qmobileapi.utils.getObjectListAsString
import com.qmobile.qmobileapi.utils.getSafeArray
import com.qmobile.qmobileapi.utils.getSafeInt
import com.qmobile.qmobileapi.utils.getSafeString
import com.qmobile.qmobileapi.utils.retrieveJSONObject
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.relation.ManyToOneRelation
import com.qmobile.qmobiledatasync.relation.OneToManyRelation
import com.qmobile.qmobiledatasync.relation.RelationHelper
import com.qmobile.qmobiledatasync.relation.RelationTypeEnum
import com.qmobile.qmobiledatasync.sync.DataSyncStateEnum
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

abstract class EntityListViewModel<T : EntityModel>(
    tableName: String,
    apiService: ApiService
) : BaseViewModel<T>(tableName, apiService) {

    init {
        Timber.v("EntityListViewModel initializing... $tableName")
    }

    companion object {
        private const val DEFAULT_PAGE_SIZE = 60
    }

    val authInfoHelper = AuthInfoHelper.getInstance(BaseApp.instance)

    val relations =
        BaseApp.fromTableForViewModel.getRelations<T>(tableName, BaseApp.instance)

    val currentQuery = MutableLiveData<String>()

    /**
     * LiveData
     */
//    open var entityList: LiveData<List<T>> = roomRepository.getAll()

//    fun getAllDynamicQuery(sqLiteQuery: SupportSQLiteQuery): LiveData<List<T>> =
//        roomRepository.getAllDynamicQuery(sqLiteQuery)

    @ExperimentalCoroutinesApi
    private val searchChanel = ConflatedBroadcastChannel<SupportSQLiteQuery>()
    // We will use a ConflatedBroadcastChannel as this will only broadcast
    // the most recent sent element to all the subscribers

    @ExperimentalCoroutinesApi
    fun setSearchQuery(sqLiteQuery: SupportSQLiteQuery) {
        // We use .offer() to send the element to all the subscribers.
        searchChanel.offer(sqLiteQuery)
    }

    @FlowPreview
    @ExperimentalCoroutinesApi
    val entityListLiveData =
        searchChanel.asFlow() // asFlow() converts received elements from broadcast channels into a flow.
            .flatMapLatest {
                // We use flatMapLatest as we don't want flows of flows and
//            //we only want to query the latest searched string.
                getAllDynamicQueryFlow(it)
            }.catch { throwable ->
                Timber.e("Error while getting entityListLiveData in EntityListViewModel of [$tableName]")
                Timber.e(throwable.localizedMessage)
            }.asLiveData()

    private fun getAllDynamicQueryFlow(sqLiteQuery: SupportSQLiteQuery): Flow<PagedList<T>> {
        val livePagedListBuilder: LivePagedListBuilder<Int, T> = LivePagedListBuilder(
            roomRepository.getAllDynamicQuery(sqLiteQuery), DEFAULT_PAGE_SIZE,
        )
        return livePagedListBuilder.build().asFlow()
    }

    /*fun getAllDynamicQuery(sqLiteQuery: SupportSQLiteQuery): LiveData<PagedList<T>> {
        val livePagedListBuilder: LivePagedListBuilder<Int, T> = LivePagedListBuilder(
            roomRepository.getAllDynamicQuery(sqLiteQuery), DEFAULT_PAGE_SIZE,
        )
        return livePagedListBuilder.build()
    }*/

    var dataLoading = MutableLiveData<Boolean>().apply { value = false }

    open val globalStamp = MutableLiveData<Int>().apply { value = authInfoHelper.globalStamp }

    val dataSynchronized =
        MutableLiveData<DataSyncStateEnum>().apply { value = DataSyncStateEnum.UNSYNCHRONIZED }

    val newRelatedEntity = MutableLiveData<ManyToOneRelation>()

    val newRelatedEntities = MutableLiveData<OneToManyRelation>()

    /**
     * Gets all entities more recent than current globalStamp
     */
    fun getEntities(
        onResult: (shouldSyncData: Boolean) -> Unit
    ) {
        val predicate: String? = buildPredicate()
        Timber.d("Performing data request, with predicate $predicate")

        val jsonRequestBody = buildPostRequestBody()
        Timber.d("Json body ${getAssociatedTableName()} : $jsonRequestBody")

        val paramsEncoded = "'" + URLEncoder.encode(
            authInfoHelper.userInfo,
            StandardCharsets.UTF_8.toString()
        ) + "'" // String encoded
        dataLoading.value = true
        restRepository.getEntitiesExtendedAttributes(
            jsonRequestBody = jsonRequestBody,
            filter = predicate,
            params = paramsEncoded
        ) { isSuccess, response, error ->
            var shouldHideDataLoading = true
            if (isSuccess) {
                response?.body()?.let { responseBody ->

                    retrieveJSONObject(responseBody.string())?.let { responseJson ->

                        val receivedGlobalStamp = responseJson.getSafeInt("__GlobalStamp") ?: 0

                        globalStamp.postValue(receivedGlobalStamp)

                        if (receivedGlobalStamp > authInfoHelper.globalStamp) {
                            onResult(true)
                            shouldHideDataLoading = false
                        } else {
                            onResult(false)
                        }
                        decodeEntityModel(responseJson.getSafeArray("__ENTITIES"), false)
                    }
                }
            } else {
                // send previous globalStamp value for data sync
                globalStamp.postValue(0)
                response?.let { toastMessage.showMessage(it, getAssociatedTableName()) }
                error?.let { toastMessage.showMessage(it, getAssociatedTableName()) }
                onResult(false)
            }
            if (shouldHideDataLoading)
                dataLoading.value = false
        }
    }

    fun getDeletedRecords(
        onResult: (entitiesList: List<String>) -> Unit
    ) {
        getDeletedRecords(restRepository, authInfoHelper) { responseJson ->
            decodeDeletedRecords(responseJson.getSafeArray("__ENTITIES")) { entitiesList ->
                onResult(entitiesList)
            }
        }
    }

    private fun getDeletedRecords(
        restRepository: RestRepository,
        authInfoHelper: AuthInfoHelper,
        onResult: (responseJson: JSONObject) -> Unit
    ) {
        val predicate = DeletedRecord.buildStampPredicate(authInfoHelper.deletedRecordsStamp)
        Timber.d("Performing data request, with predicate $predicate")

        restRepository.getEntities(
            tableName = DELETED_RECORDS,
            filter = predicate
        ) { isSuccess, response, error ->
            if (isSuccess) {
                response?.body()?.let { responseBody ->

                    retrieveJSONObject(responseBody.string())?.let { responseJson ->

                        authInfoHelper.deletedRecordsStamp = authInfoHelper.globalStamp

                        onResult(responseJson)
                    }
                }
            } else {
                response?.let { toastMessage.showMessage(it, getAssociatedTableName()) }
                error?.let { toastMessage.showMessage(it, getAssociatedTableName()) }
            }
        }
    }

    fun decodeDeletedRecords(
        entitiesJsonArray: JSONArray?,
        onResult: (entitiesList: List<String>) -> Unit
    ) {
        val entitiesList: List<String>? = entitiesJsonArray?.getObjectListAsString()
        entitiesList?.let {
            onResult(entitiesList)
        }
    }

    /**
     * Decodes the list of entities retrieved
     */
    fun decodeEntityModel(entitiesJsonArray: JSONArray?, fetchedFromRelation: Boolean) {

        val parsedList: MutableList<EntityModel> = mutableListOf()
        val entitiesList: List<String>? = entitiesJsonArray?.getObjectListAsString()
        entitiesList?.let {
            for (entityJsonString in entitiesList) {
                Timber.d("decodeEntityModel called. Extracted from relation ? $fetchedFromRelation")
                val entity: EntityModel? =
                    BaseApp.fromTableForViewModel.parseEntityFromTable(
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
    }

    /**
     * Checks in the retrieved [entityJsonString] if there is a provided relation. If there is any, we want it
     * to be added in the appropriate Room dao
     */
    fun checkRelations(entityJsonString: String) {
        for (relation in relations) {
            if (relation.relationType == RelationTypeEnum.MANY_TO_ONE) {
                val relatedEntity =
                    RelationHelper.getRelatedEntity(entityJsonString, relation.relationName)
                relatedEntity?.let {
                    newRelatedEntity.postValue(
                        ManyToOneRelation(
                            entity = it,
                            className = relation.className
                        )
                    )
                }
            } else { // relationType == ONE_TO_MANY
                val relatedEntities =
                    RelationHelper.getRelatedEntity(entityJsonString, relation.relationName)
                if (relatedEntities?.getSafeInt("__COUNT") ?: 0 > 0) {
                    relatedEntities?.getSafeString("__DATACLASS")?.let { dataClass ->
                        relatedEntities.getSafeArray("__ENTITIES")?.let { entities ->
                            newRelatedEntities.postValue(
                                OneToManyRelation(
                                    entities = entities,
                                    className = dataClass.filter { !it.isWhitespace() }
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
