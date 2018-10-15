/*
 * Module: r2-testapp-kotlin
 * Developers: Aferdita Muriqi, Paul Stoica
 *
 * Copyright (c) 2018. European Digital Reading Lab. All rights reserved.
 * Licensed to the Readium Foundation under one or more contributor license agreements.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.testapp

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import org.jetbrains.anko.db.*
import org.json.JSONArray


class PositionsDatabase(context: Context) {

    val shared: PositionsDatabaseOpenHelper = PositionsDatabaseOpenHelper(context)
    var positions: POSITIONS

    init {
        positions = POSITIONS(shared)
    }

}


class PositionsDatabaseOpenHelper(ctx: Context) : ManagedSQLiteOpenHelper(ctx, "positions_database", null, PositionsDatabaseOpenHelper.DATABASE_VERSION) {
    companion object {
        private var instance: PositionsDatabaseOpenHelper? = null
        private val DATABASE_VERSION = 1

        @Synchronized
        fun getInstance(ctx: Context): PositionsDatabaseOpenHelper {
            if (instance == null) {
                instance = PositionsDatabaseOpenHelper(ctx.applicationContext)
            }
            return instance!!
        }
    }

    override fun onCreate(db: SQLiteDatabase) {

        db.createTable(POSITIONSTable.NAME, true,
                POSITIONSTable.ID to INTEGER + PRIMARY_KEY + AUTOINCREMENT,
                POSITIONSTable.PUBLICATION_ID to TEXT,
                POSITIONSTable.POSITIONS to TEXT)
    }


    override fun onUpgrade(p0: SQLiteDatabase?, p1: Int, p2: Int) {
        //TODO
    }

}


object POSITIONSTable {
    const val NAME = "POSITIONS"
    const val ID = "id"
    const val PUBLICATION_ID = "publicationID"
    const val POSITIONS = "positions"
    var RESULT_COLUMNS = arrayOf(POSITIONSTable.ID, POSITIONSTable.PUBLICATION_ID, POSITIONSTable.POSITIONS)

}


class POSITIONS(private var database: PositionsDatabaseOpenHelper) {
    fun dropTable() {
        database.use {
            dropTable(POSITIONSTable.NAME, true)
        }
    }

    fun emptyTable() {
        database.use {
            delete(POSITIONSTable.NAME, "")
        }
    }

    fun storeSyntheticPageList(publicationID: String, synthecticPageList: JSONArray) {
        database.use {
            insert(POSITIONSTable.NAME,
                    POSITIONSTable.PUBLICATION_ID to publicationID,
                    POSITIONSTable.POSITIONS to synthecticPageList.toString())
        }
    }


    fun getSyntheticPageList(publicationID: String): JSONArray? {
        return database.use {
            select(POSITIONSTable.NAME,
                    POSITIONSTable.POSITIONS)
                    .whereArgs("publicationID = {publicationID}", "publicationID" to publicationID)
                    .exec {
                        parseOpt(MyRowParser())
                    }
        }
    }


    fun getCurrentPage(publicationID: String, href: String, progression: Double): Long {
        var currentPage: Long = 0

        val pageList = database.use {
            return@use select(POSITIONSTable.NAME,
                    POSITIONSTable.POSITIONS)
                    .whereArgs("(publicationID = {publicationID})","publicationID" to publicationID)
                    .exec {
                        parseOpt(MyRowParser())!!
                    }
        }

        for (i in 1 until pageList.length()) {
            val jsonObjectBefore = pageList.getJSONObject(i-1)
            val jsonObjectAfter = pageList.getJSONObject(i)
            if (jsonObjectBefore.getString("href") == href && jsonObjectBefore.getDouble("progression") <= progression && jsonObjectAfter.getDouble("progression") >= progression) {
                currentPage = jsonObjectBefore.getLong("pageNumber")
            }
        }

        return currentPage
    }


    fun has(publicationID: String): Boolean {
        var isGenerated = false

        val pageList = (database.use {
            select(POSITIONSTable.NAME,
                    POSITIONSTable.POSITIONS)
                    .whereArgs("publicationID = {publicationID}", "publicationID" to publicationID)
                    .exec {
                        parseOpt(MyRowParser())
                    }
        })

        pageList?.let {
            if (pageList.length() > 0) {
                isGenerated = true
            }
        }

        return isGenerated
    }


    class MyRowParser : RowParser<JSONArray> {
        override fun parseRow(columns: Array<Any?>): JSONArray {
            val pageList = columns[0]?.let {
                return@let it
            } ?: kotlin.run { return@run 0 }

            return JSONArray(pageList as String)
        }
    }


}