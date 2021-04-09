/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.sqlite.db.SupportSQLiteQuery
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.qmobile.qmobileapi.auth.AuthInfoHelper
import com.qmobile.qmobileapi.model.entity.DeletedRecord
import com.qmobile.qmobileapi.model.entity.Entities
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobileapi.network.ApiService
import com.qmobile.qmobileapi.repository.RestRepository
import com.qmobile.qmobileapi.utils.DELETED_RECORDS
import com.qmobile.qmobileapi.utils.getSafeArray
import com.qmobile.qmobileapi.utils.getSafeInt
import com.qmobile.qmobileapi.utils.getSafeString
import com.qmobile.qmobileapi.utils.getStringList
import com.qmobile.qmobileapi.utils.parseJsonToType
import com.qmobile.qmobiledatastore.data.RoomRelation
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.relation.ManyToOneRelation
import com.qmobile.qmobiledatasync.relation.OneToManyRelation
import com.qmobile.qmobiledatasync.relation.RelationHelper
import com.qmobile.qmobiledatasync.relation.RelationTypeEnum
import com.qmobile.qmobiledatasync.sync.DataSyncStateEnum
import okhttp3.internal.http2.Http2
import okio.ByteString.Companion.toByteString
import org.json.JSONArray
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.HTTP
import timber.log.Timber
import java.io.OutputStreamWriter
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.nio.charset.StandardCharsets.UTF_8

abstract class EntityListViewModel<T : EntityModel>(
    tableName: String,
    apiService: ApiService
) : BaseViewModel<T>(tableName, apiService) {

    init {
        Timber.i("EntityListViewModel initializing... $tableName")
    }

    val authInfoHelper = AuthInfoHelper.getInstance(BaseApp.instance)

    val relations =
        BaseApp.fromTableForViewModel.getRelations<T>(tableName, BaseApp.instance)

    private val gson = Gson()

    /**
     * LiveData
     */
//    open var entityList: LiveData<List<T>> = roomRepository.getAll()

    fun getAllDynamicQuery(sqLiteQuery: SupportSQLiteQuery): LiveData<List<T>> =
        roomRepository.getAllDynamicQuery(sqLiteQuery)

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
        val predicate = buildPredicate(globalStamp.value ?: authInfoHelper.globalStamp)
        Timber.d("Performing data request, with predicate $predicate")

        val jsonRequestBody = buildPostRequestBody()
        Timber.d("Json body ${getAssociatedTableName()} : $jsonRequestBody")
        //Timber.d("UserInfo :: ${JsonParser().parse(authInfoHelper.userInfo).asJsonObject.toString()}")
        //Timber.d("TEST :: ${URLEncoder.encode(authInfoHelper.userInfo,"utf-8")} --- ${authInfoHelper.userInfo}")

        val paramsEncoded =  "'"+URLEncoder.encode(authInfoHelper.userInfo, StandardCharsets.UTF_8.toString())+"'" // String encoded
        dataLoading.value = true
        restRepository.getEntitiesExtendedAttributes(
            jsonRequestBody = jsonRequestBody,
            filter = predicate,
            params = paramsEncoded
        ) { isSuccess, response, error ->
            dataLoading.value = false
            if (isSuccess) {
                response?.body()?.let {
                    Entities.decodeEntities<T>(gson, it) { entities ->

                        val receivedGlobalStamp = entities?.__GlobalStamp ?: 0

                        globalStamp.postValue(receivedGlobalStamp)

                        if (receivedGlobalStamp > authInfoHelper.globalStamp) {
                            onResult(true)
                        } else {
                            onResult(false)
                        }
                        decodeEntityModel(entities, false)
                    }
                }
            } else {
                // send previous globalStamp value for data sync
                globalStamp.postValue(0)
                response?.let { toastMessage.showError(it, getAssociatedTableName()) }
                error?.let { toastMessage.showError(it,getAssociatedTableName()) }
                onResult(false)
            }
        }
    }

    fun getDeletedRecords(
        onResult: (deletedRecordList: List<DeletedRecord>) -> Unit
    ) {
        getDeletedRecords(gson, restRepository, authInfoHelper) { entities ->
            decodeDeletedRecords(gson, entities) { deletedRecords ->
                onResult(deletedRecords)
            }
        }
    }

    private fun getDeletedRecords(
        gson: Gson,
        restRepository: RestRepository,
        authInfoHelper: AuthInfoHelper,
        onResult: (entities: Entities<DeletedRecord>?) -> Unit
    ) {
        val predicate = DeletedRecord.buildStampPredicate(authInfoHelper.deletedRecordsStamp)
        Timber.d("Performing data request, with predicate $predicate")

        restRepository.getEntities(
            tableName = DELETED_RECORDS,
            filter = predicate
        ) { isSuccess, response, error ->
            if (isSuccess) {
                response?.body()?.let {
                    Entities.decodeEntities<DeletedRecord>(gson, it) { entities ->

                        authInfoHelper.deletedRecordsStamp = authInfoHelper.globalStamp

                        onResult(entities)
                    }
                }
            } else {
                response?.let { toastMessage.showError(it,getAssociatedTableName()) }
                error?.let { toastMessage.showError(it,getAssociatedTableName()) }
            }
        }
    }

    fun decodeDeletedRecords(
        gson: Gson,
        entities: Entities<DeletedRecord>?,
        onResult: (deletedRecordList: List<DeletedRecord>) -> Unit
    ) {
        val jsonString = gson.toJson(entities?.__ENTITIES)
        val deletedRecordList = gson.parseJsonToType<List<DeletedRecord>>(jsonString)
        deletedRecordList?.let {
            onResult(deletedRecordList)
        }
    }

    /**
     * Decodes the list of entities retrieved
     */
    fun decodeEntityModel(entities: Entities<T>?, fetchedFromRelation: Boolean) {
        val entitiesJsonString = gson.toJson(entities?.__ENTITIES)

        val entitiesList = JSONArray(entitiesJsonString).getStringList()
        for (entityJsonString in entitiesList) {
            Timber.d("decodeEntityModel called. Extracted from relation ? $fetchedFromRelation")
            val entity: EntityModel? =
                BaseApp.fromTableForViewModel.parseEntityFromTable(
                    getAssociatedTableName(),
                    entityJsonString,
                    fetchedFromRelation
                )
            entity?.let {
                this.insert(it)
                if (!fetchedFromRelation)
                    checkRelations(entityJsonString)
            }
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

    // Map<entityKey, Map<relationName, LiveData<RoomRelation>>>
    abstract fun getManyToOneRelationKeysFromEntityList(
        entityList: List<EntityModel>
    ): MutableMap<String, MutableMap<String, LiveData<RoomRelation>>>
}

class TestJson(firstName: String,lastName: String)