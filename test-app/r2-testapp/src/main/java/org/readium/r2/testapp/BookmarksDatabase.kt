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
import org.joda.time.DateTime

/**
 * Bookmark model
 *
 * @var bookId: Long? - Book index in the database
 * @val resourceIndex: Long -  Index to the spine element
 * @val resourceHref: String -  Reference to the spine element
 * @val spine_index: Long - Index to the spine element of the book
 * @val progression: Double - Percentage of progression in the spine element
 * @val timestamp: String - Datetime when the bookmark has been created
 * @var id: Long? - ID of the bookmark in database
 *
 * @fun toString(): String - Return a String description of the Bookmark
 */
class Bookmark(val bookID: Long,
               val resourceIndex: Long,
               val resourceHref: String,
               val progression: Double = 0.0,
               var timestamp: Long = DateTime().toDate().time,
               var id: Long? = null
) {

    override fun toString(): String {
        return "Bookmark id : ${this.id}, book identifier : ${this.bookID}, resource href selected ${this.resourceHref}, progression saved ${this.progression} and created the ${this.timestamp}."
    }

}

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
                BOOKMARKSTable.RESOURCE_INDEX to INTEGER,
                BOOKMARKSTable.RESOURCE_HREF to TEXT,
                BOOKMARKSTable.PROGRESSION to REAL,
                BOOKMARKSTable.TIMESTAMP to INTEGER)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Here you can upgrade tables, as usual
        db.dropTable(BOOKMARKSTable.NAME, true)
    }
}

object BOOKMARKSTable {
    const val NAME = "BOOKMARKS"
    const val ID = "id"
    const val BOOK_ID = "bookID"
    const val RESOURCE_INDEX = "resourceIndex"
    const val RESOURCE_HREF = "resourceHref"
    const val PROGRESSION = "progression"
    const val TIMESTAMP = "timestamp"
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

    fun insert(bookmark: Bookmark): Long? {
        if (bookmark.bookID < 0 ||
                bookmark.resourceIndex < 0 ||
                bookmark.progression < 0 || bookmark.progression > 100){
            return null
        }
        val exists = has(bookmark)
        if (exists.isEmpty()) {
            return database.use {
                return@use insert(BOOKMARKSTable.NAME,
                        BOOKMARKSTable.BOOK_ID to bookmark.bookID,
                        BOOKMARKSTable.RESOURCE_INDEX to bookmark.resourceIndex,
                        BOOKMARKSTable.RESOURCE_HREF to bookmark.resourceHref,
                        BOOKMARKSTable.PROGRESSION to bookmark.progression,
                        BOOKMARKSTable.TIMESTAMP to bookmark.timestamp)
            }
        }
        return null
    }

    fun has(bookmark: Bookmark): List<Bookmark> {
        return database.use {
            select(BOOKMARKSTable.NAME,
                    BOOKMARKSTable.ID,
                    BOOKMARKSTable.BOOK_ID,
                    BOOKMARKSTable.RESOURCE_INDEX,
                    BOOKMARKSTable.RESOURCE_HREF,
                    BOOKMARKSTable.PROGRESSION,
                    BOOKMARKSTable.TIMESTAMP)
                    .whereArgs("(bookID = {bookID}) AND (resourceIndex = {resourceIndex}) AND (resourceHref = {resourceHref}) AND (progression = {progression})",
                            "bookID" to bookmark.bookID,
                            "resourceIndex" to bookmark.resourceIndex,
                            "resourceHref" to bookmark.resourceHref,
                            "progression" to bookmark.progression)
                    .exec {
                        parseList(MyRowParser())
                    }
        }
    }

    fun delete(bookmark: Bookmark) {
        database.use {
            delete(BOOKMARKSTable.NAME, "id = {id}",
                    "id" to bookmark.id!!)
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

    fun listAll(): MutableList<Bookmark> {
        return database.use {
            select(BOOKMARKSTable.NAME,
                    BOOKMARKSTable.ID,
                    BOOKMARKSTable.BOOK_ID,
                    BOOKMARKSTable.RESOURCE_INDEX,
                    BOOKMARKSTable.RESOURCE_HREF,
                    BOOKMARKSTable.PROGRESSION,
                    BOOKMARKSTable.TIMESTAMP)
                    .exec {
                        parseList(MyRowParser()).toMutableList()
                    }
        }
    }


    fun list(bookID: Long): MutableList<Bookmark> {
        return database.use {
            select(BOOKMARKSTable.NAME,
                    BOOKMARKSTable.ID,
                    BOOKMARKSTable.BOOK_ID,
                    BOOKMARKSTable.RESOURCE_INDEX,
                    BOOKMARKSTable.RESOURCE_HREF,
                    BOOKMARKSTable.PROGRESSION,
                    BOOKMARKSTable.TIMESTAMP)
                    .whereArgs("bookID = {bookID}", "bookID" to bookID as Any)
                    .orderBy(BOOKMARKSTable.RESOURCE_INDEX, SqlOrderDirection.ASC)
                    .orderBy(BOOKMARKSTable.PROGRESSION, SqlOrderDirection.ASC)
                    .exec {
                        parseList(MyRowParser()).toMutableList()
                    }
        }
    }

    class MyRowParser : RowParser<Bookmark> {
        override fun parseRow(columns: Array<Any?>): Bookmark {
            val id = columns[0]?.let {
                return@let it
            } ?: kotlin.run { return@run 0 }
            val bookID = columns[1]?.let {
                return@let it
            } ?: kotlin.run { return@run 0 }
            val resourceIndex = columns[2]?.let {
                return@let it
            } ?: kotlin.run { return@run 0 }
            val resourceHref = columns[3]?.let {
                return@let it
            } ?: kotlin.run { return@run "" }
            val progression = columns[4]?.let {
                return@let it
            } ?: kotlin.run { return@run 0.0f }
            val timestamp = columns[5]?.let {
                return@let it
            } ?: kotlin.run { return@run 0 }

            return Bookmark(bookID as Long, resourceIndex as Long, resourceHref as String, progression as Double, timestamp as Long, id as Long)
        }
    }

}