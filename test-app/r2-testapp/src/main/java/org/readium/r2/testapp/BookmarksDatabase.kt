/*
 * Module: r2-testapp-kotlin
 * Developers: Aferdita Muriqi, Mostapha Idoubihi, Paul Stoica
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
import org.json.JSONObject
import org.readium.r2.shared.Location
import org.readium.r2.shared.Locator


class BookmarksDatabase(context: Context) {

    val shared: BookmarksDatabaseOpenHelper = BookmarksDatabaseOpenHelper(context)
    var bookmarks: BOOKMARKS

    init {
        bookmarks = BOOKMARKS(shared)
    }

}

class BookmarksDatabaseOpenHelper(ctx: Context) : ManagedSQLiteOpenHelper(ctx, "bookmarks_database", null, 1) {
    companion object {
        private var instance: BookmarksDatabaseOpenHelper? = null

        @Synchronized
        fun getInstance(ctx: Context): BookmarksDatabaseOpenHelper {
            if (instance == null) {
                instance = BookmarksDatabaseOpenHelper(ctx.applicationContext)
            }
            return instance!!
        }
    }

    override fun onCreate(db: SQLiteDatabase) {

        db.createTable(BOOKMARKSTable.NAME, true,
                BOOKMARKSTable.ID to INTEGER + PRIMARY_KEY + AUTOINCREMENT,
                BOOKMARKSTable.BOOK_ID to INTEGER,
                BOOKMARKSTable.PUBLICATION_ID to TEXT,
                BOOKMARKSTable.RESOURCE_INDEX to INTEGER,
                BOOKMARKSTable.RESOURCE_HREF to TEXT,
                BOOKMARKSTable.RESOURCE_TITLE to TEXT,
                BOOKMARKSTable.LOCATION to TEXT,
                BOOKMARKSTable.CREATION_DATE to INTEGER)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {

        // TODO add migration to rename CREATION_DATE to CREATION_DATE

        // TODO add migration for publicationId

        // TODO add migration for location json

        // TODO add migration to convert progression into location

        // TODO add migration to remove progression

    }
}

object BOOKMARKSTable {
    const val NAME = "BOOKMARKS"
    const val ID = "id"
    const val BOOK_ID = "bookID"
    const val PUBLICATION_ID = "publicationID"
    const val RESOURCE_INDEX = "resourceIndex"
    const val RESOURCE_HREF = "resourceHref"
    const val RESOURCE_TITLE = "resourceTitle"
    const val LOCATION = "location"
    const val CREATION_DATE = "creationDat"
}

class BOOKMARKS(private var database: BookmarksDatabaseOpenHelper) {

    fun dropTable() {
        database.use {
            dropTable(BOOKMARKSTable.NAME, true)
        }
    }

    fun emptyTable() {
        database.use {
            delete(BOOKMARKSTable.NAME, "")
        }
    }

    fun insert(locator: Locator): Long? {
        if (locator.bookID < 0 ||
                locator.resourceIndex < 0 ||
                locator.location.progression!! < 0 || locator.location.progression!! > 1){
            return null
        }
        val exists = has(locator)
        if (exists.isEmpty()) {
            return database.use {
                return@use insert(BOOKMARKSTable.NAME,
                        BOOKMARKSTable.BOOK_ID to locator.bookID,
                        BOOKMARKSTable.PUBLICATION_ID to locator.publicationID,
                        BOOKMARKSTable.RESOURCE_INDEX to locator.resourceIndex,
                        BOOKMARKSTable.RESOURCE_HREF to locator.resourceHref,
                        BOOKMARKSTable.RESOURCE_TITLE to locator.resourceTitle,
                        BOOKMARKSTable.LOCATION to locator.location.toJSON().toString(),
                        BOOKMARKSTable.CREATION_DATE to locator.creationDate)
            }
        }
        return null
    }

    fun has(locator: Locator): List<Locator> {
        return database.use {
            select(BOOKMARKSTable.NAME,
                    BOOKMARKSTable.ID,
                    BOOKMARKSTable.BOOK_ID,
                    BOOKMARKSTable.PUBLICATION_ID,
                    BOOKMARKSTable.RESOURCE_INDEX,
                    BOOKMARKSTable.RESOURCE_HREF,
                    BOOKMARKSTable.RESOURCE_TITLE,
                    BOOKMARKSTable.LOCATION,
                    BOOKMARKSTable.CREATION_DATE)
                    .whereArgs("(bookID = {bookID}) AND (publicationID = {publicationID}) AND (resourceIndex = {resourceIndex}) AND (resourceHref = {resourceHref})  AND (location = {location})",
                            "bookID" to locator.bookID,
                            "publicationID" to locator.publicationID,
                            "resourceIndex" to locator.resourceIndex,
                            "resourceHref" to locator.resourceHref,
                            "location" to locator.location.toJSON().toString())
                    .exec {
                        parseList(MyRowParser())
                    }
        }
    }

    fun delete(locator: Locator) {
        database.use {
            delete(BOOKMARKSTable.NAME, "id = {id}",
                    "id" to locator.id!!)
        }
    }

    fun delete(book_id: Long?) {
        book_id?.let {
            database.use {
                delete(BOOKMARKSTable.NAME, "${BOOKMARKSTable.BOOK_ID} = {bookID}",
                        "bookID" to book_id)
            }
        }
    }

    fun listAll(): MutableList<Locator> {
        return database.use {
            select(BOOKMARKSTable.NAME,
                    BOOKMARKSTable.ID,
                    BOOKMARKSTable.BOOK_ID,
                    BOOKMARKSTable.PUBLICATION_ID,
                    BOOKMARKSTable.RESOURCE_INDEX,
                    BOOKMARKSTable.RESOURCE_HREF,
                    BOOKMARKSTable.RESOURCE_TITLE,
                    BOOKMARKSTable.LOCATION,
                    BOOKMARKSTable.CREATION_DATE)
                    .exec {
                        parseList(MyRowParser()).toMutableList()
                    }
        }
    }


    fun list(bookID: Long): MutableList<Locator> {
        return database.use {
            select(BOOKMARKSTable.NAME,
                    BOOKMARKSTable.ID,
                    BOOKMARKSTable.BOOK_ID,
                    BOOKMARKSTable.PUBLICATION_ID,
                    BOOKMARKSTable.RESOURCE_INDEX,
                    BOOKMARKSTable.RESOURCE_HREF,
                    BOOKMARKSTable.RESOURCE_TITLE,
                    BOOKMARKSTable.LOCATION,
                    BOOKMARKSTable.CREATION_DATE)
                    .whereArgs("bookID = {bookID}", "bookID" to bookID as Any)
                    .orderBy(BOOKMARKSTable.RESOURCE_INDEX, SqlOrderDirection.ASC)
                    .orderBy(BOOKMARKSTable.CREATION_DATE, SqlOrderDirection.ASC)
                    .exec {
                        parseList(MyRowParser()).toMutableList()
                    }
        }
    }

    class MyRowParser : RowParser<Locator> {
        override fun parseRow(columns: Array<Any?>): Locator {
            val id = columns[0]?.let {
                return@let it
            } ?: kotlin.run { return@run 0 }
            val bookID = columns[1]?.let {
                return@let it
            } ?: kotlin.run { return@run 0 }
            val publicationID = columns[2]?.let {
                return@let it
            } ?: kotlin.run { return@run "" }
            val resourceIndex = columns[3]?.let {
                return@let it
            } ?: kotlin.run { return@run 0 }
            val resourceHref = columns[4]?.let {
                return@let it
            } ?: kotlin.run { return@run "" }
            val resourceTitle = columns[5]?.let {
                return@let it
            } ?: kotlin.run { return@run "" }
            val location = columns[6]?.let {
                return@let it
            } ?: kotlin.run { return@run "" }
            val created = columns[7]?.let {
                return@let it
            } ?: kotlin.run { return@run null }

            return Locator(bookID as Long, publicationID as String, resourceIndex as Long, resourceHref as String, resourceTitle as String, Location.fromJSON(JSONObject(location as String)), created as Long, id as Long)
        }
    }

}