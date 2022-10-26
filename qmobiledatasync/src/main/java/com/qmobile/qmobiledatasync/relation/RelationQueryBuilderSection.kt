
package com.qmobile.qmobiledatasync.relation

import androidx.sqlite.db.SimpleSQLiteQuery
import java.lang.StringBuilder

const val SOURCE_TABLE_SQL_NAME = "SOURCE"
const val DESTINATION_TABLE_SQL_NAME = "DESTINATION"

object RelationQueryBuilderSection {

    fun createQuery(relation: Relation): SimpleSQLiteQuery {
        val builder = StringBuilder()
        builder.append(depthRelation(relation))
        return SimpleSQLiteQuery(builder.toString())
    }

    private fun depthRelation(relation: Relation): String {
        val query = StringBuilder()
        query.append(partQuery(relation))
        return query.toString()
    }

    private fun partQuery(relation: Relation): String {
        return " AS $SOURCE_TABLE_SQL_NAME LEFT  JOIN ${relation.dest} " +
            " $DESTINATION_TABLE_SQL_NAME WHERE " +
            "$DESTINATION_TABLE_SQL_NAME.__KEY = $SOURCE_TABLE_SQL_NAME.__${relation.name}KEY"
    }
}
