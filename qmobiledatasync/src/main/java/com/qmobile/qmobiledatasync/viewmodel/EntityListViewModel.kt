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
import com.qmobile.qmobileapi.utils.UTF8
import com.qmobile.qmobileapi.utils.getObjectListAsString
import com.qmobile.qmobileapi.utils.getSafeArray
import com.qmobile.qmobileapi.utils.getSafeInt
import com.qmobile.qmobileapi.utils.getSafeString
import com.qmobile.qmobileapi.utils.retrieveJSONObject
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.relation.ManyToOneRelation
import com.qmobile.qmobiledatasync.relation.OneToManyRelation
import com.qmobile.qmobiledatasync.relation.Relation
import com.qmobile.qmobiledatasync.relation.RelationHelper
import com.qmobile.qmobiledatasync.relation.RelationTypeEnum
import com.qmobile.qmobiledatasync.sync.DataSyncStateEnum
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.net.URLEncoder

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

    val authInfoHelper = AuthInfoHelper.getInstance(BaseApp.instance)

    val relations =
        BaseApp.genericTableHelper.getRelations<T>(tableName, BaseApp.instance)

    /**
     * LiveData
     */
//    open var entityList: LiveData<List<T>> = roomRepository.getAll()

//    fun getAllDynamicQuery(sqLiteQuery: SupportSQLiteQuery): LiveData<List<T>> =
//        roomRepository.getAllDynamicQuery(sqLiteQuery)

    private val searchChanel = MutableStateFlow<SupportSQLiteQuery?>(null)
    // We will use a ConflatedBroadcastChannel as this will only broadcast
    // the most recent sent element to all the subscribers

    fun setSearchQuery(sqLiteQuery: SupportSQLiteQuery) {
        searchChanel.value = sqLiteQuery
    }

    val entityListLiveData =
        searchChanel
            .filterNotNull()
            .flatMapLatest {
                // We use flatMapLatest as we don't want flows of flows and
                // we only want to query the latest searched string.
                getAllDynamicQueryFlow(it)
            }.catch { throwable ->
                Timber.e("Error while getting entityListLiveData in EntityListViewModel of [$tableName]")
                Timber.e(throwable.localizedMessage)
            }.asLiveData()

    private fun getAllDynamicQueryFlow(sqLiteQuery: SupportSQLiteQuery): Flow<PagedList<T>> {
        val livePagedListBuilder: LivePagedListBuilder<Int, T> = LivePagedListBuilder(
            roomRepository.getAllDynamicQuery(sqLiteQuery), DEFAULT_ROOM_PAGE_SIZE,
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

        var iter = 0
        var totalReceived = 0

        dataLoading.value = true

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
                        if (!shouldSyncData) {
                            dataLoading.value = false
                        }
                        return@performRequest
                    } else {
                        iter++
                        totalReceived += receivedFromIter
                        paging(onResult)
                    }
                } else {
                    onResult(shouldSyncData)
                    if (!shouldSyncData) {
                        dataLoading.value = false
                    }
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
            authInfoHelper.userInfo,
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

                                    val receivedGlobalStamp = responseJson.getSafeInt("__GlobalStamp") ?: 0

                                    globalStamp.postValue(receivedGlobalStamp)

                                    if (receivedGlobalStamp > authInfoHelper.globalStamp) {
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
                globalStamp.postValue(0)
                response?.let { toastMessage.showMessage(it, getAssociatedTableName()) }
                error?.let { toastMessage.showMessage(it, getAssociatedTableName()) }
                onResult(false, true, 0, false)
            }
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
    fun decodeEntities(entitiesJsonArray: JSONArray?, fetchedFromRelation: Boolean) {

        val parsedList: MutableList<EntityModel> = mutableListOf()
        val entitiesList: List<String>? = entitiesJsonArray?.getObjectListAsString()
        entitiesList?.let {
            entitiesList.forEach { entityJsonString ->
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
    }

    /**
     * Checks in the retrieved [entityJsonString] if there is a provided relation. If there is any, we want it
     * to be added in the appropriate Room dao
     */
    fun checkRelations(entityJsonString: String) {
        relations.forEach { relation ->
            RelationHelper.getRelatedEntity(entityJsonString, relation.relationName)?.let { relatedJson ->
                if (relation.relationType == RelationTypeEnum.MANY_TO_ONE) {
                    checkManyToOneRelation(relation, relatedJson)
                } else { // relationType == ONE_TO_MANY
                    checkOneToManyRelation(relatedJson)
                }
            }
        }
    }

    private fun checkManyToOneRelation(relation: Relation, relatedJson: JSONObject) {
        newRelatedEntity.postValue(
            ManyToOneRelation(
                entity = relatedJson,
                className = relation.className
            )
        )
    }

    private fun checkOneToManyRelation(relatedJson: JSONObject) {
        if (relatedJson.getSafeInt("__COUNT") ?: 0 > 0) {
            relatedJson.getSafeString("__DATACLASS")?.let { dataClass ->
                relatedJson.getSafeArray("__ENTITIES")?.let { entities ->
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

    fun insertNewRelatedEntity(manyToOneRelation: ManyToOneRelation) {
        val entity = BaseApp.genericTableHelper.parseEntityFromTable(
            tableName = manyToOneRelation.className,
            jsonString = manyToOneRelation.entity.toString(),
            fetchedFromRelation = true
        )
        entity?.let {
            this.insert(entity)
        }
    }

    fun insertNewRelatedEntities(oneToManyRelation: OneToManyRelation) {
        oneToManyRelation.entities.getObjectListAsString().forEach { entityString ->
            val entity = BaseApp.genericTableHelper.parseEntityFromTable(
                tableName = oneToManyRelation.className,
                jsonString = entityString,
                fetchedFromRelation = true
            )
            entity?.let {
                this.insert(entity)
            }
        }
    }
}
