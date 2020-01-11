/*
 * Module: r2-testapp-kotlin
 * Developers: Aferdita Muriqi, Paul Stoica
 *
 * Copyright (c) 2018. European Digital Reading Lab. All rights reserved.
 * Licensed to the Readium Foundation under one or more contributor license agreements.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.testapp.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import org.jetbrains.anko.db.*
import org.json.JSONObject


class PositionsDatabase(context: Context) {

    val shared: PositionsDatabaseOpenHelper = PositionsDatabaseOpenHelper(context)
    var positions: POSITIONS

    init {
        positions = POSITIONS(shared)
    }

}


class PositionsDatabaseOpenHelper(ctx: Context) : ManagedSQLiteOpenHelper(ctx, "positions_database", null, DATABASE_VERSION) {
    companion object {
        private var instance: PositionsDatabaseOpenHelper? = null
        private const val DATABASE_VERSION = 1

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
                POSITIONSTable.BOOK_ID to INTEGER,
                POSITIONSTable.SYNTHETIC_PAGE_LIST to TEXT)
    }


    override fun onUpgrade(p0: SQLiteDatabase?, p1: Int, p2: Int) {
        //TODO
    }

}


object POSITIONSTable {
    const val NAME = "POSITIONS"
    const val ID = "id"
    const val BOOK_ID = "bookID"
    const val SYNTHETIC_PAGE_LIST = "syntheticPageList"
    var RESULT_COLUMNS = arrayOf(ID, BOOK_ID, SYNTHETIC_PAGE_LIST)

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

    fun init(bookID: Long) {
        database.use {
            insert(POSITIONSTable.NAME,
                    POSITIONSTable.BOOK_ID to bookID)
        }
    }

    fun isInitialized(bookID: Long): Boolean {
        var isInitialized = false

        val bookIDDB = (database.use {
            select(POSITIONSTable.NAME,
                    POSITIONSTable.BOOK_ID)
                    .whereArgs("bookID = {bookID}", "bookID" to bookID)
                    .exec {
                        parseOpt(BookIDRowParser())
                    }
        })

        if (bookIDDB == bookID) isInitialized = true

        return isInitialized
    }

    fun storeSyntheticPageList(bookID: Long, syntheticPageList: JSONObject) {
        database.use {
            update(POSITIONSTable.NAME,
                    POSITIONSTable.SYNTHETIC_PAGE_LIST to syntheticPageList.toString())
                    .whereArgs("bookID = {bookID}", "bookID" to bookID)
                    .exec()
        }
    }


    fun getSyntheticPageList(bookID: Long): JSONObject? {
        return database.use {
            select(POSITIONSTable.NAME,
                    POSITIONSTable.SYNTHETIC_PAGE_LIST)
                    .whereArgs("bookID = {bookID}", "bookID" to bookID)
                    .exec {
                        parseOpt(PageListParser())
                    }
        }
    }


    fun getCurrentPage(bookID: Long, href: String, progression: Double): Long? {
        var currentPage: Long? = null

        val jsonPageList = database.use {
            return@use select(POSITIONSTable.NAME,
                    POSITIONSTable.SYNTHETIC_PAGE_LIST)
                    .whereArgs("(bookID = {bookID})","bookID" to bookID)
                    .exec {
                        parseOpt(PageListParser())
                    }
        }

        jsonPageList?.let {
            if (jsonPageList.has("pageList")) {
                val pageList = jsonPageList.getJSONArray("pageList")

                for (i in 1 until pageList.length()) {
                    val jsonObjectBefore = pageList.getJSONObject(i - 1)
                    val jsonObject = pageList.getJSONObject(i)
                    if (jsonObjectBefore.getString("href") == href && jsonObjectBefore.getDouble("progression") <= progression && jsonObject.getDouble("progression") >= progression) {
                        currentPage = jsonObjectBefore.getLong("pageNumber")
                        break
                    }
                }
            }
        }

        return currentPage
    }


    fun has(bookID: Long): Boolean {
        var isGenerated = false

        val pageList = (database.use {
            select(POSITIONSTable.NAME,
                    POSITIONSTable.SYNTHETIC_PAGE_LIST)
                    .whereArgs("bookID = {bookID}", "bookID" to bookID)
                    .exec {
                        parseOpt(PageListParser())
                    }
        })

        pageList?.let {
            if (it.length() > 0) {
                isGenerated = true
            }
        }

        return isGenerated
    }

    fun delete(bookID: Long?) {
        bookID?.let {
            database.use {
                delete(POSITIONSTable.NAME, "bookID = {bookID}",
                        "bookID" to bookID)
            }
        }
    }


    class PageListParser : RowParser<JSONObject> {
        override fun parseRow(columns: Array<Any?>): JSONObject {
            val pageList = columns[0]?.let {
                return@let it
            } ?: kotlin.run { return@run "{}" }

            return JSONObject(pageList as String)
        }
    }

    class BookIDRowParser : RowParser<Long> {
        override fun parseRow(columns: Array<Any?>): Long {
            val bookID = columns[0]?.let {
                return@let it
            } ?: kotlin.run { return@run 0 }

            return bookID as Long
        }
    }


}