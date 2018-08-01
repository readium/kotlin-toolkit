/*
 * Module: r2-testapp-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann, Mostapha Idoubihi, Paul Stoica
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
 * @var id: Long? - ID of the bookmark in database
 * @val pub_ref: Long -  Reference to the book
 * @val spine_index: Long - Index to the spine element of the book
 * @val progression: Double - Percentage of progression in the ( book or spine element ? )
 * @val timestamp: String - Datetime when the bookmark has been created
 *
 * @fun toString(): String - Return a String description of the Bookmark
 */
class Bookmark(val bookID: Long,
               val resourceIndex: Long,
               val resourceHref: String,
               val progression: Double = 0.0,
               var timestamp: Long = DateTime().toDate().time,
               var id: Long? = null
               ){

    override fun toString(): String {
        return "Bookmark id : ${this.id}, book identifier : ${this.bookID}, resource href selected ${this.resourceHref}, progression saved ${this.progression} and created the ${this.timestamp}."
    }

}

/**
 * UnitTests() tests the database helpers with dummies Bookmarks
 */
//fun bkmkUnitTests(ctx: Context){
//    val bk = mutableListOf<Bookmark>()
//    bk.add(Bookmark(1, 1, 0.0))
//    bk.add(Bookmark(2, 3, 50.0))
//    bk.add(Bookmark(2, 3, 50.0))
//    bk.add(Bookmark(15, 12, 99.99))
//
//
//    val bkUnknown = mutableListOf<Bookmark>()
//    bkUnknown.add(Bookmark(4, 34, 133.33))
//    bkUnknown.add(Bookmark(-4, -34, 33.33))
//
//    val db = BookmarksDatabase(ctx)
//    println("#####################################")
//    println("###########    Test    ##############")
//    var i = 0
//    bk.forEach {
//        i++
//        println("Book $i : ")
//        println(" - bookId = ${it.bookID} (${it.bookID.javaClass})")
//        try {
//            val ret = db.bookmarks.insert(it)
//            if (ret != null) {
//                println("Added with success !")
//            } else {
//                println("Already exist !")
//            }
//        } catch (e: Exception) {
//            println("Book number $i failed : ${e.message}")
//        }
//
//        try {
//            var ret = db.bookmarks.has(it)
//            if (ret.isNotEmpty()) {
//                println("Found : $ret !")
//            } else {
//                println("Book not found !")
//            }
////                db.bookmarks.delete(ret.first())
////                ret = db.bookmarks.has(it)
////                if (ret.isNotEmpty()) { println("Delete failed : $ret !") } else { println("Correctly deleted !") }
//        } catch (e: Exception) {
//            println("Book number $i finding failed : ${e.message}")
//        }
//    }
//    println("List of BookMarks  : ")
//    db.bookmarks.listAll().forEach { println(it) }
//    println("-------------------------------------")
//    println("------------  Unknown  --------------")
//    bkUnknown.forEach {
//        i++
//        println("Book $i : ")
//        println(" - bookId = ${it.bookID} (${it.bookID.javaClass})")
//        try {
//            var ret = db.bookmarks.has(it)
//            if (ret.isNotEmpty()) {
//                println("Found an unknown book ?! : $ret !")
//            } else {
//                println("Book number $i : Not found !")
//            }
////                db.bookmarks.delete(ret.first())
////                ret = db.bookmarks.has(it)
////                if (ret.isNotEmpty()) { println("Delete failed : $ret !") } else { println("Correctly deleted !") }
//        } catch (e: Exception) {
//            println("Book number $i finding failed : ${e.message}")
//        }
//    }
//    //db.bookmarks.emptyTable()
//    println("###########    End     ##############")
//    println("#####################################")
//}

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
    val NAME = "BOOKMARKS"
    val ID = "id"
    val BOOK_ID = "bookID"
    val RESOURCE_INDEX = "resourceIndex"
    val RESOURCE_HREF = "resourceHref"
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

    fun delete(bookmark: Bookmark){
        database.use {
            delete(BOOKMARKSTable.NAME,"id = {id}",
                    "id" to bookmark.id!!)
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
            }?: kotlin.run { return@run 0 }
            val bookID = columns[1]?.let {
                return@let it
            }?: kotlin.run { return@run 0 }
            val resourceIndex = columns[2]?.let {
                return@let it
            }?: kotlin.run { return@run 0 }
            val resourceHref = columns[3]?.let {
                return@let it
            }?: kotlin.run { return@run "" }
            val progression = columns[4]?.let {
                return@let it
            }?: kotlin.run { return@run 0.0f }
            val timestamp = columns[5]?.let {
                return@let it
            }?: kotlin.run { return@run 0 }

            return Bookmark(bookID as Long, resourceIndex as Long, resourceHref as String, progression as Double, timestamp as Long, id as Long)
        }
    }

}