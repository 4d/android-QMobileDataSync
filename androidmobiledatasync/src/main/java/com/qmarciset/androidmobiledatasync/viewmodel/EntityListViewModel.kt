/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmarciset.androidmobiledatasync.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.qmarciset.androidmobileapi.auth.AuthInfoHelper
import com.qmarciset.androidmobileapi.model.entity.DeletedRecord
import com.qmarciset.androidmobileapi.model.entity.Entities
import com.qmarciset.androidmobileapi.model.entity.EntityModel
import com.qmarciset.androidmobileapi.network.ApiService
import com.qmarciset.androidmobileapi.utils.RequestErrorHelper
import com.qmarciset.androidmobileapi.utils.parseJsonToType
import com.qmarciset.androidmobiledatasync.app.BaseApp
import com.qmarciset.androidmobiledatasync.relation.ManyToOneRelation
import com.qmarciset.androidmobiledatasync.relation.OneToManyRelation
import com.qmarciset.androidmobiledatasync.relation.RelationHelper
import com.qmarciset.androidmobiledatasync.relation.RelationType
import com.qmarciset.androidmobiledatasync.sync.DataSyncState
import timber.log.Timber

open class EntityListViewModel<T : EntityModel>(
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

    open var entityList: LiveData<List<T>> = roomRepository.getAll()

    var dataLoading = MutableLiveData<Boolean>().apply { value = false }

    open val globalStamp = MutableLiveData<Int>().apply { value = authInfoHelper.globalStamp }

    val dataSynchronized =
        MutableLiveData<DataSyncState>().apply { value = DataSyncState.UNSYNCHRONIZED }

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
        Timber.d("Json body : $jsonRequestBody")

        dataLoading.value = true
        restRepository.getEntitiesExtendedAttributes(
            jsonRequestBody = jsonRequestBody,
            filter = predicate
        ) { isSuccess, response, error ->
            dataLoading.value = false
            if (isSuccess) {
                response?.body()?.let {
                    Entities.decodeEntities<T>(gson, it) { entities ->

                        val receivedGlobalStamp = entities?.__GlobalStamp ?: 0

                        globalStamp.postValue(receivedGlobalStamp)
                        // For test purposes
//                        if (getAssociatedTableName() == "Service")
//                             globalStamp.postValue(248)
//                        else
//                            globalStamp.postValue(245)

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
                RequestErrorHelper.handleError(error)
                onResult(false)
            }
        }
    }

    fun getDeletedRecords(
        onResult: (deletedRecordList: List<DeletedRecord>) -> Unit
    ) {
        DeletedRecord.getDeletedRecords(gson, restRepository, authInfoHelper) { entities ->
            decodeDeletedRecords(gson, entities) { deletedRecords ->
                onResult(deletedRecords)
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
        val jsonString = gson.toJson(entities?.__ENTITIES)
        val entityList = BaseApp.fromTableForViewModel.parseEntityListFromTable<T>(
            getAssociatedTableName(),
            jsonString
        )
        entityList?.let {
            for (item in entityList) {
                val itemJson = gson.toJson(item)
                val entity: EntityModel? =
                    BaseApp.fromTableForViewModel.parseEntityFromTable(
                        getAssociatedTableName(),
                        itemJson.toString()
                    )
                entity?.let {
                    this.insert(it)
                    if (!fetchedFromRelation)
                        checkRelations(it)
                }
            }
        }
    }

    /**
     * Checks in the retrieved [entity] if there is a provided relation. If there is any, we want it
     * to be added in the appropriate Room dao
     */
    fun checkRelations(entity: EntityModel) {
        for (relation in relations) {
            if (relation.relationType == RelationType.MANY_TO_ONE) {
                val relatedEntity =
                    RelationHelper.getRelatedEntity<EntityModel>(entity, relation.relationName)
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
                    RelationHelper.getRelatedEntity<Entities<EntityModel>>(
                        entity,
                        relation.relationName
                    )
                if ((relatedEntities?.__COUNT ?: 0) > 0) {
                    relatedEntities?.__DATACLASS?.let {
                        newRelatedEntities.postValue(
                            OneToManyRelation(
                                entities = relatedEntities,
                                className = it
                            )
                        )
                    }
                }
            }
        }
    }
}
