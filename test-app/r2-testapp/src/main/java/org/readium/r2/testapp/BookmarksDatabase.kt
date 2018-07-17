/*
 * Copyright 2018 Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.testapp

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import org.jetbrains.anko.db.*
import java.text.SimpleDateFormat
import java.util.Date


class Bookmark(val book_ref: Long,
               val spine_index: Long,
               val progression: Double = 0.0,
               var timestamp: String = SimpleDateFormat("MM/dd/yyyy hh:mm:ss").format(Date())){

    override fun toString(): String {
        return "Book number ${this.book_ref}, spine item selected ${this.spine_index}, progression saved ${this.progression} and created the ${this.timestamp}."
    }
}

class BookmarksDatabase {

    val shared:BookmarksDatabaseOpenHelper
    var bookmarks: BOOKMARKS

    constructor(context: Context) {
        shared = BookmarksDatabaseOpenHelper(context)
        bookmarks = BOOKMARKS(shared)
    }

}

class BookmarksDatabaseOpenHelper(ctx: Context) : ManagedSQLiteOpenHelper(ctx, "bookmarks_database", null, 1) {
    companion object {
        private var instance: BookmarksDatabaseOpenHelper? = null

        @Synchronized
        fun getInstance(ctx: Context): BookmarksDatabaseOpenHelper {
            if (instance == null) {
                instance = BookmarksDatabaseOpenHelper(ctx.getApplicationContext())
            }
            return instance!!
        }
    }

    override fun onCreate(db: SQLiteDatabase) {

        db.createTable(BOOKMARKSTable.NAME, true,
                BOOKMARKSTable.ID to INTEGER + PRIMARY_KEY + UNIQUE,
                BOOKMARKSTable.BOOK_REF to INTEGER,
                BOOKMARKSTable.SPINE_INDEX to INTEGER,
                BOOKMARKSTable.PROGRESSION to REAL,
                BOOKMARKSTable.TIMESTAMP to TEXT)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Here you can upgrade tables, as usual
        db.dropTable(BOOKMARKSTable.NAME, true)
        // TODO need to add a migration = add extension column
    }
}

object BOOKMARKSTable {
    val NAME = "BOOKMARKS"
    val ID = "id"
    val BOOK_REF = "book_ref"
    val SPINE_INDEX = "spine_index"
    val PROGRESSION = "progression"
    val TIMESTAMP = "timestamp"
}

class BOOKMARKS(var database: BookmarksDatabaseOpenHelper) {

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
        val exists = has(bookmark)
        if (exists.isEmpty()) {
            return database.use {
                return@use insert(BOOKMARKSTable.NAME,
                        BOOKMARKSTable.BOOK_REF to bookmark.book_ref,
                        BOOKMARKSTable.SPINE_INDEX to bookmark.spine_index,
                        BOOKMARKSTable.PROGRESSION to bookmark.progression,
                        BOOKMARKSTable.TIMESTAMP to bookmark.timestamp)
            }
        }
        return null
    }

    fun has(bookmark: Bookmark): List<Bookmark> {
        return database.use {
            select(BOOKMARKSTable.NAME,
                    BOOKMARKSTable.BOOK_REF,
                    BOOKMARKSTable.SPINE_INDEX,
                    BOOKMARKSTable.PROGRESSION,
                    BOOKMARKSTable.TIMESTAMP)
                    .whereArgs("(book_ref = {book_ref}) AND (spine_index = {spine_index}) AND (progression = {progression})",
                            "book_ref" to bookmark.book_ref,
                            "spine_index" to bookmark.spine_index,
                            "progression" to bookmark.progression)
                    .exec {
                        parseList(MyRowParser())
                    }
        }
    }

    fun delete(bookmark: Bookmark){
        database.use {
            delete(BOOKMARKSTable.NAME,"(book_ref = {book_ref}) AND (spine_index = {spine_index}) AND (progression = {progression})",
                    "book_ref" to bookmark.book_ref,
                    "spine_index" to bookmark.spine_index,
                    "progression" to bookmark.progression)
        }
    }

    fun list(): MutableList<Bookmark> {
        return database.use {
            select(BOOKMARKSTable.NAME,
                    BOOKMARKSTable.BOOK_REF,
                    BOOKMARKSTable.SPINE_INDEX,
                    BOOKMARKSTable.PROGRESSION,
                    BOOKMARKSTable.TIMESTAMP)
                    .exec {
                        parseList(MyRowParser()).toMutableList()
                    }
        }
    }

    class MyRowParser : RowParser<Bookmark> {
        override fun parseRow(columns: Array<Any?>): Bookmark {
            val book_ref = columns[0]?.let {
                return@let it
            }?: kotlin.run { return@run 0 }
            val spine_index = columns[1]?.let {
                return@let it
            }?: kotlin.run { return@run 0 }
            val progression = columns[2]?.let {
                return@let it
            }?: kotlin.run { return@run 0.0f }
            val timestamp = columns[3]?.let {
                return@let it
            }?: kotlin.run { return@run "" }

            return  Bookmark(book_ref as Long, spine_index as Long, progression as Double, timestamp as String)

        }
    }

}