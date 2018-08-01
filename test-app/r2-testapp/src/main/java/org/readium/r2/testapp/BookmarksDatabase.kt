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
import java.text.SimpleDateFormat
import java.util.*

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
class Bookmark(val pub_ref: Long,
               val spine_index: Long,
               val progression: Double = 0.0,
               var timestamp: String = SimpleDateFormat("MM/dd/yyyy hh:mm:ss").format(Date()),
               var id: Long? = null
               ){

    override fun toString(): String {
        return "Bookmark id : ${this.id}, book identifier : ${this.pub_ref}, spine item selected ${this.spine_index}, progression saved ${this.progression} and created the ${this.timestamp}."
    }

}

/**
 * UnitTests() tests the database helpers with dummies Bookmarks
 */
fun bkmkUnitTests(ctx: Context){
    val bk = mutableListOf<Bookmark>()
    bk.add(Bookmark(1, 1, 0.0))
    bk.add(Bookmark(2, 3, 50.0))
    bk.add(Bookmark(2, 3, 50.0))
    bk.add(Bookmark(15, 12, 99.99))


    val bkUnknown = mutableListOf<Bookmark>()
    bkUnknown.add(Bookmark(4, 34, 133.33))
    bkUnknown.add(Bookmark(-4, -34, 33.33))

    val db = BookmarksDatabase(ctx)
    println("#####################################")
    println("###########    Test    ##############")
    var i = 0
    bk.forEach {
        i++
        println("Book $i : ")
        println(" - bookId = ${it.pub_ref} (${it.pub_ref.javaClass})")
        try {
            val ret = db.bookmarks.insert(it)
            if (ret != null) {
                println("Added with success !")
            } else {
                println("Already exist !")
            }
        } catch (e: Exception) {
            println("Book number $i failed : ${e.message}")
        }

        try {
            var ret = db.bookmarks.has(it)
            if (ret.isNotEmpty()) {
                println("Found : $ret !")
            } else {
                println("Book not found !")
            }
//                db.bookmarks.delete(ret.first())
//                ret = db.bookmarks.has(it)
//                if (ret.isNotEmpty()) { println("Delete failed : $ret !") } else { println("Correctly deleted !") }
        } catch (e: Exception) {
            println("Book number $i finding failed : ${e.message}")
        }
    }
    println("List of BookMarks  : ")
    db.bookmarks.listAll().forEach { println(it) }
    println("-------------------------------------")
    println("------------  Unknown  --------------")
    bkUnknown.forEach {
        i++
        println("Book $i : ")
        println(" - bookId = ${it.pub_ref} (${it.pub_ref.javaClass})")
        try {
            var ret = db.bookmarks.has(it)
            if (ret.isNotEmpty()) {
                println("Found an unknown book ?! : $ret !")
            } else {
                println("Book number $i : Not found !")
            }
//                db.bookmarks.delete(ret.first())
//                ret = db.bookmarks.has(it)
//                if (ret.isNotEmpty()) { println("Delete failed : $ret !") } else { println("Correctly deleted !") }
        } catch (e: Exception) {
            println("Book number $i finding failed : ${e.message}")
        }
    }
    //db.bookmarks.emptyTable()
    println("###########    End     ##############")
    println("#####################################")
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
                BOOKMARKSTable.ID to INTEGER + PRIMARY_KEY + AUTOINCREMENT,
                BOOKMARKSTable.PUB_REF to INTEGER,
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
    val PUB_REF = "pub_ref"
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
        if (exists.isEmpty() && bookmark.pub_ref >= 0) {
            return database.use {
                return@use insert(BOOKMARKSTable.NAME,
                        BOOKMARKSTable.PUB_REF to bookmark.pub_ref,
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
                    BOOKMARKSTable.ID,
                    BOOKMARKSTable.PUB_REF,
                    BOOKMARKSTable.SPINE_INDEX,
                    BOOKMARKSTable.PROGRESSION,
                    BOOKMARKSTable.TIMESTAMP)
                    .whereArgs("(pub_ref = {pub_ref}) AND (spine_index = {spine_index}) AND (progression = {progression})",
                            "pub_ref" to bookmark.pub_ref,
                            "spine_index" to bookmark.spine_index,
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
                    BOOKMARKSTable.PUB_REF,
                    BOOKMARKSTable.SPINE_INDEX,
                    BOOKMARKSTable.PROGRESSION,
                    BOOKMARKSTable.TIMESTAMP)
                    .exec {
                        parseList(MyRowParser()).toMutableList()
                    }
        }
    }


    fun list(bookId: Long): MutableList<Bookmark> {
        return database.use {
            select(BOOKMARKSTable.NAME,
                    BOOKMARKSTable.ID,
                    BOOKMARKSTable.PUB_REF,
                    BOOKMARKSTable.SPINE_INDEX,
                    BOOKMARKSTable.PROGRESSION,
                    BOOKMARKSTable.TIMESTAMP)
                    .whereArgs("pub_ref = {bookId}", "bookId" to bookId as Any)
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
            val pub_ref = columns[1]?.let {
                return@let it
            }?: kotlin.run { return@run 0 }
            val spine_index = columns[2]?.let {
                return@let it
            }?: kotlin.run { return@run 0 }
            val progression = columns[3]?.let {
                return@let it
            }?: kotlin.run { return@run 0.0f }
            val timestamp = columns[4]?.let {
                return@let it
            }?: kotlin.run { return@run "" }

            return Bookmark(pub_ref as Long, spine_index as Long, progression as Double, timestamp as String, id as Long)
        }
    }

}