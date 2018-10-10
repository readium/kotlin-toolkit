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

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import org.jetbrains.anko.db.*
import org.json.JSONObject
import org.readium.r2.shared.Locations
import org.readium.r2.shared.Publication


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
                POSITIONSTable.PAGE_NUMBER to INTEGER,
                POSITIONSTable.RESOURCE_INDEX to INTEGER,
                POSITIONSTable.RESOURCE_HREF to TEXT,
                POSITIONSTable.RESOURCE_PROGRESSION to REAL)
    }


    override fun onUpgrade(p0: SQLiteDatabase?, p1: Int, p2: Int) {
        //TODO
    }

}


object POSITIONSTable {
    const val NAME = "POSITIONS"
    const val ID = "id"
    const val PUBLICATION_ID = "publicationID"
    const val PAGE_NUMBER = "pageNumber"
    const val RESOURCE_INDEX = "resourceIndex"
    const val RESOURCE_HREF = "resourceHref"
    const val RESOURCE_PROGRESSION = "resourceProgression"
    var RESULT_COLUMNS = arrayOf(POSITIONSTable.ID, POSITIONSTable.PUBLICATION_ID, POSITIONSTable.PAGE_NUMBER, POSITIONSTable.RESOURCE_INDEX, POSITIONSTable.RESOURCE_HREF, POSITIONSTable.RESOURCE_PROGRESSION)

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

    fun storeSyntheticPageList(publicationID: String, synthecticPageList: MutableList<Triple<Long, String, Double>>) {
        database.use {
            for (page in synthecticPageList)
                insert(POSITIONSTable.NAME,
                        POSITIONSTable.PUBLICATION_ID to publicationID,
                        POSITIONSTable.PAGE_NUMBER to page.first,
                        POSITIONSTable.RESOURCE_HREF to page.second,
                        POSITIONSTable.RESOURCE_PROGRESSION to page.third)
        }
    }


    fun getSyntheticPageList(publicationID: String): MutableList<Triple<Long, String, Double>> {
        return database.use {
            select(POSITIONSTable.NAME,
                    POSITIONSTable.PAGE_NUMBER,
                    POSITIONSTable.RESOURCE_HREF,
                    POSITIONSTable.RESOURCE_PROGRESSION)
                    .whereArgs("publicationID = {publicationID}", "publicationID" to publicationID)
                    .orderBy(POSITIONSTable.PAGE_NUMBER, SqlOrderDirection.ASC)
                    .exec {
                        parseList(MyRowParser()).toMutableList()
                    }
        }
    }


    fun getCurrentPage(publicationID: String, href: String, progression: Double): Long {
        var currentPage: Long = 0

        val resourcePages = database.use {
            return@use select(POSITIONSTable.NAME,
                    POSITIONSTable.PAGE_NUMBER,
                    POSITIONSTable.RESOURCE_HREF,
                    POSITIONSTable.RESOURCE_PROGRESSION)
                    .whereArgs("(publicationID = {publicationID}) AND (resourceHref = {resourceHref})",
                            "publicationID" to publicationID,
                                "resourceHref" to href)
                    .exec {
                        parseList(MyRowParser()).toMutableList()
                    }
        }

        for (i in 1 until resourcePages.size) {
            if (progression >= resourcePages[i-1].third && progression <= resourcePages[i].third) {
                currentPage = resourcePages[i-1].first
            }
        }

        return currentPage
    }


    fun has(publicationID: String): Boolean {
        var isGenerated = false

        if ( (database.use {
            select(POSITIONSTable.NAME,
                    POSITIONSTable.PAGE_NUMBER,
                    POSITIONSTable.RESOURCE_HREF,
                    POSITIONSTable.RESOURCE_PROGRESSION)
                    .whereArgs("publicationID = {publicationID}", "publicationID" to publicationID)
                    .orderBy(POSITIONSTable.PAGE_NUMBER, SqlOrderDirection.ASC)
                    .exec {
                        parseList(MyRowParser()).toMutableList()
                    }
        }).isNotEmpty() ) {
            isGenerated = true
        }

        return isGenerated
    }


    class MyRowParser : RowParser<Triple<Long, String, Double>> {
        override fun parseRow(columns: Array<Any?>): Triple<Long, String, Double> {
            val pageNumber = columns[0]?.let {
                return@let it
            } ?: kotlin.run { return@run 0 }
            val resourceHref = columns[1]?.let {
                return@let it
            } ?: kotlin.run { return@run "" }
            val resourceProgression = columns[2]?.let {
                return@let it
            } ?: kotlin.run { return@run "" }

            return Triple(pageNumber as Long, resourceHref as String, resourceProgression as Double)
        }
    }


}